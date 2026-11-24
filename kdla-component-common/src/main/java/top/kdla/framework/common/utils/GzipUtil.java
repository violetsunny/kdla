package top.kdla.framework.common.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * 数据压缩/解压工具类
 * 
 * @author kll
 * @version $Id: GzipUtils.java $
 */
public class GzipUtil {

    /**
     * 对参数进行压缩，再转码
     * 
     * @param str
     * @return
     * @throws IOException
     */
    public static String compression(String str) throws IOException {
        if (str == null || str.trim().length() == 0) {
            return "";
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        GZIPOutputStream gunZip = new GZIPOutputStream(out);
        try {
            gunZip.write(str.getBytes(StandardCharsets.UTF_8));
        } finally {
            gunZip.close();
            out.close();
        }
        /*BASE64Decoder tBase64Decoder = new BASE64Decoder();
        byte[] t = tBase64Decoder.decodeBuffer(out.toString("UTF-8"));*/

        // 编码
        // 要用toByteArray() 这是byte流，和 String.getBytes()有区别
        return Base64.getEncoder().encodeToString(out.toByteArray());
    }

    /** 默认解压上限 64MB，防 zip bomb */
    public static final long DEFAULT_MAX_BYTES = 64L * 1024 * 1024;

    /**
     * 对参数先进行转码,再解压（默认上限 64MB）
     *
     * @param str
     * @return
     * @throws IOException
     */
    public static String unZip(String str) throws IOException {
        return unZip(str, DEFAULT_MAX_BYTES);
    }

    /**
     * 对参数先进行转码,再解压，解压结果超过 maxBytes 抛 IOException
     *
     * @param str
     * @param maxBytes 解压后字节数上限
     * @return
     * @throws IOException
     */
    public static String unZip(String str, long maxBytes) throws IOException {
        if (str == null || str.trim().length() == 0) {
            return "";
        }
        byte[] t = Base64.getDecoder().decode(str);// 转码
        try (ByteArrayInputStream bis = new ByteArrayInputStream(t);
             GZIPInputStream gzip = new GZIPInputStream(bis);
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            byte[] buf = new byte[2048];
            int num;
            long total = 0;
            while ((num = gzip.read(buf, 0, buf.length)) != -1) {
                total += num;
                if (total > maxBytes) {
                    throw new IOException("unZip exceeds maxBytes: " + maxBytes);
                }
                bos.write(buf, 0, num);
            }
            return bos.toString(StandardCharsets.UTF_8);
        }
    }

}
