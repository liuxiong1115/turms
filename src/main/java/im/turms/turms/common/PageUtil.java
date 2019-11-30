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

package im.turms.turms.common;

import im.turms.turms.cluster.TurmsClusterManager;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;

@Component
public class PageUtil {
    private final TurmsClusterManager turmsClusterManager;

    public PageUtil(TurmsClusterManager turmsClusterManager) {
        this.turmsClusterManager = turmsClusterManager;
    }

    // TODO: more options
    public int getSize(@Nullable Integer size) {
        if (size == null || size <= 0) {
            return turmsClusterManager.getTurmsProperties().getSecurity().getDefaultReturnedRecordsPerRequest();
        } else {
            int maxLimit = turmsClusterManager.getTurmsProperties().getSecurity().getMaxReturnedRecordsPerRequest();
            if (size > maxLimit) {
                size = maxLimit;
            }
        }
        return size;
    }
}
