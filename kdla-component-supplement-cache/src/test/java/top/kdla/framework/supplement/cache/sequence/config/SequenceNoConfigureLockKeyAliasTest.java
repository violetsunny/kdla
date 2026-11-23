package top.kdla.framework.supplement.cache.sequence.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.mock.env.MockEnvironment;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 回归 C4：sequenceNoLockKey 恢复旧别名 kdla.sequence.lock-key-prefix 的嵌套默认值。
 * 无修复则失败：旧表达式无嵌套别名，legacyAlias 用例解析为默认值而非别名值。
 */
class SequenceNoConfigureLockKeyAliasTest {

    private String lockKeyExpression() throws NoSuchFieldException {
        Field field = SequenceNoConfigure.class.getDeclaredField("sequenceNoLockKey");
        return field.getAnnotation(Value.class).value();
    }

    @Test
    void noProperty_resolvesToDefault() throws NoSuchFieldException {
        assertThat(new StandardEnvironment().resolvePlaceholders(lockKeyExpression()))
                .isEqualTo("top::sequenceNo::");
    }

    @Test
    void legacyAlias_resolvesToAliasValue() throws NoSuchFieldException {
        MockEnvironment env = new MockEnvironment()
                .withProperty("kdla.sequence.lock-key-prefix", "legacy::");
        assertThat(env.resolvePlaceholders(lockKeyExpression())).isEqualTo("legacy::");
    }

    @Test
    void primaryKeyWins_overAlias() throws NoSuchFieldException {
        MockEnvironment env = new MockEnvironment()
                .withProperty("kdla.sequence.lock.key.prefix", "primary::")
                .withProperty("kdla.sequence.lock-key-prefix", "legacy::");
        assertThat(env.resolvePlaceholders(lockKeyExpression())).isEqualTo("primary::");
    }
}