package top.kdla.framework.common.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
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
            gunZip.write(str.getBytes());
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

    /**
     * 对参数先进行转码,再解压
     * 
     * @param str
     * @return
     * @throws IOException
     */
    public static String unZip(String str) throws IOException {
        if (str == null || str.trim().length() == 0) {
            return "";
        }
        byte[] t = Base64.getDecoder().decode(str);// 转码
        ByteArrayInputStream bis = new ByteArrayInputStream(t);
        GZIPInputStream gzip = new GZIPInputStream(bis);// 解压
        byte[] buf = new byte[2048];
        int num = -1;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        while ((num = gzip.read(buf, 0, buf.length)) != -1) {
            bos.write(buf, 0, num);
        }
        gzip.close();
        bis.close();
        bos.flush();
        bos.close();
        return bos.toString();
    }

}
