package top.kdla.framework.supplement.timer.annotation;


import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface IotXxlJob {

    String value();

    String init() default "";

    String destroy() default "";
}
