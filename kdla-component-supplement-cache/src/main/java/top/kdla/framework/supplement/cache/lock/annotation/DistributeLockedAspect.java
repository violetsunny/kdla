package top.kdla.framework.supplement.cache.lock.annotation;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.SimpleEvaluationContext;
import top.kdla.framework.exception.LockFailException;
import top.kdla.framework.supplement.cache.lock.RedissonLockFactory;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

/**
 * 分布式锁切面，处理 {@link UnblockDistributeLocked} 注解。
 * <p>
 * <b>行为变更（1.0.1-SNAPSHOT, 2026-08-28）</b>：
 * 旧版本中 {@code redissonLockFactory.getLock(key)} 仅构造锁对象但从未真正加锁，
 * 注解形同虚设，业务永远放行。当前版本使用 {@code tryLock(0, -1, MS)} 真正加锁：
 * <ul>
 *   <li>{@code waitTime=0}：零等待，并发竞争时立即抛 {@link top.kdla.framework.exception.LockFailException}（throwEx=true）或返回 null（throwEx=false）</li>
 *   <li>{@code leaseTime=-1}：使用 Redisson 默认看门狗自动续期</li>
 * </ul>
 * 此前被掩盖的竞争问题会在升级后集中暴露，下游使用方需知晓该行为变更。
 *
 * @date 2021-05-17
 */
@Aspect
@Slf4j
public class DistributeLockedAspect {

    @Autowired
    private RedissonLockFactory redissonLockFactory;

    private static final String LOCK_KEY_PREFIX = "UnblockDistributeLock:";

    @Pointcut(value = "@annotation(top.kdla.framework.supplement.cache.lock.annotation.UnblockDistributeLocked)")
    public void pointCut() {
        if (log.isDebugEnabled()) {
            log.debug("--- DistributeLockedAspect start ---");
        }
    }

    /**
     * 如果UnblockDistributeLocked注解throwEx为true,则为获取到锁时会抛出LockFailException异常
     * key值支持spel表达式,内置参数为args(切面方法的参数数组,如args[0]表示形参中的第一个参数)
     *
     * @throws Exception 未获取到锁异常
     * @throws Throwable 业务方法抛出的异常
     */
    @Around("pointCut()")
    public Object lockAround(ProceedingJoinPoint jp) throws Throwable {
        if (log.isInfoEnabled()) {
            log.info("DistributeLockedAspect.lockAround");
        }
        Object rvt = null;

        MethodSignature signature = (MethodSignature) jp.getSignature();
        Method method = jp.getTarget().getClass().getMethod(signature.getName(), signature.getParameterTypes());

        UnblockDistributeLocked lockAction = method.getAnnotation(UnblockDistributeLocked.class);

        String key = lockAction.key();

        if (lockAction.isSpelKey()) {
            ExpressionParser spelExpressionParser = new SpelExpressionParser();
            Expression expression = spelExpressionParser.parseExpression(key);
            EvaluationContext evalContext = SimpleEvaluationContext.forReadOnlyDataBinding().build();
            evalContext.setVariable("args", jp.getArgs());
            Object eval = expression.getValue(evalContext);
            key = eval == null ? "" : String.valueOf(eval);
        }

        if (StringUtils.isBlank(key)) {
            rvt = jp.proceed();
            return rvt;
        }

        RLock lock = null;
        boolean locked = false;
        try {
            key = LOCK_KEY_PREFIX + key;
            if (log.isInfoEnabled()) {
                log.info("开始尝试上锁");
            }
            lock = redissonLockFactory.getLock(key);
            if (lock != null) {
                locked = lock.tryLock(0, -1, TimeUnit.MILLISECONDS);
            }
            if (locked) {
                if (log.isInfoEnabled()) {
                    log.info("上锁成功,执行业务代码");
                }
                rvt = jp.proceed();
            } else {
                if (lockAction.throwEx()) {
                    throw new LockFailException("lock为空,未获取到锁");
                } else {
                    if (log.isWarnEnabled()) {
                        log.warn("未获取到锁");
                    }
                }
            }
        } finally {
            if (locked && lock != null) {
                try {
                    if (lock.isHeldByCurrentThread()) {
                        lock.unlock();
                    }
                } catch (Exception e) {
                    if (log.isWarnEnabled()) {
                        log.warn("unlock failed:", e);
                    }
                }
            }
        }

        return rvt;

    }
}
