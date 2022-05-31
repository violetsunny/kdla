package top.kdla.framework.fieldvaluefinder;



import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * id、no转换注解
 *
 * @author kll
 * @date 2021/12/7
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface FieldValueFind {

    /**
     * 做查询的类
     * @return
     */
    Class queryClass();

    /**
     * 做查询的方法
     * @return
     */
    String queryMethod();
    /**
     * 计算查询的来源字段。
     * @return
     */
    String fromField();

    /**
     * 当本字段已经有值时，应当采取何种策略来处置。
     * @return
     */
    FieldValueFindHelper.ExistPolicy existPolicy() default FieldValueFindHelper.ExistPolicy.KEEP_SELF;

    /**
     * 查询策略
     * @return
     */
    FieldValueFindHelper.QueryPolicy queryPolicy() default FieldValueFindHelper.QueryPolicy.DIRECT;
}
