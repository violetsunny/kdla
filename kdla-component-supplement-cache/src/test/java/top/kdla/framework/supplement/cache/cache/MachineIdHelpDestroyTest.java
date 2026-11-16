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
 * 清单#附加3 回归测试：MachineIdHelp.destroyMachineId 用 del 立即删除注册记录。
 *
 * <p>旧实现 set(key, 空串, 1秒)：1秒窗口内 key 仍存在(空值)，会阻塞其他实例 SETNX 注册，
 * 销毁后该 machineId 在1秒内不可被复用。
 *
 * <p>修复后：del(key) 立即删除，消除窗口。
 *
 * <p>无修复则失败：旧代码调用 set 而非 del，下方 verify(del) 与 never(set) 断言不通过。
 */
@ExtendWith(MockitoExtension.class)
class MachineIdHelpDestroyTest {

    private static final String APP_ID = "mach-id-destroy-app";

    private static final long REGISTER_EXPIRE_MILLIS = 3600L * 1000;

    @Mock
    private SpringRedisHelp redisHelp;

    @Test
    void destroyMachineId_usesDel_notSetWithTtl() {
        // 第一个ID(0&0)注册成功
        when(redisHelp.lock(eq(APP_ID + "::snowflake::0&0"), anyString(), eq(REGISTER_EXPIRE_MILLIS)))
                .thenReturn(true);

        MachineIdHelp help = new MachineIdHelp(redisHelp, APP_ID);
        help.destroyMachineId();

        // 修复验证点1：销毁走 del 立即删除
        verify(redisHelp, times(1)).del(APP_ID + "::snowflake::0&0");
        // 修复验证点2：不再使用旧实现 set(空串,1秒) 的留窗口写法
        verify(redisHelp, never()).set(anyString(), any(), anyLong());
        verify(redisHelp, never()).set(anyString(), any());
    }
}
