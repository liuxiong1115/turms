package im.turms.turms.common;

import com.google.common.net.InetAddresses;
import im.turms.turms.cluster.TurmsClusterManager;
import im.turms.turms.property.TurmsProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
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
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

@Component
public class AddressUtil {
    public static final List<Function<AddressTuple, Void>> onAddressChangeListeners = new LinkedList<>();
    private static HttpClient client;
    private final TurmsClusterManager turmsClusterManager;
    private final ApplicationContext context;
    private AddressTuple addressTuple;
    @Getter
    private boolean isSslEnabled;

    public AddressUtil(TurmsClusterManager turmsClusterManager, ConfigurableApplicationContext context) {
        this.turmsClusterManager = turmsClusterManager;
        this.context = context;
        TurmsProperties.propertiesChangeListeners.add(properties -> {
            AddressTuple tuple = queryAddresses();
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

    @PostConstruct
    private void initAddresses() {
        addressTuple = queryAddresses();
        for (Function<AddressTuple, Void> listener : onAddressChangeListeners) {
            listener.apply(addressTuple);
        }
    }

    private AddressTuple queryAddresses() {
        String ip = queryIp(turmsClusterManager.getTurmsProperties());
        if (ip != null) {
            Environment env = context.getEnvironment();
            isSslEnabled = Boolean.parseBoolean(env.getProperty("server.ssl.enabled", "false"));
            return new AddressTuple(ip,
                    String.format("%s://%s", isSslEnabled ? "https" : "http", ip),
                    String.format("%s://%s", isSslEnabled ? "wss" : "ws", ip));
        } else {
            TurmsLogger.log("Exit because the IP for current server cannot be found");
            ((ConfigurableApplicationContext) context).close();
            return null;
        }
    }

    public static String queryIp(TurmsProperties properties) {
        String ip = properties.getIp().getIp();
        if (ip != null && InetAddresses.isInetAddress(ip)) {
            return ip;
        } else if (properties.getIp().isUseLocalIp()) {
            return getLocalIp();
        } else {
            return queryPublicIp(properties);
        }
    }

    public static String queryPublicIp(TurmsProperties properties) {
        List<String> checkerAddresses = properties.getIp().getIpCheckerAddresses();
        if (!checkerAddresses.isEmpty()) {
            List<CompletableFuture<HttpResponse<String>>> futures = new ArrayList<>(checkerAddresses.size());
            if (client == null) {
                client = HttpClient.newHttpClient();
            }
            for (String checkerAddress : checkerAddresses) {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(checkerAddress))
                        .build();
                CompletableFuture<HttpResponse<String>> future = client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
                futures.add(future);
            }
            try {
                String ip = FutureUtil.race(futures).get(15, TimeUnit.SECONDS).body();
                return ip != null && InetAddresses.isInetAddress(ip) ? ip : null;
            } catch (Exception e) {
                return null;
            }
        } else {
            return null;
        }
    }

    public static String getLocalIp() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            return null;
        }
    }

    public String getIp() {
        return addressTuple.ip;
    }

    public String getHttpAddress() {
        return addressTuple.httpAddress;
    }

    public String getWsAddress() {
        return addressTuple.wsAddress;
    }

    @AllArgsConstructor
    @Data
    public static final class AddressTuple {
        private final String ip;
        private final String httpAddress;
        private final String wsAddress;
    }
}
