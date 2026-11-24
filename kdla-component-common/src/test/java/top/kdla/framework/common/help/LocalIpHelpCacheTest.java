package top.kdla.framework.common.help;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 回归 A-01：getLocalIp4Address 旧实现每次调用都枚举网卡/socket探测，traceId生成为每请求热路径，开销大。
 * 1.x 有 volatile 缓存解析一次后复用，2.x 迁移时丢失。修复后恢复缓存字段与短路逻辑。
 * 无修复则失败：缓存字段不存在（反射 NoSuchFieldException），且哨兵注入后仍重新计算返回真实IP。
 */
class LocalIpHelpCacheTest {

    private static Field cacheField() throws NoSuchFieldException {
        Field field = LocalIpHelp.class.getDeclaredField("localIp4AddressCache");
        field.setAccessible(true);
        return field;
    }

    @BeforeEach
    void resetCache() throws Exception {
        cacheField().set(null, null);
    }

    @Test
    void cacheField_isVolatile() throws Exception {
        assertTrue(Modifier.isVolatile(cacheField().getModifiers()), "localIp4AddressCache 必须为 volatile");
    }

    @Test
    void cachedValue_shortCircuitsRecompute() throws Exception {
        // 注入哨兵：命中缓存须直接返回，不再枚举网卡/socket探测（无修复则返回真实IP，断言失败）
        cacheField().set(null, "10.99.99.99");
        assertEquals("10.99.99.99", LocalIpHelp.getLocalIp4Address(), "缓存命中须短路返回缓存值");
    }

    @Test
    void firstCall_populatesCache() throws Exception {
        String ip = LocalIpHelp.getLocalIp4Address();
        assertNotNull(ip);
        assertEquals(ip, cacheField().get(null), "首次解析后须写入缓存供后续复用");
    }
}
