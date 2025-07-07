package top.kdla.framework.supplement.cache.lock.annotation;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.redisson.api.RLock;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import top.kdla.framework.exception.LockFailException;
import top.kdla.framework.supplement.cache.lock.KRedissonRedDisLock;

import java.util.concurrent.TimeUnit;

/**
 * @date 2021-05-17
 */
@Aspect
@Slf4j
public class KDistributeLockedAspect {

    private KRedissonRedDisLock redissonRedDisLock;

    public KDistributeLockedAspect(KRedissonRedDisLock redissonRedDisLock) {
        this.redissonRedDisLock = redissonRedDisLock;
    }

    private static final String LOCK_KEY_PREFIX = "KUnblockDistributeLock:";

    @Pointcut("@annotation(lockAction) && execution(public * *(..))")
    public void pointcut(KUnblockDistributeLocked lockAction) {
        if (log.isDebugEnabled()) {
            log.debug("--- KDistributeLockedAspect start ---");
        }
    }

    /**
     * 如果UnblockDistributeLocked注解throwEx为true,则为获取到锁时会抛出LockFailException异常
     * key值支持spel表达式,内置参数为args(切面方法的参数数组,如args[0]表示形参中的第一个参数)
     *
     * @throws Exception 未获取到锁异常
     * @throws Throwable 业务方法抛出的异常
     */
    @Around(value = "pointcut(lockAction)", argNames = "jp,lockAction")
    public Object lockAround(ProceedingJoinPoint jp, KUnblockDistributeLocked lockAction) throws Exception, Throwable {
        if (log.isInfoEnabled()) {
            log.info("KDistributeLockedAspect lockAround");
        }
        Object rvt = null;

        //MethodSignature signature = (MethodSignature) jp.getSignature();
        //Method method = jp.getTarget().getClass().getMethod(signature.getName(), signature.getParameterTypes());
        //UnblockDistributeLocked lockAction = method.getAnnotation(UnblockDistributeLocked.class);

        String lockKey = lockAction.key();
        long timeout = lockAction.timeout();
        long leaseTime = lockAction.leaseTime();

        //如果key值为空,不加锁,放行
        if (lockKey == null || lockKey.trim().isEmpty()) {
            rvt = jp.proceed();
            return rvt;
        }

        //如果是spel表达式,解析之
        if (lockAction.isSpelKey()) {
            ExpressionParser spelExpressionParser = new SpelExpressionParser();
            Expression expression = spelExpressionParser.parseExpression(lockKey);
            EvaluationContext evalContext = new StandardEvaluationContext(jp.getArgs());
            evalContext.setVariable("args", jp.getArgs());
            Object eval = expression.getValue(evalContext);
            lockKey = eval == null ? "" : String.valueOf(eval);
        }

        RLock lock = null;
        try {
            //key = LOCK_KEY_PREFIX + key;
            if (log.isInfoEnabled()) {
                log.info("KDistributeLockedAspect {} 开始尝试上锁", lockKey);
            }
            //非阻塞方法,取不到锁抛出LockFailException异常
            lock = redissonRedDisLock.lock(lockKey, TimeUnit.MILLISECONDS, timeout, leaseTime);
            //获取到锁
            if (lock != null) {
                if (log.isInfoEnabled()) {
                    log.info("KDistributeLockedAspect {} 上锁成功,执行业务代码", lockKey);
                }
                rvt = jp.proceed();
            } else {
                throw new LockFailException("KDistributeLockedAspect " + lockKey + " lock为空,未获取到锁");
            }
        } catch (Exception e) {
            if (lockAction.throwEx()) {
                throw new Exception("KDistributeLockedAspect " + lockKey + " 未获取到锁,请重新尝试");
            } else {
                if (log.isWarnEnabled()) {
                    log.warn("KDistributeLockedAspect {} 未获取到锁", lockKey);
                }
            }
        } finally {
            if (lock != null) {
                lock.unlock();
            }
        }

        return rvt;

    }
}
