package com.xxl.job.core.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

/**
 * @author xuxueli 2020-04-12 0:14:00
 */
public class JdkSerializeTool {
    private static Logger logger = LoggerFactory.getLogger(JdkSerializeTool.class);


    // ------------------------ serialize and unserialize ------------------------

    /**
     * 将对象-->byte[] (由于jedis中不支持直接存储object所以转换成byte[]存入)
     *
     * @param object
     * @return
     */
    public static byte[] serialize(Object object) {
        ObjectOutputStream oos = null;
        ByteArrayOutputStream baos = null;
        try {
            // 序列化
            baos = new ByteArrayOutputStream();
            oos = new ObjectOutputStream(baos);
            oos.writeObject(object);
            byte[] bytes = baos.toByteArray();
            return bytes;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        } finally {
            try {
                oos.close();
                baos.close();
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
            }
        }
        return null;
    }


    /**
     * 将byte[] -->Object，默认按 clazz 与内嵌 xxl-job 模型构造 JEP 290 白名单过滤。
     * 被拒绝的类不会实例化，返回 null（fail-closed）。下游自有类型请改用带 filter 的重载。
     */
    public static  <T> Object deserialize(byte[] bytes, Class<T> clazz) {
        return deserialize(bytes, clazz, defaultFilter(clazz));
    }

    /**
     * 带 ObjectInputFilter 的反序列化（JEP 290）；filter 为 null 时不过滤，仅限可信字节。
     */
    public static <T> Object deserialize(byte[] bytes, Class<T> clazz, ObjectInputFilter filter) {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
             ObjectInputStream ois = new ObjectInputStream(bais)) {
            if (filter != null) {
                ois.setObjectInputFilter(filter);
            }
            return ois.readObject();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    // ponytail: 白名单 = java.base + 调用方声明类型 + com.xxl.job.**；升级路径为下游显式传入更窄的 filter。
    private static ObjectInputFilter defaultFilter(Class<?> clazz) {
        String expected = (clazz == null || clazz.isArray()) ? "" : clazz.getName() + ";";
        return ObjectInputFilter.Config.createFilter(
                "maxdepth=20;maxarray=100000;maxbytes=10485760;maxrefs=100000;java.base/*;"
                        + expected + "com.xxl.job.**;!*");
    }


}
