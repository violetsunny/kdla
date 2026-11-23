package top.kdla.framework.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 回归测试：isSuccess() 由 code 派生为唯一判定真源。
 * 本仓保留 boolean success 字段与 Lombok setSuccess(boolean)（B1 裁定不删除），
 * 该字段为历史遗留，不参与 isSuccess() 判定。
 */
class ResponseTest {

    @Test
    void buildSuccess_isSuccessTrue() {
        Response response = Response.buildSuccess();
        assertTrue(response.isSuccess());
        assertEquals("200", response.getCode());
    }

    @Test
    void buildFailure_isSuccessFalse_msgAndMessageAligned() {
        Response response = Response.buildFailure("500", "boom");
        assertFalse(response.isSuccess());
        assertEquals("boom", response.getMsg());
        assertEquals("boom", response.getMessage());
    }

    @Test
    void isSuccess_derivedFromCode() {
        Response response = new Response();
        assertFalse(response.isSuccess(), "code 为空时不能误判为成功");
        response.setCode("200");
        assertTrue(response.isSuccess(), "isSuccess 必须由 code 派生");
        response.setCode("2001");
        assertFalse(response.isSuccess());
    }

    @Test
    void subclassBuildFailure_isSuccessFalse() {
        assertFalse(SingleResponse.buildFailure("500", "boom").isSuccess());
        assertFalse(MultiResponse.buildFailure("500", "boom").isSuccess());
        assertFalse(PageResponse.buildFailure("500", "boom").isSuccess());
        assertTrue(SingleResponse.buildSuccess().isSuccess());
    }
}
