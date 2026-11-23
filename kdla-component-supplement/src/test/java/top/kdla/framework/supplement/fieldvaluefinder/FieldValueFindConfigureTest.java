package top.kdla.framework.supplement.fieldvaluefinder;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 清单 H37 配套回归测试：FieldValueFindConfigure 装配开关与用户自定义优先。
 *
 * <p>旧实现缺陷：类上无 @ConditionalOnProperty，无法关闭字段转换装配；
 * @Bean 无 @ConditionalOnMissingBean，用户自定义 FieldValueFindHelp 会被框架默认实例覆盖。
 *
 * <p>修复后：kdla.config.field-value-find.enabled 默认 true 可关闭；用户已注册同类型 Bean 时不再创建默认实例。
 *
 * <p>无修复则失败：旧代码 enabled=false 仍创建 Bean，且用户自定义实例被覆盖。
 */
class FieldValueFindConfigureTest {

    @Test
    void defaultContext_helperAssembled() {
        new ApplicationContextRunner()
                .withUserConfiguration(FieldValueFindConfigure.class)
                .run(ctx -> {
                    assertTrue(ctx.containsBean("fieldValueFindHelper"));
                    assertInstanceOf(FieldValueFindHelp.class, ctx.getBean("fieldValueFindHelper"));
                });
    }

    @Test
    void disabled_helperNotAssembled() {
        new ApplicationContextRunner()
                .withUserConfiguration(FieldValueFindConfigure.class)
                .withPropertyValues("kdla.config.field-value-find.enabled=false")
                .run(ctx -> assertFalse(ctx.containsBean("fieldValueFindHelper")));
    }

    @Test
    void userDefinedHelper_takesPrecedence() {
        FieldValueFindHelp custom = new FieldValueFindHelp();
        new ApplicationContextRunner()
                .withUserConfiguration(FieldValueFindConfigure.class)
                .withBean(FieldValueFindHelp.class, () -> custom)
                .run(ctx -> {
                    assertSame(custom, ctx.getBean(FieldValueFindHelp.class));
                    assertFalse(ctx.containsBean("fieldValueFindHelper"));
                });
    }
}
