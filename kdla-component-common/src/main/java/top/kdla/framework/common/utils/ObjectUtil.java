package top.kdla.framework.common.utils;

import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

@Slf4j
public class ObjectUtil {

    public static byte[] ObjectToByte(Object obj) {
        byte[] bytes = null;
        // try-with-resources 确保流关闭；toByteArray 前必须 flush，否则 ObjectOutputStream 块缓冲区尾部字节可能丢失
        try (ByteArrayOutputStream bo = new ByteArrayOutputStream();
             ObjectOutputStream oo = new ObjectOutputStream(bo)) {
            // object to bytearray
            oo.writeObject(obj);
            oo.flush();

            bytes = bo.toByteArray();
        } catch (Exception e) {
            log.error("ObjectUtil ObjectToByte Exception", e);
        }
        return bytes;
    }


    /**
     * 反序列化（无过滤，仅限可信字节）。
     *
     * @deprecated 无 ObjectInputFilter，不可信输入可触发反序列化 RCE（CWE-502）。
     * 请改用 {@link #byteToObject(byte[], ObjectInputFilter)} 并配置类白名单，或改用 JacksonUtil（JSON）。
     */
    @Deprecated
    public static Object byteToObject(byte[] bytes) {
        return byteToObject(bytes, null);
    }

    /**
     * 带 ObjectInputFilter 的反序列化（JEP 290）。
     * filter 为 null 时不做过滤（等同废弃方法，仅限可信字节）；不可信输入必须传入拒绝未知类的白名单 filter。
     * 被 filter 拒绝的类不会实例化，返回 null（fail-closed）。
     */
    public static Object byteToObject(byte[] bytes, ObjectInputFilter filter) {
        Object obj = null;
        try (ByteArrayInputStream bi = new ByteArrayInputStream(bytes);
             ObjectInputStream oi = new ObjectInputStream(bi)) {
            if (filter != null) {
                oi.setObjectInputFilter(filter);
            }
            obj = oi.readObject();
        } catch (Exception e) {
            log.error("ObjectUtil byteToObject Exception", e);
        }
        return obj;
    }
}
