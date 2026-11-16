package top.kdla.framework.validator;

import com.baidu.unbiz.fluentvalidator.FluentValidator;
import com.baidu.unbiz.fluentvalidator.Result;
import com.baidu.unbiz.fluentvalidator.jsr303.HibernateSupportedValidator;

import top.kdla.framework.dto.exception.ErrorCode;
import top.kdla.framework.exception.BizException;

import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import static com.baidu.unbiz.fluentvalidator.ResultCollectors.toSimple;

/**
 * 通用的默认校验器。
 * <p>
 * 校验消息插值使用JVM默认Locale；此前实例初始化块中Locale.setDefault(Locale.CHINA)
 * 属JVM全局副作用（会影响宿主应用全部Locale敏感逻辑），已移除。
 * 需要中文消息的宿主请自行在启动参数/入口设置Locale。
 *
 * @author kll
 */
public class CommonValidator {

    protected Validator validator;

    {
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
