package top.kdla.framework.common.help;

import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;

/**
 * @author
 * @version Id: AopTargetUtils, v 0.1 2018/2/28 20:14 czj12867 Exp $
 */
public class AopTargetHelp {

    /**
     * Constructor AopTargetUtils creates a new AopTargetUtils instance.
     */
    private AopTargetHelp() {
        throw new IllegalAccessError("Utility class");
    }

    /**
     * 获取 目标对象
     * @param proxy 代理对象
     * @return
     * @throws Exception
     */
    public static Object getTarget(Object proxy) {
        if (!AopUtils.isAopProxy(proxy)) {
            //不是代理对象
            return proxy;
        }
        try {
            // Spring JDK/CGLIB 代理均实现 Advised，直接取目标；反射 Proxy.h 在 JDK17+ 抛 InaccessibleObjectException
            return ((Advised) proxy).getTargetSource().getTarget();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}