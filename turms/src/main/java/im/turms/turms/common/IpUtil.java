package im.turms.turms.common;

import com.google.common.net.InetAddresses;
import im.turms.turms.cluster.TurmsClusterManager;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Component
public class IpUtil {
    private final TurmsClusterManager turmsClusterManager;
    private final ConfigurableApplicationContext context;
    private HttpClient client;
    private String currentIp;

    public IpUtil(TurmsClusterManager turmsClusterManager, ConfigurableApplicationContext context) {
        this.turmsClusterManager = turmsClusterManager;
        this.context = context;
    }

    public String getCurrentIp() {
        return currentIp;
    }

    @PostConstruct
    private void getIp() {
        String ip = queryIp();
        if (ip != null) {
            currentIp = ip;
        } else {
            TurmsLogger.log("Exit because the IP for current server cannot be found");
            context.close();
        }
    }

    private String queryIp() {
        String ip = turmsClusterManager.getTurmsProperties().getIp().getIp();
        if (ip != null && InetAddresses.isInetAddress(ip)) {
            return ip;
        } else if (turmsClusterManager.getTurmsProperties().getIp().isShouldUseLocalIp()) {
            return getLocalIp();
        } else {
            return queryPublicIp();
        }
    }

    private String queryPublicIp() {
        List<String> checkerAddresses = turmsClusterManager.getTurmsProperties().getIp().getIpCheckerAddresses();
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

    private String getLocalIp() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            return null;
        }
    }
}
