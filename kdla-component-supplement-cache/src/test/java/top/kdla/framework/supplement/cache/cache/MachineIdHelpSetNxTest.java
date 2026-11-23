package top.kdla.framework.supplement.cache.cache;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 清单#12 回归测试：MachineIdHelp.init 机器ID注册改为 SETNX 原子操作。
 *
 * <p>旧实现先 get(key) 判空、再 set(key, ip, 3600)，是 check-then-act 竞态：
 * 并发启动的多实例可能同时判空成功，抢到同一个 machineId/dataCenterId，
 * 导致雪花ID重复。
 *
 * <p>修复后：通过 redisHelp.lock(key, ip, REGISTER_EXPIRE_MILLIS)（SETNX）
 * 原子注册，失败则换下一个ID重试。
 *
 * <p>无修复则失败：旧代码调用 get/set 而非 lock，下方 never() 与 times(2) 断言不通过。
 */
@ExtendWith(MockitoExtension.class)
class MachineIdHelpSetNxTest {

    private static final String APP_ID = "mach-id-test-app";

    private static final long REGISTER_EXPIRE_MILLIS = 3600L * 1000;

    @Mock
    private SpringRedisHelp redisHelp;

    @Test
    void init_registersViaSetNx_notGetThenSet() {
        // 第一个ID(0&0)已被占用（SETNX失败），第二个ID(1&0)注册成功
        when(redisHelp.lock(eq(APP_ID + "::snowflake::0&0"), anyString(), eq(REGISTER_EXPIRE_MILLIS)))
                .thenReturn(false);
        when(redisHelp.lock(eq(APP_ID + "::snowflake::1&0"), anyString(), eq(REGISTER_EXPIRE_MILLIS)))
                .thenReturn(true);

        // 构造函数内即执行 init() 注册
        new MachineIdHelp(redisHelp, APP_ID);

        // 修复验证点1：注册走 SETNX 原子操作 lock(key,value,expire)，恰好尝试2次后成功返回
        verify(redisHelp, times(2)).lock(anyString(), anyString(), anyLong());
        // 修复验证点2：不再使用旧实现 get判空后set 的 check-then-act 竞态写法
        verify(redisHelp, never()).get(anyString());
        verify(redisHelp, never()).set(anyString(), any());
        verify(redisHelp, never()).set(anyString(), any(), anyLong());
    }
}
