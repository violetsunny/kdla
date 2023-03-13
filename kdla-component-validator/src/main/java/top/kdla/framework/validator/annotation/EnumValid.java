package top.kdla.framework.validator.annotation;

import org.springframework.util.StringUtils;

import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * 通过指定方法校验
 * @author dongguo.tao
 * @description
 * @date 2021-08-11 10:10:57
 */
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = EnumValid.Validator.class)
public @interface EnumValid {

    String message() default "{EnumValidator's value is invalid}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    Class<? extends Enum<?>> enumClass();

    /**
     * 必须要有指定校验方法
     * @return
     */
    String checkMethod();

    class Validator implements ConstraintValidator<EnumValid, Object> {

        private Class<? extends Enum<?>> enumClass;

        private String enumMethod;


        @Override
        public void initialize(EnumValid enumValue) {
            enumMethod = enumValue.checkMethod();
            enumClass = enumValue.enumClass();
        }

        @Override
        public boolean isValid(Object value, ConstraintValidatorContext constraintValidatorContext) {
            if (value == null) {
                return Boolean.FALSE;
            }

            Class<?> valueClass = value.getClass();

            try {
                Method method;
                if (StringUtils.hasText(enumMethod)){
                    method = enumClass.getMethod(enumMethod, valueClass);
                }else{
                    method = enumClass.getMethod(enumMethod);
                }
                if (!Boolean.TYPE.equals(method.getReturnType()) && !Boolean.class.equals(method.getReturnType())) {
                    return Boolean.FALSE;
                }

                if (!Modifier.isStatic(method.getModifiers())) {
                    return Boolean.FALSE;
                }

                Boolean result = (Boolean) method.invoke(null, value);
                return result != null && result;
            } catch (Exception e) {
                e.printStackTrace();
                return Boolean.FALSE;
            }

        }

    }
}
