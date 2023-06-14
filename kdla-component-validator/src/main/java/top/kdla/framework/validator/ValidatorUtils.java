package top.kdla.framework.validator;


import top.kdla.framework.dto.ErrorCode;
import top.kdla.framework.exception.BizException;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import java.util.Set;

/**
 * hibernate-validator校验工具类
 *
 * @author kll
 * @since 2021/7/9 14:15
 */
public class ValidatorUtils {
    private static Validator validator;

    static {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    /**
     * 校验对象
     *
     * @param object 待校验对象
     * @param groups 待校验的组
     * @throws BizException 校验不通过，则报BizException异常
     */
    public static void validate(Object object, Class<?>... groups)
            throws BizException {
        Set<ConstraintViolation<Object>> constraintViolations = validator.validate(object, groups);
        if (!constraintViolations.isEmpty()) {
            StringBuilder msg = new StringBuilder();
            for (ConstraintViolation<Object> constraint : constraintViolations) {
                msg.append(constraint.getMessage()).append("<br>");
            }
            throw new BizException(ErrorCode.PARAMETER_ERROR,msg.toString());
        }
    }

    /**
     * 校验对象
     *
     * @param object 待校验对象
     * @throws BizException 校验不通过，则报BizException异常
     */
    public static void validate(Object object)
            throws BizException {
        Set<ConstraintViolation<Object>> constraintViolations = validator.validate(object);
        if (!constraintViolations.isEmpty()) {
            StringBuilder msg = new StringBuilder();
            for (ConstraintViolation<Object> constraint : constraintViolations) {
                msg.append(constraint.getMessage()).append("<br>");
            }
            throw new BizException(ErrorCode.PARAMETER_ERROR,msg.toString());
        }
    }
}
