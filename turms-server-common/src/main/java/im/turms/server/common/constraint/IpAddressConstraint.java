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

package im.turms.server.common.constraint;

import com.google.common.net.InetAddresses;

import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;
import java.lang.annotation.*;

/**
 * @author James Chen
 */
@Target({ElementType.PARAMETER, ElementType.TYPE, ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = IpAddressConstraint.IpAddressValidator.class)
@Documented
public @interface IpAddressConstraint {

    String message() default "The string must be a valid IP";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class IpAddressValidator implements ConstraintValidator<IpAddressConstraint, String> {

        @Override
        public void initialize(IpAddressConstraint constraintAnnotation) {
        }

        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            if (value == null) {
                return true;
            } else {
                return InetAddresses.isInetAddress(value);
            }
        }

        public static void validate(String value) {
            if (value != null && !InetAddresses.isInetAddress(value)) {
                throw new IllegalArgumentException("The string must be a valid IP");
            }
        }
    }

}
