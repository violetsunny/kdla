package top.kdla.framework.fieldvaluefinder;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

@Component
public class FieldValueFindApplicationContextUtils implements ApplicationContextAware {

    private static ApplicationContext applicationContext;

    /**
     * @param applicationContext
     * @throws BeansException
     */
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        FieldValueFindApplicationContextUtils.applicationContext = applicationContext;
    }

    /**
     * @return
     */
    public static ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    /**
     * @param beanId
     * @return
     * @throws BeansException
     */
    public static <T> T getBean(String beanId, Class<T> clazz) throws BeansException {
        return (T) applicationContext.getBean(beanId);
    }

    /**
     * @param clazz
     * @param <T>
     * @return
     */
    public static <T> T getBean(Class<T> clazz) {
        return getApplicationContext().getBean(clazz);
    }

    public static Object getBean(String name) throws BeansException {
        return applicationContext.getBean(name);
    }
}