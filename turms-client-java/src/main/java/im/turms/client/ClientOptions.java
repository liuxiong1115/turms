package im.turms.client;

public class ClientOptions {

    private String turmsServerUrl;
    private Integer connectionTimeout;
    private Integer minRequestsInterval;
    private String storageServerUrl;

    public static ClientOptions build() {
        return new ClientOptions();
    }

    private ClientOptions() {
    }


    public String turmsServerUrl() {
        return turmsServerUrl;
    }

    public ClientOptions turmsServerUrl(String turmsServerUrl) {
        this.turmsServerUrl = turmsServerUrl;
        return this;
    }

    public Integer connectionTimeout() {
        return connectionTimeout;
    }

    public ClientOptions connectionTimeout(Integer connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
        return this;
    }

    public Integer minRequestsInterval() {
        return minRequestsInterval;
    }

    public ClientOptions minRequestsInterval(Integer minRequestsInterval) {
        this.minRequestsInterval = minRequestsInterval;
        return this;
    }

    public String storageServerUrl() {
        return storageServerUrl;
    }

    public ClientOptions storageServerUrl(String storageServerUrl) {
        this.storageServerUrl = storageServerUrl;
        return this;
    }
}
