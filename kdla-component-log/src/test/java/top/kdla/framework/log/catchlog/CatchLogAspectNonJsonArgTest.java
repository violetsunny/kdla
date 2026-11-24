package top.kdla.framework.log.catchlog;

import org.aspectj.lang.JoinPoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static top.kdla.framework.common.constants.CommonConstants.LOG_TRACE_ID;

/**
 * 回归测试：doBefore 曾对首个参数无保护地调用 JSONObject.parseObject，
 * 首参为普通字符串（如"张三"）时抛 JSONException，直接炸掉被切的业务方法。
 * 修复后解析失败必须降级为自动生成 traceId 写入 MDC，不影响业务方法执行。
 */
class CatchLogAspectNonJsonArgTest {

    private CatchLogAspect aspect;

    @BeforeEach
    void setUp() {
        aspect = new CatchLogAspect();
        MDC.remove(LOG_TRACE_ID);
    }

    @AfterEach
    void tearDown() {
        MDC.remove(LOG_TRACE_ID);
    }

    @Test
    void doBefore_nonJsonStringArg_doesNotThrow_andSetsTraceId() {
        // 首参为普通中文字符串，不是JSON
        JoinPoint jp = mock(JoinPoint.class);
        when(jp.getArgs()).thenReturn(new Object[]{"张三"});

        // 修复前：JSONObject.parseObject("张三") 抛 JSONException 向上传播
        assertDoesNotThrow(() -> aspect.doBefore(jp),
                "首参非JSON时 doBefore 不得抛异常炸掉业务方法");

        // 降级路径：必须自动生成traceId写入MDC
        String traceId = MDC.get(LOG_TRACE_ID);
        assertNotNull(traceId, "解析失败必须降级为自动生成traceId写入MDC");
        assertFalse(traceId.isEmpty(), "traceId不得为空串");
    }

    @Test
    void doBefore_jsonStringArgWithTraceId_stillExtractsTraceId() {
        // 原成功路径行为保持不变：首参为含traceId字段的JSON时提取其中的traceId
        JoinPoint jp = mock(JoinPoint.class);
        when(jp.getArgs()).thenReturn(new Object[]{"{\"" + LOG_TRACE_ID + "\":\"trace-from-arg\"}"});

        aspect.doBefore(jp);

        assertNotNull(MDC.get(LOG_TRACE_ID), "合法JSON首参路径必须照常写入MDC");
        assertEquals("trace-from-arg", MDC.get(LOG_TRACE_ID),
                "首参JSON携带traceId时必须原样提取，成功路径行为不得改变");
    }

    @Test
    void doBefore_jsonArgWithMaliciousTraceId_generatesOwn() {
        // D-12 同类回归：首参JSON携带含CRLF的traceId，旧实现直接进MDC，修复后必须自生成
        JoinPoint jp = mock(JoinPoint.class);
        when(jp.getArgs()).thenReturn(new Object[]{"{\"" + LOG_TRACE_ID + "\":\"abc\\r\\nforged\"}"});

        aspect.doBefore(jp);

        String traceId = MDC.get(LOG_TRACE_ID);
        assertNotNull(traceId);
        assertFalse(traceId.contains("\r"), "非法traceId不得进MDC");
        assertFalse(traceId.contains("\n"), "非法traceId不得进MDC");
    }
}
