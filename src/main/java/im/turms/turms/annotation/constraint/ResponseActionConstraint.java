package im.turms.turms.annotation.constraint;

import im.turms.turms.constant.ResponseAction;

import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;
import java.lang.annotation.*;

@Target({ElementType.PARAMETER, ElementType.TYPE, ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ResponseActionConstraint.ResponseActionValidator.class)
@Documented
public @interface ResponseActionConstraint {
    String message() default "ResponseActionConstraint";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    public static class ResponseActionValidator implements ConstraintValidator<ResponseActionConstraint, ResponseAction> {

        @Override
        public void initialize(ResponseActionConstraint constraintAnnotation) {
        }

        @Override
        public boolean isValid(ResponseAction value, ConstraintValidatorContext context) {
            return value != ResponseAction.UNRECOGNIZED;
        }
    }
}
