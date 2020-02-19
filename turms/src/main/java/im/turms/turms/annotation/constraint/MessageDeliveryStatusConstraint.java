package im.turms.turms.annotation.constraint;

import im.turms.common.constant.MessageDeliveryStatus;

import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;
import java.lang.annotation.*;

@Target({ElementType.PARAMETER, ElementType.TYPE, ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = MessageDeliveryStatusConstraint.MessageDeliveryStatusValidator.class)
@Documented
public @interface MessageDeliveryStatusConstraint {
    String message() default "The message delivery status must not be UNRECOGNIZED";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    public static class MessageDeliveryStatusValidator implements ConstraintValidator<MessageDeliveryStatusConstraint, MessageDeliveryStatus> {

        @Override
        public void initialize(MessageDeliveryStatusConstraint constraintAnnotation) {
        }

        @Override
        public boolean isValid(MessageDeliveryStatus value, ConstraintValidatorContext context) {
            return value != MessageDeliveryStatus.UNRECOGNIZED;
        }
    }
}
