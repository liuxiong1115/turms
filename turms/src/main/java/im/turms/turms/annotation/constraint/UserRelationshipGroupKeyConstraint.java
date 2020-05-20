package im.turms.turms.annotation.constraint;

import im.turms.turms.pojo.domain.UserRelationshipGroup;

import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;
import java.lang.annotation.*;

@Target({ElementType.PARAMETER, ElementType.TYPE, ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = UserRelationshipGroupKeyConstraint.UserRelationshipGroupKeyValidator.class)
@Documented
public @interface UserRelationshipGroupKeyConstraint {
    String message() default "The user relationship group key must not be null";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    public static class UserRelationshipGroupKeyValidator implements ConstraintValidator<UserRelationshipGroupKeyConstraint, UserRelationshipGroup.Key> {

        @Override
        public void initialize(UserRelationshipGroupKeyConstraint constraintAnnotation) {
        }

        @Override
        public boolean isValid(UserRelationshipGroup.Key value, ConstraintValidatorContext context) {
            return value != null && value.getOwnerId() != null && value.getGroupIndex() != null;
        }
    }
}
