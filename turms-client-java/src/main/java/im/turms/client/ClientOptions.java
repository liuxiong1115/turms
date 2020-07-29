/*
 * Copyright (C) 2019 The Turms Project
 * https://github.com/turms-im/turms
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
