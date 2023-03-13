package top.kdla.framework.validator.annotation;


import top.kdla.framework.dto.IEnum;

import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.stream.Stream;

/**
 * 枚举校验注解 IEnum
 *
 * @author kll
 * @date 2021/10/3 10:36
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = IEnumValid.IEnumValidator.class)
public @interface IEnumValid {

    /**
     * 必须是继承 IEnum 的枚举才能检验
     * @return
     */
    Class<? extends IEnum> value();

    String message() default "未找到相关枚举值";

    Class<?>[] groups() default { };

    Class<? extends Payload>[] payload() default { };

    boolean checkForNull() default true;

    /**
     * 枚举校验器  实现IEnum接口
     */
    class IEnumValidator implements ConstraintValidator<IEnumValid,String> {

        private Class<? extends IEnum> enumClazz;

        private boolean checkForNull;

        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            if (checkForNull && value == null) {
                return false;
            }
            return Stream.of(enumClazz.getEnumConstants()).anyMatch(enumConstant -> enumConstant.getCode().equals(value));
        }

        @Override
        public void initialize(IEnumValid constraintAnnotation) {
            enumClazz = constraintAnnotation.value();
            checkForNull = constraintAnnotation.checkForNull();
        }
    }
}

