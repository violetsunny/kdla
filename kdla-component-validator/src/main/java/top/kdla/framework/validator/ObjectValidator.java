/**
 * kanglele Inc. Copyright (c) 2021 All Rights Reserved.
 */
package top.kdla.framework.validator;

import com.baidu.unbiz.fluentvalidator.Validator;
import com.baidu.unbiz.fluentvalidator.ValidatorHandler;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;
import top.kdla.framework.dto.exception.ErrorCode;
import top.kdla.framework.dto.IEnum;
import top.kdla.framework.exception.BizException;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.Temporal;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 对复杂对象中有需要校验的注解进行校验
 *
 * @author kanglele
 * @version $Id: ObjectValidator, v 0.1 2021/9/29 19:00 Exp $
 */
public class ObjectValidator<T> extends ValidatorHandler<T> implements Validator<T> {

    /**
     * 递归深度上限：超过后不再向下遍历，防止超深对象图拖垮校验
     */
    private static final int MAX_RECURSION_DEPTH = 16;

    /**
     * 如果校验不过会抛出BizException
     *
     * @throws BizException
     */
    public <T> void getValidatorMsg(T t, Class<? extends Annotation> annotation, Object bizEnum) throws Exception {
        List<String> msg = new ArrayList<>();
        // 深度上限 + 按对象身份(identity)去重的已访问集合，防循环引用导致StackOverflowError
        getClassErrorMsg(t, annotation, bizEnum, msg, 0, Collections.newSetFromMap(new IdentityHashMap<Object, Boolean>()));
        if (!CollectionUtils.isEmpty(msg)) {
            throw new BizException(ErrorCode.PARAMETER_ERROR, StringUtils.join(msg, ","));
        }
    }

    /**
     * 对象越复杂层级越深，性能就越差
     *
     * @param t          需要被校验的实例对象
     * @param annotation 标记的注解，必须要指定（match，nonMatch，message）
     *                   例如：
     *                   Object[] match() default {};
     *                   Object[] nonMatch() default {};
     *                   String message() default "{validator's value is invalid}";
     * @param bizObj     当前需要匹配的数据
     * @param msg        错误信息
     * @param depth      当前递归深度
     * @param visited    已访问对象集合（按引用身份判重）
     * @param <T>
     * @throws Exception
     */
    private <T> void getClassErrorMsg(T t, Class<? extends Annotation> annotation, Object bizObj, List<String> msg,
                                      int depth, Set<Object> visited) throws Exception {
        if (t == null || depth > MAX_RECURSION_DEPTH) {
            return;
        }
        // 循环引用防护：同一对象只遍历一次
        if (!visited.add(t)) {
            return;
        }
        // 沿继承链向上收集字段（getDeclaredFields不含父类字段，此前父类上的校验注解全部漏检），到Object为止
        List<Field> fields = new ArrayList<>();
        for (Class<?> clazz = t.getClass(); clazz != null && clazz != Object.class; clazz = clazz.getSuperclass()) {
            fields.addAll(Arrays.asList(clazz.getDeclaredFields()));
        }
        for (Field field : fields) {
            // 静态字段（serialVersionUID/Logger等）不属于实例校验范围，跳过
            if (Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            // 设置属性可以直接的进行访问
            field.setAccessible(true);
            Object fieldObject = field.get(t);

            if (field.isAnnotationPresent(annotation)) {
                Method method_match = annotation.getMethod("match", null);
                Method method_nonMatch = annotation.getMethod("nonMatch", null);
                Method method_message = annotation.getMethod("message", null);

                // 获取到注解对象
                Object annotationObj = field.getAnnotation(annotation);
                // 反射调用方法 method.invoke(静态方法是null非静态方法是所属实例Obj,参数)
                Object[] objs_match = (Object[]) method_match.invoke(annotationObj, null);
                Object[] objs_nonMatch = (Object[]) method_nonMatch.invoke(annotationObj, null);
                //空是都校验，不需要校验则跳过
                if ((objs_match.length == 0 || Arrays.asList(objs_match).contains(bizObj))
                        && !Arrays.asList(objs_nonMatch).contains(bizObj)) {
                    // 判空
                    boolean result = Objects.nonNull(fieldObject);
                    if (result) {
                        if (fieldObject instanceof String) {
                            result = StringUtils.isNotBlank((String) fieldObject);
                        } else if (fieldObject instanceof BigDecimal) {
                            result = BigDecimal.ZERO.compareTo((BigDecimal) fieldObject) <= 0;
                        } else if (fieldObject instanceof Collection) {
                            result = !CollectionUtils.isEmpty((Collection) fieldObject);
                        } else if (fieldObject instanceof Map) {
                            result = !CollectionUtils.isEmpty((Map) fieldObject);
                        } else if (fieldObject instanceof Object[]) {
                            result = ArrayUtils.isNotEmpty((Object[]) fieldObject);
                        }
                    }
                    if (!result) {
                        msg.add((String) method_message.invoke(annotationObj, null));
                    }
                }
            }

            // 递归获取其他值 剔除常用类型
            if (Objects.nonNull(fieldObject) && !isCommonlyClass(field.getType())) {
                if (fieldObject instanceof Collection) {
                    Collection coll = (Collection) fieldObject;
                    for (Object oj : coll) {
                        getClassErrorMsg(oj, annotation, bizObj, msg, depth + 1, visited);
                    }
                } else if (fieldObject instanceof Map) {
                    Map map = (Map) fieldObject;
                    for (Object oj : map.values()) {
                        getClassErrorMsg(oj, annotation, bizObj, msg, depth + 1, visited);
                    }
                } else if (fieldObject instanceof Object[]) {
                    Object[] objects = (Object[]) fieldObject;
                    for (Object oj : objects) {
                        getClassErrorMsg(oj, annotation, bizObj, msg, depth + 1, visited);
                    }
                } else {
                    getClassErrorMsg(fieldObject, annotation, bizObj, msg, depth + 1, visited);
                }
            }
        }

    }

    /**
     * 判断字段类型是否命中常用类型排除列表。
     * 用isAssignableFrom匹配（此前equals精确匹配，Temporal/Enum/Number等父类型对子类不生效，
     * 如自定义枚举、Instant仍被递归遍历）。
     *
     * @param type 字段声明类型
     * @return true表示不再向下递归
     */
    private static boolean isCommonlyClass(Class<?> type) {
        for (Class<?> commonlyClass : getCommonlyClass()) {
            if (commonlyClass.isAssignableFrom(type)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 如果实体中有新的基本类型，不能再往子属性中校验的，一定要在getCommonlyClass加入，剔除掉。
     * 不然会报 StackOverflowError：ObjectValidator.getClassErrorMsg
     *
     * @return
     */
    private static List<Class<?>> getCommonlyClass() {
        return Arrays.asList(String.class, Number.class, Integer.class, Long.class, Boolean.class, BigDecimal.class, Float.class,
                Double.class, Byte.class, Date.class, LocalDateTime.class, LocalDate.class, LocalTime.class,
                BigInteger.class, Short.class, Character.class, boolean.class, int.class, long.class, float.class,
                double.class, byte.class, char.class, AtomicBoolean.class, AtomicInteger.class,
                AtomicLong.class, ThreadLocal.class, CharSequence.class, Temporal.class, Enum.class, IEnum.class);
    }

}
