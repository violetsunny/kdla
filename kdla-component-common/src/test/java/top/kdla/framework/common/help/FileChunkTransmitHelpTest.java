package top.kdla.framework.common.help;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * 回归测试：
 * 1. merge 旧实现 new FileOutputStream(destPath, true) 为 append 模式，目标文件已存在时旧内容污染合并结果；修复为覆盖写。
 * 2. merge/splitDetail 旧实现 IOException 仅 log 不上抛，调用方误以为成功（静默失败）；修复为包装 RuntimeException 上抛。
 * 3. merge 循环打开的 FileInputStream 在异常路径下未关闭（句柄泄漏）；修复后 finally 中确保全部关闭。
 */
class FileChunkTransmitHelpTest {

    @TempDir
    Path tempDir;

    @Test
    void splitThenMerge_roundTripContentIdentical() throws Exception {
        File src = tempDir.resolve("src.bin").toFile();
        byte[] data = new byte[2500];
        new Random(42).nextBytes(data);
        Files.write(src.toPath(), data);

        FileChunkTransmitHelp help = new FileChunkTransmitHelp(src.getAbsolutePath(),
                tempDir.resolve("chunks").toString(), 1000);
        help.split();
        assertEquals(3, help.getDestPaths().size());

        File dest = tempDir.resolve("dest.bin").toFile();
        FileChunkTransmitHelp.merge(help.getDestPaths(), dest.getAbsolutePath());

        assertArrayEquals(data, Files.readAllBytes(dest.toPath()), "切分再合并后内容必须与源文件一致");
    }

    @Test
    void merge_overwritesExistingDestFile_notAppend() throws Exception {
        File chunk = tempDir.resolve("chunk").toFile();
        Files.write(chunk.toPath(), "AAA".getBytes("UTF-8"));
        File dest = tempDir.resolve("dest.txt").toFile();
        Files.write(dest.toPath(), "OLD-CONTENT-SHOULD-BE-OVERWRITTEN".getBytes("UTF-8"));

        FileChunkTransmitHelp.merge(Collections.singletonList(chunk.getAbsolutePath()), dest.getAbsolutePath());

        assertEquals("AAA", new String(Files.readAllBytes(dest.toPath()), "UTF-8"),
                "merge 必须覆盖写目标文件，不能 append 到旧内容后面");
    }

    @Test
    void merge_missingSrcFile_throwsRuntimeException() {
        File dest = tempDir.resolve("dest2.txt").toFile();

        RuntimeException ex = assertThrows(RuntimeException.class, () -> FileChunkTransmitHelp.merge(
                Collections.singletonList(tempDir.resolve("not-exists").toString()), dest.getAbsolutePath()));
        assertEquals("FileChunkTransmitHelp merge IOException", ex.getMessage(),
                "合并失败必须显性上抛，禁止只记日志静默吞掉");
    }

    @Test
    void split_chunkFileNotWritable_throwsRuntimeException() throws Exception {
        File src = tempDir.resolve("src2.bin").toFile();
        Files.write(src.toPath(), new byte[2048]);
        String chunkDir = tempDir.resolve("chunks2").toString();
        FileChunkTransmitHelp help = new FileChunkTransmitHelp(src.getAbsolutePath(), chunkDir, 1024);
        // 先正常切分生成子文件
        help.split();
        // 将全部子文件置为只读，令二次切分的 RandomAccessFile("rw") 打开失败
        boolean locked = true;
        for (String path : help.getDestPaths()) {
            locked = locked && new File(path).setWritable(false);
        }
        assumeTrue(locked, "当前环境无法将文件置为只读，跳过");

        try {
            RuntimeException ex = assertThrows(RuntimeException.class, help::split);
            assertEquals("FileChunkTransmitHelp splitDetail IOException", ex.getMessage(),
                    "切分失败必须显性上抛，禁止只记日志静默吞掉");
        } finally {
            for (String path : help.getDestPaths()) {
                new File(path).setWritable(true);
            }
        }
    }
}
