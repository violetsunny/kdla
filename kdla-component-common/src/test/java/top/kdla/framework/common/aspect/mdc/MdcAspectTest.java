package top.kdla.framework.common.aspect.mdc;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.context.support.StaticApplicationContext;
import top.kdla.framework.common.help.MdcHelp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 回归测试（T 语义：@MdcDot 的 bizCode 表达式值写入 "bizCode" 键，traceId 键为 T 的 "traceId"）：
 * 1. 旧实现在 finally 中 MDC.clear() 清空整个 MDC，嵌套 @MdcDot 场景外层 traceId/bizCode 与其他 MDC 键全部丢失；
 *    修复后进入切面时保存 "bizCode" 旧值，finally 精确恢复。
 * 2. 旧实现 SpEL parseExpression(key).getValue 无保护，表达式非法时异常直接炸掉业务方法；
 *    修复后降级为空串，业务方法正常执行。
 */
class MdcAspectTest {

    private static final String BIZ_CODE_KEY = "bizCode";

    @AfterEach
    void cleanup() {
        MDC.clear();
    }

    static class InnerService {
        @MdcDot(bizCode = "'inner-' + #name")
        public String inner(String name) {
            return MDC.get(BIZ_CODE_KEY);
        }
    }

    static class OuterService {
        private final InnerService inner;

        OuterService(InnerService inner) {
            this.inner = inner;
        }

        @MdcDot(bizCode = "'outer-' + #name")
        public String[] outer(String name) {
            String outerVal = MDC.get(BIZ_CODE_KEY);
            String innerVal = inner.inner(name);
            String afterInner = MDC.get(BIZ_CODE_KEY);
            return new String[]{outerVal, innerVal, afterInner};
        }
    }

    static class BadSpelService {
        @MdcDot(bizCode = "(#name")
        public String run(String name) {
            return "ok:" + MDC.get(BIZ_CODE_KEY);
        }
    }

    private static MdcAspect newAspect() {
        MdcAspect aspect = new MdcAspect();
        aspect.setApplicationContext(new StaticApplicationContext());
        return aspect;
    }

    private static <T> T proxy(T target) {
        AspectJProxyFactory factory = new AspectJProxyFactory(target);
        factory.addAspect(newAspect());
        return factory.getProxy();
    }

    @Test
    void nestedMdcDot_outerBizCodeAndOtherKeysNotLost() {
        OuterService outer = proxy(new OuterService(proxy(new InnerService())));
        MDC.put(MdcHelp.TRACE_ID_KEY, "trace-origin");
        MDC.put(BIZ_CODE_KEY, "biz-origin");

        String[] result = outer.outer("x");

        assertEquals("outer-x", result[0], "外层方法内 bizCode 应为外层表达式值");
        assertEquals("inner-x", result[1], "内层方法内 bizCode 应为内层表达式值");
        assertEquals("outer-x", result[2], "内层切面返回后必须恢复外层 bizCode，不能被 clear 清空");
        assertEquals("trace-origin", MDC.get(MdcHelp.TRACE_ID_KEY), "全部结束后 traceId 必须恢复，不能被 MDC.clear() 误清");
        assertEquals("biz-origin", MDC.get(BIZ_CODE_KEY), "全部结束后必须恢复进入切面前的原始 bizCode");
    }

    @Test
    void noPreviousBizCode_removedAfterExit() {
        InnerService inner = proxy(new InnerService());

        String val = inner.inner("y");

        assertEquals("inner-y", val);
        assertNull(MDC.get(BIZ_CODE_KEY), "进入前无旧值时，退出后应移除该键而不是残留");
    }

    @Test
    void badSpel_degradesToEmptyAndBusinessMethodStillRuns() {
        BadSpelService service = proxy(new BadSpelService());

        String result = service.run("n");

        assertEquals("ok:", result, "SpEL 解析失败必须降级为空串，且不影响业务方法执行");
    }
}
