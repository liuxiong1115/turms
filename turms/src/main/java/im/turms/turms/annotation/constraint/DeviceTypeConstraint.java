package im.turms.turms.annotation.constraint;

import im.turms.turms.constant.DeviceType;

import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;
import java.lang.annotation.*;

@Target({ElementType.PARAMETER, ElementType.TYPE, ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = DeviceTypeConstraint.GroupMemberRoleValidator.class)
@Documented
public @interface DeviceTypeConstraint {
    String message() default "The device type must not be UNRECOGNIZED";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    public static class GroupMemberRoleValidator implements ConstraintValidator<DeviceTypeConstraint, DeviceType> {

        @Override
        public void initialize(DeviceTypeConstraint constraintAnnotation) {
        }

        @Override
        public boolean isValid(DeviceType value, ConstraintValidatorContext context) {
            return value != DeviceType.UNRECOGNIZED;
        }
    }
}
