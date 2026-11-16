package top.kdla.framework.supplement.cache.cache;

import cn.hutool.core.lang.generator.SnowflakeGenerator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * P1 回归测试：MachineIdHelp.init 失败路径不残留脏 machineId/dataCenterId。
 *
 * <p>旧实现在 SETNX 之前先给静态字段赋值，Redis 异常或 1024 槽耗尽时
 * 字段停留在未注册成功的脏值上，getGenerator 判空不命中、绕过随机回退，
 * 静默用脏 ID 发雪花号 → 多实例撞号 → 主键冲突。
 *
 * <p>修复后：注册成功才发布到静态字段；失败路径字段保持 null，
 * getGenerator 判空命中走随机回退（带 ERROR 告警）。
 *
 * <p>无修复则失败：旧代码在 lock 前已赋值，下方 assertNull 断言不通过。
 */
@ExtendWith(MockitoExtension.class)
class MachineIdHelpFailPathTest {

    private static final String APP_ID = "mach-id-failpath-app";

    @Mock
    private SpringRedisHelp redisHelp;

    @BeforeEach
    void resetStaticIds() throws Exception {
        setStaticId("machineId", null);
        setStaticId("dataCenterId", null);
    }

    @AfterEach
    void clearStaticIds() throws Exception {
        setStaticId("machineId", null);
        setStaticId("dataCenterId", null);
    }

    @Test
    void init_redisException_noDirtyId_fallsBackToRandom() throws Exception {
        when(redisHelp.lock(anyString(), anyString(), anyLong()))
                .thenThrow(new RuntimeException("redis down"));

        MachineIdHelp help = new MachineIdHelp(redisHelp, APP_ID);

        // 修复验证点1：异常路径静态字段不残留脏值
        assertNull(getStaticId("machineId"), "Redis异常后machineId应保持null");
        assertNull(getStaticId("dataCenterId"), "Redis异常后dataCenterId应保持null");

        // 修复验证点2：getGenerator 判空命中，走随机回退而非静默用脏值
        SnowflakeGenerator generator = help.getGenerator();
        assertNotNull(generator, "随机回退应产出可用generator");
        Integer machineId = getStaticId("machineId");
        Integer dataCenterId = getStaticId("dataCenterId");
        assertNotNull(machineId);
        assertNotNull(dataCenterId);
        assertTrue(machineId >= 0 && machineId <= 31, "回退machineId应在0~31");
        assertTrue(dataCenterId >= 0 && dataCenterId <= 31, "回退dataCenterId应在0~31");
    }

    @Test
    void init_slotsExhausted_noDirtyId_fallsBackToRandom() throws Exception {
        when(redisHelp.lock(anyString(), anyString(), anyLong())).thenReturn(false);

        MachineIdHelp help = new MachineIdHelp(redisHelp, APP_ID);

        // 修复验证点1：1024 槽全部尝试后字段不残留脏值（旧实现停在(31,31)）
        verify(redisHelp, times(32 * 32)).lock(anyString(), anyString(), anyLong());
        assertNull(getStaticId("machineId"), "槽耗尽后machineId应保持null");
        assertNull(getStaticId("dataCenterId"), "槽耗尽后dataCenterId应保持null");

        // 修复验证点2：getGenerator 走随机回退
        assertNotNull(help.getGenerator(), "随机回退应产出可用generator");
    }

    private static Integer getStaticId(String fieldName) throws Exception {
        Field field = MachineIdHelp.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return (Integer) field.get(null);
    }

    private static void setStaticId(String fieldName, Integer value) throws Exception {
        Field field = MachineIdHelp.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(null, value);
    }
}
