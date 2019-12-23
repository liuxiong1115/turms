package im.turms.turms.annotation.constraint;

import im.turms.turms.pojo.domain.MessageStatus;

import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;
import java.lang.annotation.*;

@Target({ElementType.PARAMETER, ElementType.TYPE, ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = MessageStatusKeyConstraint.MessageStatusKeyValidator.class)
@Documented
public @interface MessageStatusKeyConstraint {
    String message() default "MessageStatusKeyConstraint";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    public static class MessageStatusKeyValidator implements ConstraintValidator<MessageStatusKeyConstraint, MessageStatus.Key> {

        @Override
        public void initialize(MessageStatusKeyConstraint constraintAnnotation) {
        }

        @Override
        public boolean isValid(MessageStatus.Key value, ConstraintValidatorContext context) {
            return value != null && value.getMessageId() != null && value.getRecipientId() != null;
        }
    }
}
