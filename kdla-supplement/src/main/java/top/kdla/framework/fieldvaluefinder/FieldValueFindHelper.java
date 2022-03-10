package top.kdla.framework.fieldvaluefinder;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import top.kdla.framework.common.utils.JacksonUtil;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 字段转换帮助类
 *
 * @author hjs
 * @date 2021/12/7
 */
@Slf4j
@Component
public class FieldValueFindHelper {


    /**
     * 对列表中的对象进行遍历处理。
     * 如果对象的字段上挂载了注解 @FieldValueFind，那么就会做对该字段做查询、注入新值的逻辑
     * @param list
     */
    public void process(List<?> list) {
        if (list == null || list.size() == 0) {
            return;
        }
        List<ProcessFieldDto> dtoList = new ArrayList<>();
        for (Object sourceObject : list) {
            Field[] fields = sourceObject.getClass().getDeclaredFields();
            for (Field field : fields) {
                Annotation[] declaredAnnotations = field.getDeclaredAnnotations();
                for (Annotation annotation : declaredAnnotations) {
                    if (annotation instanceof FieldValueFind) {
                        ProcessFieldDto dto = ProcessFieldDto.builder()
                                .sourceObject(sourceObject).field(field)
                                .fieldValueFind((FieldValueFind) annotation)
                                .build();
                        dtoList.add(dto);
                    }
                }
            }
        }
        processDtoList(dtoList);
    }
    private void processDtoList(List<ProcessFieldDto> list) {
        //直接查询设置
        List<ProcessFieldDto> directQueryList = list.stream().filter(e -> e.getFieldValueFind().queryPolicy().equals(QueryPolicy.DIRECT))
                .collect(Collectors.toList());
        for (ProcessFieldDto dto : directQueryList) {
            directQuery(dto);
        }
        //批量查询设置
        List<ProcessFieldDto> listBatchQueryList = list.stream().filter(e -> e.getFieldValueFind().queryPolicy().equals(QueryPolicy.BATCH))
                .collect(Collectors.toList());
        Map<String, List<ProcessFieldDto>> listMap = listBatchQueryList.stream().collect(Collectors.groupingBy(e -> {
            Class queryClass = e.getFieldValueFind().queryClass();
            String queryMethod = e.getFieldValueFind().queryMethod();
            String key = queryClass.getName() + "#" + queryMethod;
            return key;
        }));
        for (List<ProcessFieldDto> dtoList : listMap.values()) {
            batchQuery(dtoList);
        }

    }



    private void directQuery(ProcessFieldDto dto) {
        FieldValueFind fieldValueFind = dto.getFieldValueFind();
        Field field = dto.getField();
        Object sourceObject = dto.getSourceObject();
        String fromFieldName = fieldValueFind.fromField();
        Class queryClass = fieldValueFind.queryClass();
        String queryMethodName = fieldValueFind.queryMethod();
        ExistPolicy existPolicy = fieldValueFind.existPolicy();
        try {
            field.setAccessible(true);
            if (field.get(sourceObject) != null) {
                if (ExistPolicy.KEEP_SELF.equals(existPolicy)) {
                    return;
                }
                if (!ExistPolicy.USE_FROM.equals(existPolicy)) {
                    throw new IllegalArgumentException();
                }
            }
            Field fromField = sourceObject.getClass().getDeclaredField(fromFieldName);
            fromField.setAccessible(true);
            Object fromFieldValue = fromField.get(sourceObject);

            Method queryMethod = queryClass.getDeclaredMethod(queryMethodName, fromField.getType());
            Object queryBean = FieldValueFindApplicationContextUtils.getBean(queryClass);
            Object result = queryMethod.invoke(queryBean, fromFieldValue);
            fromField.setAccessible(false);
            field.set(sourceObject, result);
            field.setAccessible(false);
        } catch (RuntimeException e) {
            log.error("findAndInjectValue failed,dto is:" + JacksonUtil.toJson(dto) + ",exception is:", e);
            throw e;
        } catch (Exception e) {
            log.error("findAndInjectValue failed,dto is:" + JacksonUtil.toJson(dto) + ",exception is:", e);
            throw new RuntimeException("FieldValueFindHelper process failed");
        }
    }

    private void batchQuery(List<ProcessFieldDto> dtoList) {
        try {
            Class queryClass = dtoList.get(0).getFieldValueFind().queryClass();
            String queryMethodName = dtoList.get(0).getFieldValueFind().queryMethod();
            Method queryMethod = queryClass.getDeclaredMethod(queryMethodName, List.class);
            Object queryBean = FieldValueFindApplicationContextUtils.getBean(queryClass);
            //打开权限
            for (ProcessFieldDto dto : dtoList) {
                dto.getField().setAccessible(true);
            }
            //校验、过滤
            List<ProcessFieldDto> list = new ArrayList<>();
            for (ProcessFieldDto dto : dtoList) {
                Field field = dto.getField();
                Object sourceObject = dto.getSourceObject();
                FieldValueFind fieldValueFind = dto.getFieldValueFind();
                ExistPolicy existPolicy = fieldValueFind.existPolicy();
                if (field.get(sourceObject) != null) {
                    if (ExistPolicy.KEEP_SELF.equals(existPolicy)) {
                        break;
                    }
                    if (!ExistPolicy.USE_FROM.equals(existPolicy)) {
                        throw new IllegalArgumentException();
                    }
                }
                list.add(dto);
            }
            //准备查询参数
            Set fromObjectValueSet = new HashSet();
            for (ProcessFieldDto dto : list) {
                Field fromField = dto.getSourceObject().getClass().getDeclaredField(dto.getFieldValueFind().fromField());
                fromField.setAccessible(true);
                Object fromFieldValue = fromField.get(dto.getSourceObject());
                fromField.setAccessible(false);
                fromObjectValueSet.add(fromFieldValue);
            }
            //执行查询
            Map resultMap = (Map) queryMethod.invoke(queryBean, new ArrayList<>(fromObjectValueSet));
            //设置结果
            for (ProcessFieldDto dto : list) {
                Field fromField = dto.getSourceObject().getClass().getDeclaredField(dto.getFieldValueFind().fromField());
                fromField.setAccessible(true);
                Object fromFieldValue = fromField.get(dto.getSourceObject());
                fromField.setAccessible(false);
                Object result = resultMap.get(fromFieldValue);
                dto.getField().set(dto.getSourceObject(), result);
            }

            //关闭权限
            for (ProcessFieldDto dto : dtoList) {
                dto.getField().setAccessible(false);
            }
        } catch (RuntimeException e) {
            log.error("findAndInjectValue failed,dtoList is:" + JacksonUtil.toJson(dtoList) + ",exception is:", e);
            throw e;
        } catch (Exception e) {
            log.error("findAndInjectValue failed,dtoList is:" + JacksonUtil.toJson(dtoList) + ",exception is:", e);
            throw new RuntimeException("FieldValueFindHelper process failed");
        }
    }

    public enum ExistPolicy {
        //继续使用自己的值
        KEEP_SELF,
        //使用查询过来的值
        USE_FROM
    }
    public enum QueryPolicy {
        //直接拿fromField查询，然后把查询结果设置回来
        DIRECT,
        //先把fromField的数据依次缓存到list中，一次性查询得到list结果，然后把查询结果依次设置回来
        BATCH
    }

    @Builder
    @Data
    static class ProcessFieldDto {
        private Object sourceObject;
        private Field field;
        private FieldValueFind fieldValueFind;
    }

}
