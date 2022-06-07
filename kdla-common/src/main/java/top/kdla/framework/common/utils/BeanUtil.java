package top.kdla.framework.common.utils;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * bean对象转换成Map
 *
 * @author kll
 * @date 2022/1/21 19:41
 */
public class BeanUtil {
    public static Map<String, Object> objectToMap(Object obj) throws Exception {
        if (obj == null) {
            return null;
        } else {
            Map<String, Object> map = new HashMap();
            BeanInfo beanInfo = Introspector.getBeanInfo(obj.getClass());
            PropertyDescriptor[] propertyDescriptors = beanInfo.getPropertyDescriptors();
            PropertyDescriptor[] propertyAry = propertyDescriptors;
            int length = propertyDescriptors.length;

            for (int i = 0; i < length; ++i) {
                PropertyDescriptor property = propertyAry[i];
                String key = property.getName();
                if (key.compareToIgnoreCase("class") != 0) {
                    Method getter = property.getReadMethod();
                    Object value = getter != null ? getter.invoke(obj) : null;
                    if (value != null) {
                        map.put(key, value);
                    }
                }
            }

            return map;
        }
    }
}
