package top.kdla.framework.exception;

import org.junit.jupiter.api.Test;
import top.kdla.framework.dto.Response;
import top.kdla.framework.dto.SingleResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 回归测试：handleKdlaResponse 旧实现只回填 code/msg，漏掉 message 字段，
 * 与 Response.buildFailure（msg、message 双字段回填）不一致，宿主取 getMessage() 得到 null。
 */
class ResponseHandlerTest {

    @Test
    void handle_kdlaSubReturnType_fillsMessage() {
        Object result = ResponseHandler.handle(SingleResponse.class, "500", "boom");

        assertTrue(result instanceof SingleResponse);
        SingleResponse<?> response = (SingleResponse<?>) result;
        assertEquals("500", response.getCode());
        assertEquals("boom", response.getMsg());
        assertEquals("boom", response.getMessage(), "handleKdlaResponse 必须回填 message，与 buildFailure 对齐");
        assertFalse(response.isSuccess());
    }

    @Test
    void handle_responseReturnType_fillsMessage() {
        Object result = ResponseHandler.handle(Response.class, "500", "boom");

        assertTrue(result instanceof Response);
        assertEquals("boom", ((Response) result).getMessage());
    }

    @Test
    void handle_nonKdlaReturnType_fallsBackToBuildFailure() {
        Object result = ResponseHandler.handle(String.class, "500", "boom");

        assertTrue(result instanceof Response);
        Response response = (Response) result;
        assertEquals("boom", response.getMsg());
        assertEquals("boom", response.getMessage());
    }

    @Test
    void handle_baseException_carriesCodeAndMessage() {
        Object result = ResponseHandler.handle(Response.class, new BizException("4001", "biz fail"));

        Response response = (Response) result;
        assertEquals("4001", response.getCode());
        assertEquals("biz fail", response.getMessage());
    }
}
