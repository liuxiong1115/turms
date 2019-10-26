package helper.util;/*
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

import java.net.URI;
import java.net.URISyntaxException;

import static im.turms.turms.common.SessionUtil.PASSWORD_FIELD;
import static im.turms.turms.common.SessionUtil.USER_ID_FIELD;

public class LoginUtil {

    private LoginUtil() {}

    public static URI getServerUrl(int port) throws URISyntaxException {
        return new URI(String.format("ws://127.0.0.1:%d", port));
    }

    public static String getLoginParams(
            Long userId,
            String password) {
        StringBuilder builder = new StringBuilder();
        builder.append(USER_ID_FIELD)
                .append("=")
                .append(userId)
                .append(";");
        builder.append(PASSWORD_FIELD)
                .append("=")
                .append(password)
                .append(";");
        return builder.toString();
    }
}
