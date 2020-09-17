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

/**
 * @author James Chen
 */
public class ClientOptions {

    private String url;
    private Integer connectTimeout;
    private Integer requestTimeout;
    private Integer minRequestInterval;
    private Integer heartbeatInterval;
    private String storageServerUrl;

    public static ClientOptions build() {
        return new ClientOptions();
    }

    public String url() {
        return url;
    }

    public ClientOptions url(String url) {
        this.url = url;
        return this;
    }

    public Integer connectTimeout() {
        return connectTimeout;
    }

    public ClientOptions connectTimeout(Integer connectTimeout) {
        this.connectTimeout = connectTimeout;
        return this;
    }

    public Integer requestTimeout() {
        return requestTimeout;
    }

    public ClientOptions requestTimeout(Integer requestTimeout) {
        this.requestTimeout = requestTimeout;
        return this;
    }

    public Integer minRequestInterval() {
        return minRequestInterval;
    }

    public ClientOptions minRequestInterval(Integer minRequestInterval) {
        this.minRequestInterval = minRequestInterval;
        return this;
    }

    public Integer heartbeatInterval() {
        return heartbeatInterval;
    }

    public ClientOptions heartbeatInterval(Integer heartbeatInterval) {
        this.heartbeatInterval = heartbeatInterval;
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
