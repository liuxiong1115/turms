package im.turms.turms.util;

import com.google.common.net.InetAddresses;
import im.turms.turms.manager.TurmsClusterManager;
import im.turms.turms.property.TurmsProperties;
import im.turms.turms.property.env.LoadBalancing;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.validation.Valid;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

@Log4j2
@Component
public class AddressUtil {
    public static final List<Function<AddressTuple, Void>> onAddressChangeListeners = new LinkedList<>();
    private static HttpClient client;
    private final TurmsClusterManager turmsClusterManager;
    private final ApplicationContext context;
    private AddressTuple addressTuple;
    @Getter
    private final boolean isEnabled;
    @Getter
    private String bindAddress;
    @Getter
    private boolean isSslEnabled;
    @Getter
    private final Integer port;

    public AddressUtil(TurmsClusterManager turmsClusterManager, ApplicationContext context, TurmsProperties turmsProperties) throws UnknownHostException {
        this.turmsClusterManager = turmsClusterManager;
        this.context = context;
        isEnabled = turmsProperties.getLoadBalancing().isEnabled();
        port = context.getEnvironment().getProperty("server.port", Integer.class);
        if (port == null) {
            throw new UnknownHostException("The local port of the current server cannot be found");
        }
        if (isEnabled) {
            TurmsProperties.propertiesChangeListeners.add(properties -> {
                AddressTuple tuple;
                try {
                    tuple = queryAddressTuple();
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                    return null;
                }
                boolean changed = !addressTuple.equals(tuple);
                if (changed) {
                    for (Function<AddressTuple, Void> listener : onAddressChangeListeners) {
                        listener.apply(tuple);
                    }
                    addressTuple = tuple;
                }
                return null;
            });
        }
    }

    @PostConstruct
    private void initAddress() throws UnknownHostException, InterruptedException, ExecutionException, TimeoutException {
        if (isEnabled) {
            Environment env = context.getEnvironment();
            bindAddress = env.getProperty("server.address");
            isSslEnabled = env.getProperty("server.ssl.enabled", Boolean.class, false);
            addressTuple = queryAddressTuple();
            for (Function<AddressTuple, Void> listener : onAddressChangeListeners) {
                listener.apply(addressTuple);
            }
        }
    }

    private String attachPortToIp(String ip) {
        return String.format("%s:%d", ip, port);
    }

    private String queryIp() throws UnknownHostException, InterruptedException, ExecutionException, TimeoutException {
        LoadBalancing loadBalancing = turmsClusterManager.getTurmsProperties().getLoadBalancing();
        String address;
        LoadBalancing.AdvertiseStrategy advertiseStrategy = loadBalancing.getAdvertiseStrategy();
        switch (advertiseStrategy) {
            case ADVERTISE_ADDRESS:
                address = loadBalancing.getAdvertiseAddress();
                break;
            case BIND_ADDRESS:
                address = bindAddress;
                break;
            case LOCAL_ADDRESS:
                address = InetAddress.getLocalHost().getHostAddress();
                break;
            case PUBLIC_ADDRESS:
                address = queryPublicIp(loadBalancing.getPublicIpDetectorAddresses());
                break;
            default:
                throw new IllegalArgumentException("Unexpected value: " + advertiseStrategy.name());
        }
        if (InetAddresses.isInetAddress(address)) {
            return address;
        } else {
            String message = "The address isn't an illegal address: " + advertiseStrategy.name();
            throw new UnknownHostException(message);
        }
    }

    private AddressTuple queryAddressTuple() throws UnknownHostException, InterruptedException, ExecutionException, TimeoutException {
        @Valid LoadBalancing loadBalancing = turmsClusterManager.getTurmsProperties().getLoadBalancing();
        if (loadBalancing.getAdvertiseStrategy() == LoadBalancing.AdvertiseStrategy.IDENTIFY) {
            return new AddressTuple(loadBalancing.getIdentity(), null, null, null, null);
        } else {
            String ip = queryIp();
            boolean attachPortToIp = loadBalancing.isAttachPortToIp();
            if (ip != null) {
                return new AddressTuple(null,
                        ip,
                        attachPortToIp ? attachPortToIp(ip) : ip,
                        String.format("%s://%s", isSslEnabled ? "https" : "http", ip),
                        String.format("%s://%s", isSslEnabled ? "wss" : "ws", ip));
            } else {
                throw new UnknownHostException("The address of the current server cannot be found");
            }
        }
    }

    public static String queryPublicIp(List<String> ipDetectorAddresses) throws InterruptedException, ExecutionException, TimeoutException {
        if (!ipDetectorAddresses.isEmpty()) {
            List<CompletableFuture<HttpResponse<String>>> futures = new ArrayList<>(ipDetectorAddresses.size());
            if (client == null) {
                client = HttpClient.newHttpClient();
            }
            for (String checkerAddress : ipDetectorAddresses) {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(checkerAddress))
                        .build();
                CompletableFuture<HttpResponse<String>> future = client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
                futures.add(future);
            }
            String ip = FutureUtil.race(futures).get(15, TimeUnit.SECONDS).body();
            return ip != null && InetAddresses.isInetAddress(ip) ? ip : null;
        } else {
            throw new RuntimeException("The IP detector addresses is empty");
        }
    }

    public String getIdentity() {
        return addressTuple.identity;
    }

    public String getIp() {
        return addressTuple.ip;
    }

    public String getAddress() {
        return addressTuple.address;
    }

    public String getHttpAddress() {
        return addressTuple.httpAddress;
    }

    public String getWsAddress() {
        return addressTuple.wsAddress;
    }

    @AllArgsConstructor
    @EqualsAndHashCode
    @Data
    public static final class AddressTuple {
        private final String identity;
        private final String ip;
        private final String address;
        private final String httpAddress;
        private final String wsAddress;
    }
}
