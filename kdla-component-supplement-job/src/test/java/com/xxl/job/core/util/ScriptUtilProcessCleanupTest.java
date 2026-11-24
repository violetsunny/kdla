package com.xxl.job.core.util;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 回归测试：脚本任务超时/中断后子进程未被销毁，成为孤儿继续占用执行机。
 *
 * <p>旧实现：JobThread 超时仅 futureThread.interrupt()，ScriptUtil 的 process.waitFor() 抛
 * InterruptedException 后直接 return -1，从不 destroy 子进程；executorTimeout=0 时更是无限等待。
 *
 * <p>修复后：waitFor/join 外包 try-finally，退出前 process.destroyForcibly()，正常结束时无副作用。
 *
 * <p>无修复则失败：旧代码中断后 sleep 子进程仍存活，ProcessHandle.isAlive() 为 true。
 */
class ScriptUtilProcessCleanupTest {

    @Test
    void interruptedExec_killsChildProcess() throws Exception {
        Path dir = Files.createTempDirectory("kdla-script-cleanup");
        Path pidFile = dir.resolve("child.pid");
        Path script = dir.resolve("hang.sh");
        // exec 让 sleep 顶替 sh，pid 与 $$ 一致，便于直接断言被 spawn 的进程已死
        Files.writeString(script, "echo $$ > " + pidFile + "\nexec sleep 30\n");
        Path logFile = dir.resolve("run.log");

        AtomicInteger exit = new AtomicInteger(Integer.MIN_VALUE);
        Thread worker = new Thread(() -> {
            try {
                exit.set(ScriptUtil.execToFile("sh", script.toString(), logFile.toString()));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
        worker.start();
        long pid = awaitPid(pidFile);
        try {
            worker.interrupt();
            worker.join(5000);
            assertFalse(worker.isAlive(), "中断后 execToFile 须及时返回");
            assertEquals(-1, exit.get(), "中断须返回 -1");
            assertTrue(deadWithin(pid, Duration.ofSeconds(5)), "中断后子进程须被销毁，不得成为孤儿");
        } finally {
            ProcessHandle.of(pid).ifPresent(ProcessHandle::destroyForcibly);
        }
    }

    @Test
    void normalScript_stillReturnsExitCodeAndLog() throws Exception {
        Path dir = Files.createTempDirectory("kdla-script-normal");
        Path script = dir.resolve("ok.sh");
        Files.writeString(script, "echo hello\n");
        Path logFile = dir.resolve("run.log");

        assertEquals(0, ScriptUtil.execToFile("sh", script.toString(), logFile.toString()));
        assertTrue(Files.readString(logFile).contains("hello"), "正常脚本日志须完整落盘");
    }

    private static long awaitPid(Path pidFile) throws Exception {
        long deadline = System.nanoTime() + Duration.ofSeconds(10).toNanos();
        while (System.nanoTime() < deadline) {
            if (Files.exists(pidFile)) {
                String text = Files.readString(pidFile).trim();
                if (!text.isEmpty()) {
                    return Long.parseLong(text);
                }
            }
            Thread.sleep(20);
        }
        throw new AssertionError("子进程未在10s内写出pid");
    }

    private static boolean deadWithin(long pid, Duration timeout) throws InterruptedException {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadline) {
            Optional<ProcessHandle> handle = ProcessHandle.of(pid);
            if (handle.isEmpty() || !handle.get().isAlive()) {
                return true;
            }
            Thread.sleep(50);
        }
        return false;
    }
}
