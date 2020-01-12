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

import org.springframework.data.mongodb.core.query.Update;

import java.util.Collection;

public class UpdateBuilder {
    private Update update;

    private UpdateBuilder() {
        this.update = new Update();
    }

    public static UpdateBuilder newBuilder() {
        return new UpdateBuilder();
    }

    public UpdateBuilder setIfNotNull(String key, Object value) {
        if (value != null) {
            if (value instanceof Collection) {
                if (!((Collection) value).isEmpty()) {
                    update.set(key, value);
                }
            } else {
                update.set(key, value);
            }
        }
        return this;
    }

    public Update build() {
        return update;
    }
}
