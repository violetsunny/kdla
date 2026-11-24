package top.kdla.framework.validator;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import top.kdla.framework.dto.exception.ErrorCode;
import top.kdla.framework.exception.BizException;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 通用的默认校验器。
 * <p>
 * 校验消息插值使用JVM默认Locale；此前实例初始化块中Locale.setDefault(Locale.CHINA)
 * 属JVM全局副作用（会影响宿主应用全部Locale敏感逻辑），已移除。
 * 需要中文消息的宿主请自行在启动参数/入口设置Locale。
 * <p>
 * 2.x直接用jakarta Validator校验：fluent-validator-jsr303桥接器只认javax.validation.Validator，
 * 无法接纳jakarta实例，故不再经FluentValidator桥接。
 *
 * @author kll
 */
public class CommonValidator implements AutoCloseable {

    protected volatile Validator validator;

    private ValidatorFactory factory;

    public <T> void defaultValidate(T t) {
        if (null == t) {
            throw new BizException(ErrorCode.PARAMETER_ERROR.getCode(), "请求参数错误,提示信息: %s", "请求对象为null");
        }

        //懒加载：此前实例初始化块急建factory，下游只有validation-api无实现时构造即NoProviderFoundException
        if (validator == null) {
            synchronized (this) {
                if (validator == null) {
                    factory = Validation.buildDefaultValidatorFactory();
                    validator = factory.getValidator();
                }
            }
        }
        Set<ConstraintViolation<T>> violations = validator.validate(t);
        if (!violations.isEmpty()) {
            List<String> errors = violations.stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .collect(Collectors.toList());
            throw new BizException(ErrorCode.PARAMETER_ERROR.getCode(), "请求参数错误,提示信息: %s", errors);
        }
    }

    /**
     * 释放ValidatorFactory（Spring Bean销毁时回调），此后再校验会重新懒加载
     */
    @Override
    public void close() {
        synchronized (this) {
            if (factory != null) {
                factory.close();
                factory = null;
                validator = null;
            }
        }
    }

}