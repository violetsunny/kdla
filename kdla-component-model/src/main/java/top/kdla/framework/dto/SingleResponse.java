package top.kdla.framework.dto;

import top.kdla.framework.dto.exception.ErrorCodeI;

/**
 * Response with single record to return
 *
 * @author kll
 * @since 2021/7/9 14:15
 */
public class SingleResponse<T> extends Response {

    private static final long serialVersionUID = 1L;

    private T data;

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public static SingleResponse buildSuccess() {
        SingleResponse response = new SingleResponse();
        response.setSuccess();
        return response;
    }

    public static <T> SingleResponse<T> buildSuccess(T data) {
        SingleResponse<T> response = new SingleResponse<>();
        response.setSuccess();
        response.setData(data);
        return response;
    }

    public static SingleResponse buildFailure(String code, String message) {
        SingleResponse response = new SingleResponse();
        response.setSuccess(false);
        response.setCode(code);
        response.setMsg(message);
        return response;
    }

    public static SingleResponse buildFailure(ErrorCodeI error) {
        return buildFailure(error.getCode(), error.getMsg());
    }

    public static <T> SingleResponse<T> of(T data) {
        SingleResponse<T> response = new SingleResponse<>();
        response.setSuccess();
        response.setData(data);
        return response;
    }

}
