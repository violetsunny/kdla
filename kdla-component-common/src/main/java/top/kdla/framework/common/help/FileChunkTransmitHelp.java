/**
 * llkang.com Inc.
 * Copyright (c) 2010-2023 All Rights Reserved.
 */
package top.kdla.framework.common.help;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

/**
 * 文件分块传输
 * 建议采用 MappedByteBuffer+RandomAccessFile的方式。
 *
 * @author kanglele
 * @version $Id: FileChunkTransmitHelp, v 0.1 2023/7/18 17:11 kanglele Exp $
 */
@Slf4j
public class FileChunkTransmitHelp {

    private final File src;//输入源
    private final String destDir;//分割子文件输出的目录
    @Getter
    private final List<String> destPaths;//保存每个分割子文件的路径
    private final int blockSize;//切割大小
    private int size;//切割总块数

    public FileChunkTransmitHelp(String srcDir, String destDir) {
        this(srcDir, destDir, 1024 * 1024);
    }

    public FileChunkTransmitHelp(String srcDir, String destDir, int blockSize) {
        this.src = new File(srcDir);//初始化输入源
        this.destDir = destDir;//初始化分割子文件输出的目录
        this.destPaths = new ArrayList<>();//初始化保存分割子文件的路径容器
        this.blockSize = blockSize;//初始化切割大小

        //初始化对象参数
        init();
        log.info("init FileChunkTransmitHelp successfully");
    }

    /**
     * 初始化SplitFileUtils 参数
     */
    private void init() {
        if (!this.src.exists()) {
            log.warn("FileChunkTransmitHelp init 输入源 不存在{} ", this.src.getName());
            return;
        }

        long len = this.src.length();//文件总长度

        this.size = (int) Math.ceil(len * 1.0 / this.blockSize);// 四舍五入计算分割总总次数

        //根据size循环保存 切分子文件路径
        for (int i = 0; i < this.size; i++) {
            this.destPaths.add(this.destDir + File.separator + i + "-" + this.src.getName());
        }

        // 如果保存切分子文件目录不存在
        if (len > 0) {
            File destFile = new File(this.destDir);
            if (!destFile.exists()) {
                destFile.mkdirs();
            }
        }
    }

    public void split() {
        if (!this.src.exists()) {
            log.warn("FileChunkTransmitHelp split 输入源 不存在{} ", this.src.getName());
            return;
        }
        //总长度
        long len = this.src.length();
        //起始位置和实际大小
        int beginPos = 0;
        int actualSize = (int) (blockSize > len ? len : blockSize);

        for (int i = 0; i < this.size; i++) {
            beginPos = i * this.blockSize;
            if (i == this.size - 1) { //最后一块
                actualSize = (int) len;
            } else {
                actualSize = this.blockSize;
                len -= actualSize; //剩余量
            }

            splitDetail(i, beginPos, actualSize);
        }
        log.info("子文件切分后保存至{}", this.destDir);
    }

    /**
     * 根据 循环次数,偏移量,实际读取量, 使用随机流输入模式读取文件字节,并使用随机流读写模式写入读取字节
     *
     * @param i
     * @param beginPos
     * @param actualSize
     */
    private void splitDetail(int i, int beginPos, int actualSize) {
        try (
                RandomAccessFile readRaf = new RandomAccessFile(this.src, "r");
                RandomAccessFile writeRaf = new RandomAccessFile(this.destPaths.get(i), "rw");
        ) {
            // 设置随机读取流的偏移量
            readRaf.seek(beginPos);
            // 设置缓存容器
            byte[] flush = new byte[actualSize];

            int len = -1; //接收长度
            while ((len = readRaf.read(flush)) != -1) {
                if (actualSize > len) { //获取本次读取的所有内容
                    writeRaf.write(flush, 0, len);
                    actualSize -= len;
                } else {
                    writeRaf.write(flush, 0, actualSize);
                    break;
                }
            }

        } catch (IOException e) {
            log.error("FileChunkTransmitHelp splitDetail IOException", e);
        }
    }

