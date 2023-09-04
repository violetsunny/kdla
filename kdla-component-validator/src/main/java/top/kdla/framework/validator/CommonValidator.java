package top.kdla.framework.validator;

import com.baidu.unbiz.fluentvalidator.FluentValidator;
import com.baidu.unbiz.fluentvalidator.Result;
import com.baidu.unbiz.fluentvalidator.jsr303.HibernateSupportedValidator;
import org.springframework.stereotype.Component;
import top.kdla.framework.dto.exception.ErrorCode;
import top.kdla.framework.exception.BizException;

import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.util.Locale;

import static com.baidu.unbiz.fluentvalidator.ResultCollectors.toSimple;

/**
 * 通用的默认校验器
 * @author kll
 */
@Component
public class CommonValidator {

    protected Validator validator;

    {
        Locale.setDefault(Locale.CHINA);
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    public <T> void defaultValidate(T t) {
        if (null == t) {
            throw new BizException(ErrorCode.PARAMETER_ERROR.getCode(), "请求参数错误,提示信息: %s", "请求对象为null");
        }

        Result result = FluentValidator.checkAll()
            .on(t, new HibernateSupportedValidator().setHiberanteValidator(validator))
            .doValidate()
            .result(toSimple());

        if (!result.isSuccess()) {
            throw new BizException(ErrorCode.PARAMETER_ERROR.getCode(), "请求参数错误,提示信息: %s", result.getErrors());
        }
    }

}
