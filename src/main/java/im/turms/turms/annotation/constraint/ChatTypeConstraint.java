package im.turms.turms.annotation.constraint;

import im.turms.turms.constant.ChatType;

import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;
import java.lang.annotation.*;

@Target({ElementType.PARAMETER, ElementType.TYPE, ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ChatTypeConstraint.ChatTypeValidator.class)
@Documented
public @interface ChatTypeConstraint {
    String message() default "The chat type must not be UNRECOGNIZED";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    public static class ChatTypeValidator implements ConstraintValidator<ChatTypeConstraint, ChatType> {

        @Override
        public void initialize(ChatTypeConstraint constraintAnnotation) {
        }

        @Override
        public boolean isValid(ChatType value, ConstraintValidatorContext context) {
            return value != ChatType.UNRECOGNIZED;
        }
    }
}
