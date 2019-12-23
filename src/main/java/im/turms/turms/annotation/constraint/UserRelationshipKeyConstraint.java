package im.turms.turms.annotation.constraint;

import im.turms.turms.pojo.domain.UserRelationship;

import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;
import java.lang.annotation.*;

@Target({ElementType.PARAMETER, ElementType.TYPE, ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = UserRelationshipKeyConstraint.UserRelationshipKeyValidator.class)
@Documented
public @interface UserRelationshipKeyConstraint {
    String message() default "UserRelationshipKeyConstraint";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    public static class UserRelationshipKeyValidator implements ConstraintValidator<UserRelationshipKeyConstraint, UserRelationship.Key> {

        @Override
        public void initialize(UserRelationshipKeyConstraint constraintAnnotation) {
        }

        @Override
        public boolean isValid(UserRelationship.Key value, ConstraintValidatorContext context) {
            return value != null && value.getOwnerId() != null && value.getRelatedUserId() != null;
        }
    }
}