    //splitDetail2 > splitDetail
    private void splitDetail2(int i, int beginPos, int actualSize) {
        try (
                RandomAccessFile readRaf = new RandomAccessFile(this.src, "r");
                RandomAccessFile writeRaf = new RandomAccessFile(this.destPaths.get(i), "rw");
                FileChannel readChannel = readRaf.getChannel();
                FileChannel writeChannel = writeRaf.getChannel()
        ) {
            // map the buffer from readChannel to writeChannel
            MappedByteBuffer readBuffer = readChannel.map(FileChannel.MapMode.READ_ONLY, beginPos, actualSize);
            MappedByteBuffer writeBuffer = writeChannel.map(FileChannel.MapMode.READ_WRITE, 0, actualSize);

            // copy the content from readBuffer to writeBuffer
            writeBuffer.put(readBuffer);
        } catch (IOException e) {
            log.error("FileChunkTransmitHelp splitDetail IOException", e);
        }
    }

    /**
     * srcPaths = destPaths
     * 调用 merge(String destPath) 将 srcPaths 每一个分割文件,声明一个缓冲输入流,然后保存到 Vector<InputStream>中,
     * 然后使用SequenceInputStream将Vector<InputStream>合并到一个输入流中,
     * 最后使用destPath,创建一个缓冲输出流,将合并输入流读取字节,全部写入输出流中
     *
     * @param destPath
     */
    public static void merge(List<String> srcPaths, String destPath) {
        //声明向量集合保存 InputStream
        Vector<InputStream> vis = new Vector<>();
        SequenceInputStream sis = null;
        BufferedOutputStream bos = null;
        try {
            // 将每一个切分后的子文件使用输入流 保存到 向量集合中
            for (String path : srcPaths) {
                vis.add(new BufferedInputStream(new FileInputStream(path)));
            }

            // 将向量集合中的输入流合并成一个 合并流 SequenceInputStream
            sis = new SequenceInputStream(vis.elements());
            // 声明缓冲输出流
            bos = new BufferedOutputStream(new FileOutputStream(destPath, true));

            //拷贝
            //3、操作 (分段读取)
            byte[] flush = new byte[1024 * 1024]; //缓冲容器1M
            int len = -1; //接收长度
            while ((len = sis.read(flush)) != -1) {
                bos.write(flush, 0, len); //分段写出
            }
            bos.flush();

            //是否需要删除原路径文件
            //delFileBySrcPath(srcPaths);
        } catch (IOException e) {
            log.error("FileChunkTransmitHelp merge IOException", e);
        } finally {
            try {
                if (bos != null) {
                    bos.close();
                }
                if (sis != null) {
                    sis.close();
                }
            } catch (IOException e) {
                log.error("FileChunkTransmitHelp merge IOException", e);
            }
        }
    }

    //merge2 > merge
    public static void merge2(List<String> srcPaths, String destPath) {
        try (
                RandomAccessFile writeRaf = new RandomAccessFile(destPath, "rw");
                FileChannel writeChannel = writeRaf.getChannel()
        ) {
            // 将每一个切分后的子文件使用输入流
            long beginPos = 0;
            for (String path : srcPaths) {
                FileChannel readChannel = new RandomAccessFile(path, "r").getChannel();
                long actualSize = readChannel.size();
                MappedByteBuffer readBuffer = readChannel.map(FileChannel.MapMode.READ_ONLY, 0, actualSize);
                MappedByteBuffer writeBuffer = writeChannel.map(FileChannel.MapMode.READ_WRITE, beginPos, actualSize);
                writeBuffer.put(readBuffer);
                //新的起点
                beginPos += actualSize;
            }
        } catch (IOException e) {
            log.error("FileChunkTransmitHelp merge IOException", e);
        }
    }

    /**
     * 删除文件
     */
    public static void delFileBySrcPath(List<String> srcPaths) {
        for (String path : srcPaths) {
            File file = new File(path);
            if (!file.exists()) {
                continue;
            }
            if (file.isFile()) {
                file.deleteOnExit();
            }
        }
    }

}
