package top.kdla.framework.supplement.lock.annotation;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import top.kdla.framework.supplement.lock.RedissonRedDisLock;

import java.lang.reflect.Method;
import java.util.concurrent.locks.Lock;

/**
 * @auther wangjyc@enn.cn
 * @date 2021-05-17
 */
@Component
@Aspect
@Slf4j
public class DistributeLockedAspect {

    @Autowired
    private RedissonRedDisLock redissonRedDisLock;

    private static final String LOCK_KEY_PREFIX = "UnblockDistributeLock:";

    @Pointcut(value = "@annotation(top.kdla.framework.supplement.lock.annotation.UnblockDistributeLocked)")
    public void pointCut() {
    }

    /**
     * 如果UnblockDistributeLocked注解throwEx为true,则为获取到锁时会抛出LockFailException异常
     * key值支持spel表达式,内置参数为args(切面方法的参数数组,如args[0]表示形参中的第一个参数)
     *
     * @throws Exception 未获取到锁异常
     * @throws Throwable 业务方法抛出的异常
     */
    @Around("pointCut()")
    public Object lockAround(ProceedingJoinPoint jp) throws Exception, Throwable {
        log.info("DistributeLockedAspect.lockAround");
        Object rvt = null;

        MethodSignature signature = (MethodSignature) jp.getSignature();
        Method method = jp.getTarget().getClass().getMethod(signature.getName(), signature.getParameterTypes());

        UnblockDistributeLocked lockAction = method.getAnnotation(UnblockDistributeLocked.class);

        String key = lockAction.key();

        if (lockAction.isSpelKey()) {//如果是spel表达式,解析之
            ExpressionParser spelExpressionParser = new SpelExpressionParser();
            Expression expression = spelExpressionParser.parseExpression(key);
            EvaluationContext evalContext = new StandardEvaluationContext(jp.getArgs());
            evalContext.setVariable("args", jp.getArgs());
            key = expression.getValue(evalContext).toString();
        }

        if (StringUtils.isBlank(key)) {//如果key值为空,不加锁,放行
            rvt = jp.proceed();
            return rvt;
        }

        Lock lock = null;
        try {
            key = LOCK_KEY_PREFIX + key;
            if (log.isDebugEnabled()) {
                log.debug("开始尝试上锁");
            }
            lock = redissonRedDisLock.lock(key);//非阻塞方法,取不到锁抛出LockFailException异常
            if (lock != null) {//获取到锁
                if (log.isDebugEnabled()) {
                    log.debug("上锁成功,执行业务代码");
                }
                rvt = jp.proceed();
            } else {
                throw new Exception("lock为空,未获取到锁");
            }
        } catch (Exception e) {
            if (lockAction.throwEx()) {
                throw new Exception("未获取到锁,请重新尝试");
            } else {
                log.warn("未获取到锁");
            }
        } finally {
            if (lock != null) {
                redissonRedDisLock.unlock(lock);
            }
        }

        return rvt;

    }
}
