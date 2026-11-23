package top.kdla.framework.log.catchlog;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static top.kdla.framework.common.constants.CommonConstants.LOG_TRACE_ID;

/**
 * Verifies that CatchLogAspect.doBefore() does not throw IndexOutOfBoundsException
 * when called on a no-argument method, and that MDC traceId is still populated.
 */
class CatchLogAspectNoArgsTest {

    private CatchLogAspect aspect;

    @BeforeEach
    void setUp() {
        aspect = new CatchLogAspect();
        MDC.remove(LOG_TRACE_ID); // ensure clean state
    }

    @AfterEach
    void tearDown() {
        MDC.remove(LOG_TRACE_ID);
    }

    @Test
    void doBefore_noArgs_doesNotThrow_andSetsTraceId() {
        // Arrange: a JoinPoint whose getArgs() returns an empty array
        JoinPoint jp = mock(JoinPoint.class);
        Signature sig = mock(Signature.class);
        when(jp.getArgs()).thenReturn(new Object[0]);
        when(jp.getSignature()).thenReturn(sig);
        when(sig.getName()).thenReturn("noArgsMethod");

        // Act — must not throw IndexOutOfBoundsException
        aspect.doBefore(jp);

        // Assert — MDC must contain a traceId
        String traceId = MDC.get(LOG_TRACE_ID);
        assertNotNull(traceId, "MDC traceId must be set even for no-arg methods");
        assertFalse(traceId.isEmpty(), "MDC traceId must not be empty");
        // 回归C/M10：修复前doBefore放{"traceId":"xxx"}整段JSON，必须放裸traceId
        assertFalse(traceId.startsWith("{"), "MDC必须放裸traceId字符串，不得是JSON串");
    }

    @Test
    void doBefore_existingTraceId_isNotOverwritten() {
        // Arrange: MDC already has a traceId
        String existingTraceId = "existing-trace-123";
        MDC.put(LOG_TRACE_ID, existingTraceId);

        JoinPoint jp = mock(JoinPoint.class);
        when(jp.getArgs()).thenReturn(new Object[0]);

        // Act
        aspect.doBefore(jp);

        // Assert — existing traceId is preserved
        String traceId = MDC.get(LOG_TRACE_ID);
        assert existingTraceId.equals(traceId) : "Existing traceId must not be overwritten";
    }
}
