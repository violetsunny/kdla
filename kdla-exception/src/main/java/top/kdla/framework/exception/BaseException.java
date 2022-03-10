package top.kdla.framework.exception;

/**
 * Base Exception is the parent of all exceptions
 *
 * @author vincent.li
 * @since 2021/7/9 14:15
 */
public abstract class BaseException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private String code;

    public BaseException(String message) {
        super(message);
    }

    public BaseException(String code, String message) {
        super(message);
        this.code = code;
    }

    public BaseException(String message, Throwable e) {
        super(message, e);
    }

    public BaseException(String code, String message, Throwable e) {
        super(message, e);
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

}
