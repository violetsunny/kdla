package top.kdla.framework.common.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

/**
 * Jackson工具类
 * @author kll
 * @since 2021/7/20
 */
@Slf4j
public class JacksonUtil {

    private JacksonUtil() {

    }

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final ObjectMapper OBJECT_MAPPER_LOWER_CASE_WITH_UNDER_SCORES = new ObjectMapper();

    static {
        //有时JSON字符串中含有我们并不需要的字段，那么当对应的实体类中不含有该字段时，会抛出一个异常，告诉你有些字段（java 原始类型）没有在实体类中找到
        //设置为false即不抛出异常，并设置默认值 int->0 double->0.0
        OBJECT_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        OBJECT_MAPPER_LOWER_CASE_WITH_UNDER_SCORES.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        //序列化时添加下划线
        OBJECT_MAPPER_LOWER_CASE_WITH_UNDER_SCORES.setPropertyNamingStrategy(new PropertyNamingStrategies.SnakeCaseStrategy());
    }

    /**
     * bean对象转Json
     * @param object bean对象
     * @return String
     */
    public static String toJson(Object object) {
        if (object != null) {
            try {
                return OBJECT_MAPPER.writeValueAsString(object);
            } catch (JsonProcessingException e) {
                log.warn("bean to json exception", e);
            }
        }
        return "";
    }

    /**
     * Json对象转bean
     * @param json json字符串
     * @param classType bean对象的class
     * @return 对象
     */
    public static <T> T toBean(String json, Class<T> classType) {
        if (StringUtils.isNotBlank(json) && classType != null) {
            try {
                return OBJECT_MAPPER.readValue(json, classType);
            } catch (IOException e) {
                log.warn("json to bean exception", e);
            }
        }
        return null;
    }

    public static <T> T toBean(String json, Type type) {
        try {
            return OBJECT_MAPPER.readValue(json, OBJECT_MAPPER.constructType(type));
        } catch (IOException ioe) {
           String errMsg =  String.format("deserialize json for class [%s] failed. ", type.toString());
            log.warn("json to bean exception", ioe);
            throw new RuntimeException(errMsg);
        }
    }

    public static <T> T toBean(String json, TypeReference<T> typeReference) {
        try {
            return OBJECT_MAPPER.readValue(json, typeReference);
        } catch (IOException ioe) {
            String errMsg =  String.format("deserialize json for class [%s] failed. ", typeReference.getClass().getName());
            log.warn("json to bean exception", ioe);
            throw new RuntimeException(errMsg);
        }
    }

    /**
     * Json对象转List
     * @param json json字符串
     * @param classType List对象的class
     * @return List
     */
    public static <T> List<T> toList(String json, Class<T> classType) {
        if (StringUtils.isNotBlank(json) && classType != null) {
            try {
                return OBJECT_MAPPER.readValue(json, OBJECT_MAPPER.getTypeFactory().constructCollectionType(List.class, classType));
            } catch (IOException e) {
                log.error("json to list exception", e);
            }
        }
        return null;
    }

    /**
     * Json对象转Map
     * @param json json字符串
     * @param kType Key的class
     * @param vType value的class
     * @return Map
     */
    public static <k, v> Map<k, v> toMap(String json, Class<k> kType, Class<v> vType) {
        if (StringUtils.isNotBlank(json)) {
            try {
                return OBJECT_MAPPER.readValue(json, OBJECT_MAPPER.getTypeFactory().constructMapType(Map.class, kType, vType));
            } catch (IOException e) {
                log.error("json to Map exception", e);
            }
        }
        return null;
    }

    /**
     * bean对象转Json,带下划线
     * @param object bean对象
     * @return String
     */
    public static String toJsonByLowerCase(Object object) {
        if (object != null) {
            try {
                return OBJECT_MAPPER_LOWER_CASE_WITH_UNDER_SCORES.writeValueAsString(object);
            } catch (JsonProcessingException e) {
                log.error("bean to json with lowerCase exception", e);
            }
        }
        return "";
    }

    /**
     * Json转bean对象,带下划线
     * @param json Json字符串，带下划线
     * @param classType 对象class
     * @return T对象
     */
    public static <T> T toBeanByLowerCase(String json, Class<T> classType) {
        if (StringUtils.isNotBlank(json)) {
            try {
                return OBJECT_MAPPER_LOWER_CASE_WITH_UNDER_SCORES.readValue(json, classType);
            } catch (IOException e) {
                log.error("json to Bean with lowerCase exception", e);
            }
        }
        return null;
    }
}
