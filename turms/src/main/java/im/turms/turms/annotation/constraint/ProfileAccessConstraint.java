package im.turms.turms.annotation.constraint;

import im.turms.turms.constant.ProfileAccessStrategy;

import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;
import java.lang.annotation.*;

@Target({ElementType.PARAMETER, ElementType.TYPE, ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ProfileAccessConstraint.ProfileAccessValidator.class)
@Documented
public @interface ProfileAccessConstraint {
    String message() default "The profile access strategy must not be UNRECOGNIZED";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    public static class ProfileAccessValidator implements ConstraintValidator<ProfileAccessConstraint, ProfileAccessStrategy> {

        @Override
        public void initialize(ProfileAccessConstraint constraintAnnotation) {
        }

        @Override
        public boolean isValid(ProfileAccessStrategy value, ConstraintValidatorContext context) {
            return value != ProfileAccessStrategy.UNRECOGNIZED;
        }
    }
}
