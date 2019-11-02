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

package im.turms.turms.task;

import com.hazelcast.spring.context.SpringAware;
import im.turms.turms.cluster.TurmsClusterManager;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.io.Serializable;
import java.util.concurrent.Callable;

@SpringAware
public class QueryResponsibleTurmsServerAddressTask implements Callable<String>, Serializable, ApplicationContextAware {
    private transient ApplicationContext context;
    private transient TurmsClusterManager turmsClusterManager;

    @Override
    public String call() {
        return turmsClusterManager.getLocalTurmsServerAddress();
    }

    @Override
    public void setApplicationContext(final ApplicationContext applicationContext) throws BeansException {
        context = applicationContext;
    }

    @Autowired
    public void setTurmsClusterManager(final TurmsClusterManager turmsClusterManager) {
        this.turmsClusterManager = turmsClusterManager;
    }
}
