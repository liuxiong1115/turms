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

import im.turms.turms.property.TurmsProperties;
import im.turms.turms.property.env.Security;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.MessageDigestPasswordEncoder;
import org.springframework.stereotype.Component;

import javax.validation.constraints.NotNull;

@Component
public class TurmsPasswordUtil {
    private static BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder(10);
    // Ignore @Deprecated because it's still secure enough for IM.
    @SuppressWarnings("deprecation")
    private static MessageDigestPasswordEncoder messageDigestPasswordEncoder = new MessageDigestPasswordEncoder("SHA-256");
    private final TurmsProperties turmsProperties;

    public TurmsPasswordUtil(TurmsProperties turmsProperties) {
        this.turmsProperties = turmsProperties;
    }

    public String encodePassword(Security.PasswordEncodingAlgorithm strategy, String rawPassword) {
        switch (strategy) {
            case BCRYPT:
                return bCryptPasswordEncoder.encode(rawPassword);
            case SALTED_SHA256:
                return messageDigestPasswordEncoder.encode(rawPassword);
            case RAW:
            default:
                return rawPassword;
        }
    }

    public String encodeAdminPassword(@NotNull String rawPassword) {
        if (rawPassword != null) {
            return encodePassword(turmsProperties.getSecurity().getAdminPasswordEncodingAlgorithm(),
                    rawPassword);
        } else {
            throw new IllegalArgumentException("rawPassword must not be null");
        }
    }

    public String encodeUserPassword(@NotNull String rawPassword) {
        if (rawPassword != null) {
            return encodePassword(turmsProperties.getSecurity().getUserPasswordEncodingAlgorithm(),
                    rawPassword);
        } else {
            throw new IllegalArgumentException("rawPassword must not be null");
        }
    }

    public boolean matchesAdminPassword(String rawPassword, String encodedPassword) {
        return matchesPassword(turmsProperties.getSecurity().getAdminPasswordEncodingAlgorithm(),
                rawPassword,
                encodedPassword);
    }

    public boolean matchesUserPassword(String rawPassword, String encodedPassword) {
        return matchesPassword(turmsProperties.getSecurity().getUserPasswordEncodingAlgorithm(),
                rawPassword,
                encodedPassword);
    }

    public boolean matchesPassword(
            Security.PasswordEncodingAlgorithm strategy,
            String rawPassword,
            String encodedPassword) {
        switch (strategy) {
            case BCRYPT:
                return bCryptPasswordEncoder.matches(rawPassword, encodedPassword);
            case SALTED_SHA256:
                return messageDigestPasswordEncoder.matches(rawPassword, encodedPassword);
            case RAW:
                return rawPassword.equals(encodedPassword);
            default:
                return false;
        }
    }
}
