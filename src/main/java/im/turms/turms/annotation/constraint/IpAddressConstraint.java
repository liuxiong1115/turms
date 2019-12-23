package im.turms.turms.annotation.constraint;

import com.google.common.net.InetAddresses;

import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;
import java.lang.annotation.*;

@Target({ElementType.PARAMETER, ElementType.TYPE, ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = IpAddressConstraint.IpAddressValidator.class)
@Documented
public @interface IpAddressConstraint {
    String message() default "IpAddressConstraint";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    public static class IpAddressValidator implements ConstraintValidator<IpAddressConstraint, String> {

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
    }
}
