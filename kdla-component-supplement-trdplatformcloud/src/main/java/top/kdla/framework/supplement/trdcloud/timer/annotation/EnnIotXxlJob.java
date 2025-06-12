package top.kdla.framework.supplement.trdcloud.timer.annotation;


import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface EnnIotXxlJob {

    String value();

    String init() default "";

    String destroy() default "";
}
