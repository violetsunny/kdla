package top.kdla.framework.dto;

import top.kdla.framework.dto.exception.ErrorCodeI;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

/**
 * Response with batch record to return,
 * usually use in conditional query
 * <p/>
 * @author kll
 * @since 2021/7/9 14:15
 */
public class MultiResponse<T> extends Response {

    private static final long serialVersionUID = 1L;

    private Collection<T> data;

    public Collection<T> getData() {
        return this.data;
    }

    public void setData(Collection<T> data) {
        this.data = data;
    }

    public static MultiResponse buildSuccess() {
        return buildSuccess(Collections.emptyList());
    }

    public static <T> MultiResponse<T> buildSuccess(Collection<T> items) {

        return buildSuccess(items, Objects.nonNull(items) ? items.size() : 0);
    }

    public static <T> MultiResponse<T> buildSuccess(Collection<T> items, int totalCount) {
        MultiResponse<T> response = new MultiResponse<>();
        response.setSuccess();
        response.setData(items);
        return response;
    }

    public static MultiResponse buildFailure(String code, String message) {
        MultiResponse response = new MultiResponse();
        response.setSuccess(false);
        response.setCode(code);
        response.setMsg(message);
        response.setMessage(message);
        return response;
    }

    public static MultiResponse buildFailure(ErrorCodeI error) {
        return buildFailure(error.getCode(), error.getMsg());
    }

    public static <T> MultiResponse<T> of(Collection<T> items) {
        return buildSuccess(items, items.size());
    }


}
