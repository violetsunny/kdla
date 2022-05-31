package top.kdla.framework.validator;


import top.kdla.framework.dto.ErrorCode;
import top.kdla.framework.dto.ErrorCodeI;
import top.kdla.framework.exception.BizException;

import java.util.Collection;
import java.util.Map;

/**
 * Assertion utility class that assists in validating arguments.
 *
 * <p>Useful for identifying programmer errors early and clearly at runtime.
 *
 * <p>For example, if the contract of a public method states it does not
 * allow {@code null} arguments, {@code Assert} can be used to validate that
 * contract.
 *
 * For example:
 *
 * <pre class="code">
 * Assert.notNull(clazz, "The class must not be null");
 * Assert.isTrue(i > 0, "The value must be greater than zero");</pre>
 *
 * This class is empowered by  {@link org.springframework.util.Assert}
 *
 * @author kll
 * @since 2021/7/9 14:15
 */
public abstract class BaseAssert {

    /**
     * Assert a boolean expression, throwing {@code BizException}
     *
     * for example
     *
     * <pre class="code">Assert.isTrue(i != 0, code.B_ORDER_illegalNumber, "The order number can not be zero");</pre>
     *
     * @param expression a boolean expression
     * @param code
     * @param message the exception message to use if the assertion fails
     * @throws BizException if expression is {@code false}
     */
    public static void isTrue(boolean expression, String code, String message) {
        if (!expression) {
            throw new BizException(code, message);
        }
    }

    /**
     * Assert a boolean expression, if expression is true, throwing {@code BizException}
     *
     * for example
     *
     * <pre class="code">Assert.isFalse(i == 0, code.B_ORDER_illegalNumber, "The order number can not be zero");</pre>
     *
     * This is more intuitive than isTure.
     */
    public static void isFalse(boolean expression, String code, String message) {
        if (expression) {
            throw new BizException(code, message);
        }
    }

    public static void isTrue(boolean expression, String message) {
        if (!expression) {
            throw new BizException(message);
        }
    }

    public static void isFalse(boolean expression, String message) {
        if (expression) {
            throw new BizException(message);
        }
    }

    public static void isTrue(boolean expression) {
        isTrue(expression, "[Assertion failed] Must be true");
    }

    public static void isFalse(boolean expression) {
        isFalse(expression, "[Assertion failed] Must be false");
    }

    public static void notNull(Object object, String code, String message) {
        if (object == null) {
            throw new BizException(code, message);
        }
    }

    public static void notNull(Object object, ErrorCodeI errorCodeI, String message) {
        if (object == null) {
            throw new BizException(errorCodeI, message);
        }
    }

    public static void notNull(Object object, String message) {
        if (object == null) {
            throw new BizException(message);
        }
    }

    public static void notNull(Object object) {
        notNull(object, "[Assertion failed] Must not null");
    }

    public static void notEmpty(Collection<?> collection, String code, String message) {
        if (collection == null || collection.isEmpty()) {
            throw new BizException(code, message);
        }
    }

    public static void notEmpty(Collection<?> collection, ErrorCodeI errorCodeI, String message) {
        if (collection == null || collection.isEmpty()) {
            throw new BizException(errorCodeI, message);
        }
    }


    public static void notEmpty(Collection<?> collection, String message) {
        if (collection == null || collection.isEmpty()) {
            throw new BizException(message);
        }
    }

    public static void notEmpty(Collection<?> collection) {
        notEmpty(collection, "[Assertion failed] Collection must not be empty: it must contain at least 1 element");
    }

    public static void notEmpty(Map<?, ?> map, String code, String message) {
        if (map == null || map.isEmpty()) {
            throw new BizException(code, message);
        }
    }

    public static void notEmpty(Map<?, ?> map, ErrorCodeI errorCodeI, String message) {
        if (map == null || map.isEmpty()) {
            throw new BizException(errorCodeI, message);
        }
    }

    public static void notEmpty(Map<?, ?> map, String message) {
        if (map == null || map.isEmpty()) {
            throw new BizException(message);
        }
    }

    public static void notEmpty(Map<?, ?> map) {
        notEmpty(map, "[Assertion failed] Map must not be empty: it must contain at least one entry");
    }

    public static void isBlank(String str, String code, String message) {
        if (null == str || "".equals(str)) {
            throw new BizException(code, message);
        }
    }

    public static void isBlank(String str, ErrorCodeI errorCodeI, String message) {
        if (null == str || "".equals(str)) {
            throw new BizException(errorCodeI, message);
        }
    }

    public static void isBlank(String str, String message) {
        if (null == str || "".equals(str)) {
            throw new BizException(ErrorCode.PARAMETER_ERROR, message);
        }
    }

    public static void isNull(Object object, String code, String message) {
        if (object == null) {
            throw new BizException(code, message);
        }
    }

}
