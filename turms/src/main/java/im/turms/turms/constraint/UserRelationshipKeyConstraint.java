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

package im.turms.turms.constraint;

import im.turms.turms.workflow.dao.domain.UserRelationship;

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
@Constraint(validatedBy = UserRelationshipKeyConstraint.UserRelationshipKeyValidator.class)
@Documented
public @interface UserRelationshipKeyConstraint {

    String message() default "The user relationship key must not be null";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class UserRelationshipKeyValidator implements ConstraintValidator<UserRelationshipKeyConstraint, UserRelationship.Key> {

        @Override
        public void initialize(UserRelationshipKeyConstraint constraintAnnotation) {
        }

        @Override
        public boolean isValid(UserRelationship.Key value, ConstraintValidatorContext context) {
            return value != null && value.getOwnerId() != null && value.getRelatedUserId() != null;
        }

        public static void validate(UserRelationship.Key value) {
            if (value == null || value.getOwnerId() == null || value.getRelatedUserId() == null) {
                throw new IllegalArgumentException("The user relationship key must not be null");
            }
        }
    }
}
