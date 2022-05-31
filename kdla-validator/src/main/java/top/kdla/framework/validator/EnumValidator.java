package top.kdla.framework.validator;


import top.kdla.framework.common.enums.IEnum;
import top.kdla.framework.validator.annotation.ValidateEnum;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

/**
 * 枚举校验器
 *
 * @author kll
 * @date 2021/10/3 10:38
 */
public class EnumValidator implements ConstraintValidator<ValidateEnum,String> {

    private Class<? extends Enum> enumClazz;

    private boolean checkForNull;

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        Enum[] enumConstants = enumClazz.getEnumConstants();
        for (Enum enumConstant : enumConstants) {
            IEnum iEnum = (IEnum) enumConstant;
            if (checkForNull && value == null) {
                return false;
            }
            if (iEnum.getCode().equals(value)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void initialize(ValidateEnum constraintAnnotation) {
        enumClazz = constraintAnnotation.value();
        checkForNull = constraintAnnotation.checkForNull();
    }
}
