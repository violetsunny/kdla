package top.kdla.framework.validator.annotation;

import org.springframework.util.StringUtils;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

        private static final Logger log = LoggerFactory.getLogger(EnumValid.class);

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
                // JSR-303约定：null值放行，交由@NotNull负责非空校验（此前返回FALSE导致可选字段恒不通过）
                return Boolean.TRUE;
            }

            Class<?> valueClass = value.getClass();

            if (!StringUtils.hasText(enumMethod)) {
                return Boolean.FALSE;
            }
            try {
                Method method = findCheckMethod(valueClass);
                if (method == null) {
                    return Boolean.FALSE;
                }
                if (!Boolean.TYPE.equals(method.getReturnType()) && !Boolean.class.equals(method.getReturnType())) {
                    return Boolean.FALSE;
                }

                if (!Modifier.isStatic(method.getModifiers())) {
                    return Boolean.FALSE;
                }

                Boolean result;
                if (method.getParameterCount() == 0) {
                    result = (Boolean) method.invoke(null);
                } else {
                    result = (Boolean) method.invoke(null, value);
                }
                return result != null && result;
            } catch (Exception e) {
                log.warn("EnumValid check failed for value: {}", value, e);
                return Boolean.FALSE;
            }

        }

        /**
         * 按方法名+参数可赋值性查找校验方法。
         * getMethod精确匹配要求参数类型与value运行时类型完全一致，
         * 校验方法参数声明为父类型（如CharSequence/Object）时永远找不到。
         * 优先返回参数可接纳valueClass的单参方法，找不到再回退无参方法，均无则返回null。
         *
         * @param valueClass 被校验值的运行时类型
         * @return 命中的校验方法，未找到返回null
         */
        private Method findCheckMethod(Class<?> valueClass) {
            Method noArgFallback = null;
            for (Method method : enumClass.getMethods()) {
                if (!method.getName().equals(enumMethod)) {
                    continue;
                }
                Class<?>[] paramTypes = method.getParameterTypes();
                if (paramTypes.length == 1 && paramTypes[0].isAssignableFrom(valueClass)) {
                    return method;
                }
                if (paramTypes.length == 0 && noArgFallback == null) {
                    noArgFallback = method;
                }
            }
            return noArgFallback;
        }

    }
}
