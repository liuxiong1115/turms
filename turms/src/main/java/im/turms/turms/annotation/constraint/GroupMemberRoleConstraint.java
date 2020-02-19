package im.turms.turms.annotation.constraint;

import im.turms.common.constant.GroupMemberRole;

import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;
import java.lang.annotation.*;

@Target({ElementType.PARAMETER, ElementType.TYPE, ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = GroupMemberRoleConstraint.GroupMemberRoleValidator.class)
@Documented
public @interface GroupMemberRoleConstraint {
    String message() default "The group member role must not be UNRECOGNIZED";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    public static class GroupMemberRoleValidator implements ConstraintValidator<GroupMemberRoleConstraint, GroupMemberRole> {

        @Override
        public void initialize(GroupMemberRoleConstraint constraintAnnotation) {
        }

        @Override
        public boolean isValid(GroupMemberRole value, ConstraintValidatorContext context) {
            return value != GroupMemberRole.UNRECOGNIZED;
        }
    }
}
