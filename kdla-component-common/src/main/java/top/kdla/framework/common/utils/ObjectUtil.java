package top.kdla.framework.common.utils;

import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
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


    public static Object byteToObject(byte[] bytes) {
        Object obj = null;
        try {
            // bytearray to object
            ByteArrayInputStream bi = new ByteArrayInputStream(bytes);
            ObjectInputStream oi = new ObjectInputStream(bi);

            obj = oi.readObject();

            bi.close();
            oi.close();
        } catch (Exception e) {
            log.error("ObjectUtil byteToObject Exception", e);
        }
        return obj;
    }
}
