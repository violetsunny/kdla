package top.kdla.framework.supplement.excel.imp;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 清单 M8 回归测试：错误报告临时文件未注册 deleteOnExit。
 *
 * <p>旧实现 generateErrorReportTempFile 只 createTempFile，JVM 退出后文件残留，
 * 导入报告持续累积占满磁盘。修复后调用 tempFile.deleteOnExit()。
 *
 * <p>deleteOnExit 只在 JVM 退出时生效，进程内无法断言（JDK21 强封装禁止反射
 * DeleteOnExitHook，且禁改 pom 加 --add-opens），故用子进程探针：
 * 子进程生成临时文件并打印路径后正常退出，父进程断言文件已被清理。
 *
 * <p>无修复则失败：旧代码子进程退出后临时文件仍存在。
 */
class ExcelReadListenerTempFileCleanupTest {

    public static class TempFileProbe {
        public static void main(String[] args) throws Exception {
            ExcelReadListener<Object, Object> listener = new ExcelReadListener<>();
            Method m = ExcelReadListener.class.getDeclaredMethod("generateErrorReportTempFile");
            m.setAccessible(true);
            File tempFile = (File) m.invoke(listener);
            System.out.println("TEMP_FILE=" + tempFile.getAbsolutePath());
        }
    }

    @Test
    void tempReportFile_deletedOnJvmExit() throws Exception {
        String classpath = System.getProperty("surefire.test.class.path");
        if (classpath == null || classpath.isEmpty()) {
            classpath = System.getProperty("java.class.path");
        }
        Process process = new ProcessBuilder(
                System.getProperty("java.home") + File.separator + "bin" + File.separator + "java",
                "-cp", classpath, TempFileProbe.class.getName())
                .redirectErrorStream(true)
                .start();
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertTrue(process.waitFor(60, TimeUnit.SECONDS), "子进程60秒内未退出");
        assertEquals(0, process.exitValue(), "子进程异常退出：" + output);
        String path = output.lines()
                .filter(line -> line.startsWith("TEMP_FILE="))
                .map(line -> line.substring("TEMP_FILE=".length()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("子进程未输出临时文件路径：" + output));
        assertFalse(Files.exists(Path.of(path)), "JVM退出后临时报告文件应被deleteOnExit清理：" + path);
    }
}