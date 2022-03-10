package top.kdla.framework.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * Response to caller
 *  @author vincent.li
 *  @since 2021/7/9 14:15
 */
@Getter
@Setter
public class Response extends BaseDto {

    private static final long serialVersionUID = 1L;

    private boolean success;

    private String code;

    private String message;

    @Override
    public String toString() {
        return "Response [success=" + success + ", code=" + code + ", message=" + message + "]";
    }

    public static Response buildSuccess() {
        Response response = new Response();
        response.setSuccess();
        return response;
    }

    public static <T> SingleResponse<T> buildSuccess(T data) {
        SingleResponse response = new SingleResponse();
        response.setSuccess();
        response.setData(data);
        return response;
    }

    public static Response buildFailure(String code, String message) {
        Response response = new Response();
        response.setSuccess(false);
        response.setCode(code);
        response.setMessage(message);
        return response;
    }

    public static Response buildFailure(ErrorCodeI error) {
        return buildFailure(error.getCode(), error.getMsg());
    }

    public boolean isSuccess() {
        return ErrorCode.SUCCESS.getCode().equals(this.code);
    }

    protected void setSuccess() {
        this.code = ErrorCode.SUCCESS.getCode();
    }


}
