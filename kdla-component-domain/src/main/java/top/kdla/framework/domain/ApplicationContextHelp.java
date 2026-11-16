package top.kdla.framework.domain;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.util.Map;

/**
 * ApplicationContextHelper
 *
 * @author kll
 * @since 2021/7/9 14:15
 */
@Slf4j
public class ApplicationContextHelp implements ApplicationContextAware {
    private static ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        ApplicationContextHelp.applicationContext = applicationContext;
    }

    /**
     * 容器未初始化时抛出明确异常，而非在调用点抛NPE
     *
     * @return 已注入的ApplicationContext
     */
    private static ApplicationContext requireContext() {
        if (applicationContext == null) {
            throw new IllegalStateException("ApplicationContext 未初始化：ApplicationContextHelp 尚未被 Spring 注入，请确认其已注册为 Bean");
        }
        return applicationContext;
    }

    public static <T> T getBean(Class<T> targetClz) {
        ApplicationContext context = requireContext();
        T beanInstance = null;
        //优先按type查
        try {
            beanInstance = context.getBean(targetClz);
        } catch (Exception e) {
            // 按type查失败（无此bean/多实例等）记录日志后降级按name查，不再静默吞异常
            log.warn("getBean by type [{}] failed, fallback to name lookup: {}", targetClz.getName(), e.getMessage());
        }
        //按name查
        if (beanInstance == null) {
            String simpleName = targetClz.getSimpleName();
            //首字母小写
            simpleName = Character.toLowerCase(simpleName.charAt(0)) + simpleName.substring(1);
            try {
                beanInstance = (T) context.getBean(simpleName);
            } catch (NoSuchBeanDefinitionException e) {
                // 按name也未命中时不再直接上抛，落入下方统一的友好报错（此前该异常直接抛出，友好报错不可达）
                log.warn("getBean by name [{}] not found in Spring Container", simpleName);
            }
        }
        if (beanInstance == null) {
            throw new RuntimeException("Component " + targetClz + " can not be found in Spring Container");
        }
        return beanInstance;
    }

    public static Object getBean(String claz) {
        return requireContext().getBean(claz);
    }

    public static <T> T getBean(String name, Class<T> requiredType) {
        return requireContext().getBean(name, requiredType);
    }

    public static <T> T getBean(Class<T> requiredType, Object... params) {
        return requireContext().getBean(requiredType, params);
    }

    public static <T> Map<String, T> getBeansOfType(Class<T> requiredType){
        return requireContext().getBeansOfType(requiredType);
    }

    public static ApplicationContext getApplicationContext() {
        return applicationContext;
    }
}
