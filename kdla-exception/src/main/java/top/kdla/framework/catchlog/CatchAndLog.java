package top.kdla.framework.catchlog;

/**
 * CatchAndLog
 *
 * @author vincent.li
 * @since 2021/7/9 14:15
 */

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface CatchAndLog {

}
