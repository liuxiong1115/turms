package im.turms.turms.annotation.constraint;

import im.turms.turms.pojo.domain.GroupMember;

import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;
import java.lang.annotation.*;

@Target({ElementType.PARAMETER, ElementType.TYPE, ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = GroupMemberKeyConstraint.GroupMemberKeyValidator.class)
@Documented
public @interface GroupMemberKeyConstraint {
    String message() default "GroupMemberKeyConstraint";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    public static class GroupMemberKeyValidator implements ConstraintValidator<GroupMemberKeyConstraint, GroupMember.Key> {

        @Override
        public void initialize(GroupMemberKeyConstraint constraintAnnotation) {
        }

        @Override
        public boolean isValid(GroupMember.Key value, ConstraintValidatorContext context) {
            return value != null && value.getGroupId() != null && value.getUserId() != null;
        }
    }
}
