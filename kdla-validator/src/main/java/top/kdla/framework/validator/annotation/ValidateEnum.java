package top.kdla.framework.validator.annotation;


import top.kdla.framework.validator.EnumValidator;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 枚举校验注解
 *
 * @author kll
 * @date 2021/10/3 10:36
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = EnumValidator.class)
public @interface ValidateEnum {

    Class<? extends Enum> value();

    String message() default "未找到相关枚举值";

    Class<?>[] groups() default { };

    Class<? extends Payload>[] payload() default { };

    boolean checkForNull() default true;

}

