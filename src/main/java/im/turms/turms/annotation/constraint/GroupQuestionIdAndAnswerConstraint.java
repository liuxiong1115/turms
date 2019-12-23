package im.turms.turms.annotation.constraint;

import im.turms.turms.pojo.bo.group.GroupQuestionIdAndAnswer;

import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;
import java.lang.annotation.*;

@Target({ElementType.PARAMETER, ElementType.TYPE, ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = GroupQuestionIdAndAnswerConstraint.GroupQuestionIdAndAnswerValidator.class)
@Documented
public @interface GroupQuestionIdAndAnswerConstraint {
    String message() default "GroupQuestionIdAndAnswerConstraint";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    public static class GroupQuestionIdAndAnswerValidator implements ConstraintValidator<GroupQuestionIdAndAnswerConstraint, GroupQuestionIdAndAnswer> {

        @Override
        public void initialize(GroupQuestionIdAndAnswerConstraint constraintAnnotation) {
        }

        @Override
        public boolean isValid(GroupQuestionIdAndAnswer value, ConstraintValidatorContext context) {
            return value != null && value.getId() != null && value.getAnswer() != null;
        }
    }
}
