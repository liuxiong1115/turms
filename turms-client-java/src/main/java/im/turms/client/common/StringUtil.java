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

package im.turms.client.common;

public class StringUtil {
    private StringUtil() {}

    public static String camelToSnakeCase(String camelcase) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < camelcase.length(); i++) {
            char c = camelcase.charAt(i);
            if (Character.isUpperCase(c)) {
                builder.append(builder.length() != 0 ? '_' : "").append(Character.toLowerCase(c));
            } else {
                builder.append(Character.toLowerCase(c));
            }
        }
        return builder.toString();
    }
}
