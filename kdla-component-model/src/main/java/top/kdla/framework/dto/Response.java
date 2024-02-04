package top.kdla.framework.dto;

import lombok.Getter;
import lombok.Setter;
import top.kdla.framework.dto.exception.ErrorCode;
import top.kdla.framework.dto.exception.ErrorCodeI;

/**
 * Response to caller
 *
 * @author kll
 * @since 2021/7/9 14:15
 */
@Getter
@Setter
public class Response extends BaseModel {

    private static final long serialVersionUID = 1L;

    private boolean success;

    private String code;

    private String msg;

    private String message;

    @Override
    public String toString() {
        return "Response [success=" + success + ", code=" + code + ", message=" + msg + "]";
    }

    public static Response buildSuccess() {
        Response response = new Response();
        response.setSuccess();
        return response;
    }

    public static Response buildFailure(String code, String message) {
        Response response = new Response();
        response.setSuccess(false);
        response.setCode(code);
        response.setMsg(message);
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
        this.msg = ErrorCode.SUCCESS.getMsg();
        this.message = ErrorCode.SUCCESS.getMsg();
    }


}
