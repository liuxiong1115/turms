package im.turms.turms.annotation.constraint;

import org.springframework.scheduling.support.CronSequenceGenerator;

import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;
import java.lang.annotation.*;

@Target({ElementType.PARAMETER, ElementType.TYPE, ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = CronConstraint.CronValidator.class)
@Documented
public @interface CronConstraint {
    String message() default "The cron expression is invalid";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    public static class CronValidator implements ConstraintValidator<CronConstraint, String> {

        @Override
        public void initialize(CronConstraint constraintAnnotation) {
        }

        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            return CronSequenceGenerator.isValidExpression(value);
        }
    }
}
