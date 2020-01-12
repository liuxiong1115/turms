package im.turms.turms.annotation.constraint;

import im.turms.turms.constant.RequestStatus;

import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;
import java.lang.annotation.*;

@Target({ElementType.PARAMETER, ElementType.TYPE, ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = RequestStatusConstraint.RequestStatusValidator.class)
@Documented
public @interface RequestStatusConstraint {
    String message() default "The request status must not be UNRECOGNIZED";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};

    public static class RequestStatusValidator implements ConstraintValidator<RequestStatusConstraint, RequestStatus> {

        @Override
        public void initialize(RequestStatusConstraint constraintAnnotation) {
        }

        @Override
        public boolean isValid(RequestStatus value, ConstraintValidatorContext context) {
            return value != RequestStatus.UNRECOGNIZED;
        }
    }
}
