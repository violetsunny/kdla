package top.kdla.framework.domain;

/**
 * DomainFactory
 *
 * @author kll
 * @since 2021/7/9 14:15
 */
public class DomainFactory {

    public static <T> T create(Class<T> entityClz){
        return ApplicationContextHelp.getBean(entityClz);
    }

}
