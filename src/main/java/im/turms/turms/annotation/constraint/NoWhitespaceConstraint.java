package im.turms.turms.annotation.constraint;

import org.springframework.util.StringUtils;

import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;
import java.lang.annotation.*;

@Target({ElementType.PARAMETER, ElementType.TYPE, ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = NoWhitespaceConstraint.NoWhitespaceValidator.class)
@Documented
public @interface NoWhitespaceConstraint {
    String message() default "NoWhitespaceConstraint";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    public static class NoWhitespaceValidator implements ConstraintValidator<NoWhitespaceConstraint, String> {

        @Override
        public void initialize(NoWhitespaceConstraint constraintAnnotation) {
        }

        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            return !StringUtils.containsWhitespace(value);
        }
    }
}
