package top.kdla.framework.supplement.cache.sequence;

import cn.hutool.core.date.DateUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.redisson.api.RLock;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StopWatch;
import top.kdla.framework.exception.BizException;
import top.kdla.framework.exception.LockFailException;
import top.kdla.framework.supplement.cache.lock.RedissonLockFactory;
import top.kdla.framework.supplement.cache.lock.RedissonRedDisLock;
import top.kdla.framework.supplement.cache.sequence.mapper.CodeGeneratorCfgMapper;
import top.kdla.framework.supplement.cache.sequence.model.entity.CodeGeneratorCfg;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

/**
 * 序列号生成器
 *
 * @author kll
 */
@Slf4j
public class SequenceNoGenerator {

    private CodeGeneratorCfgMapper codeGeneratorCfgMapper;

    private RedissonRedDisLock redissonRedDisLock;

    private String sequenceNoLockKey;

    public void setCodeGeneratorCfgMapper(CodeGeneratorCfgMapper codeGeneratorCfgMapper) {
        this.codeGeneratorCfgMapper = codeGeneratorCfgMapper;
    }

    public void setRedissonClient(RedissonRedDisLock redissonRedDisLock) {
        this.redissonRedDisLock = redissonRedDisLock;
    }

    public void setSequenceNoLockKey(String sequenceNoLockKey) {
        this.sequenceNoLockKey = sequenceNoLockKey;
    }

    private static Map<String, Map<String, ConcurrentLinkedQueue<String>>> noCacheMap = new ConcurrentHashMap<>();
    private static Map<String, String> cfgRuleCacheMap = new ConcurrentHashMap<>();

    private static final int DEFAULT_CACHE_NUM = 1000;
    private static final char DEFAULT_FILL_CHAR = '0';
    private static final String RULE_GLIDE_SEPARATE = "+";
    private static final String DEFAULT_RULE = "yyyyMMdd+8";

    private static final long WAITE_TIME = 1_000;
    private static final long LEASE_TIME = 10_000;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String getMaxNo(String code) throws Exception {
        try {
            return this.createNo(code, getKey(code));
        } catch (BizException e) {
            log.error("[SequenceNoGenerator.createMaxNo.error]业务处理异常", e);
            throw e;
        } catch (Exception e) {
            log.error("[SequenceNoGenerator.createMaxNo.error]未知异常", e);
            throw e;
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<String> batchGetMaxNo(String code, int count) throws Exception {
        try {
            if (count < 1) {
                throw new BizException("不合法的入参count:" + count);
            }

            String key = getKey(code);
            List<String> list = new ArrayList<>();
            for (int i = 1; i <= count; i++) {
                list.add(this.createNo(code, key));
            }
            return list;
        } catch (BizException e) {
            log.error("[SequenceNoGenerator.createMaxNo.error]业务处理异常", e);
            throw e;
        } catch (Exception e) {
            log.error("[SequenceNoGenerator.createMaxNo.error]未知异常", e);
            throw e;
        }
    }

    private String createNo(String code, String key) throws Exception {
        if (StringUtils.isBlank(code)) {
            throw new BizException("参数code为空");
        }

        String sequenceNo = null;
        synchronized (this) {
            sequenceNo = getSequenceNoFromLocalCache(code, key);
            if (sequenceNo == null) {
                // 通过lock(key,unit,waitTime,leaseTime)一次完成加锁；
                // 旧实现lock(key)预加锁后又tryLock，可重入计数为2而finally只unlock一次，锁永不释放
                RLock lock = null;
                boolean isLocked = false;
                try {
                    lock = redissonRedDisLock.lock(sequenceNoLockKey + code, TimeUnit.MILLISECONDS, WAITE_TIME, LEASE_TIME);
                    isLocked = true;
                } catch (LockFailException e) {
                    log.error("createNo,tryLock exception:", e);
                }
                if (!isLocked) {
                    if (log.isWarnEnabled()) {
                        log.warn("createNo failed,get lock failed");
                    }
                    throw new BizException("生成流水号失败");
                }
                try {
                    if (sequenceNo == null) {
                        sequenceNo = generateSequenceNo(code);
                    }
                } finally {
                    try {
                        if (lock.isHeldByCurrentThread()) {
                            lock.unlock();
                        }
                    } catch (Exception e) {
                        if (log.isWarnEnabled()) {
                            log.warn("createNo,unlock failed,exception is:", e);
                        }
                    }
                }
            }

            return sequenceNo;
        }
    }

    private String getSequenceNoFromLocalCache(String code, String key) {
        String sequenceNo = null;
        Map<String, ConcurrentLinkedQueue<String>> queueMap = noCacheMap.get(code);
        if (queueMap != null) {
            ConcurrentLinkedQueue<String> sequenceNolist = queueMap.get(key);
            if (sequenceNolist != null && !sequenceNolist.isEmpty()) {
                sequenceNo = sequenceNolist.poll();
            }
        }
        return sequenceNo;
    }

    private String getKey(String code) {
        StopWatch stopWatch = new StopWatch("getKey");
        stopWatch.start("getFromCache");
        String rule = cfgRuleCacheMap.get(code);
        if (StringUtils.isBlank(rule)) {
            CodeGeneratorCfg cfg = getCfg(code);
            if (cfg == null) {
                throw new BizException("生成流水号失败，不合法的规则code:" + code);
            }
            rule = cfg.getRule();
            cfgRuleCacheMap.put(code, cfg.getRule());
        }
        stopWatch.stop();
        stopWatch.start("getHeadStr");
        String headStr = getHeadStr(rule);
        stopWatch.stop();
        if (log.isInfoEnabled()) {
            log.info("getKey consume time:{}", stopWatch);
        }
        return headStr;
    }

    private String generateSequenceNo(final String code) {
        String remark = UUID.randomUUID().toString();
        codeGeneratorCfgMapper.update(code, remark);
        CodeGeneratorCfg cfg = getCfg(code);
        if (cfg == null) {
            throw new BizException("流水号规则不存在");
        }
        setDefaultValue(cfg);

        try {
            String maxSequenceNum = getSequenceNo(cfg);
            if (StringUtils.isBlank(maxSequenceNum)) {
                throw new BizException("获取流水号失败");
            }
            cfg.setRemark(remark);
            int i = codeGeneratorCfgMapper.updateByIdAndRemark(cfg);
            if (i != 1) {
                throw new BizException("获取流水号失败");
            }
            return maxSequenceNum;
        } catch (BizException e) {
            //旧实现catch(Exception)统一转通用"获取流水号失败"且不记录日志，吞掉根因(如溢出拒绝/规则不合法原因)；
            //改为保留具体业务异常并记录日志，失败可追溯
            noCacheMap.remove(code);
            log.error("[SequenceNoGenerator.generateSequenceNo]业务异常, code:{}", code, e);
            throw e;
        } catch (Exception e) {
            noCacheMap.remove(code);
            log.error("[SequenceNoGenerator.generateSequenceNo]未知异常, code:{}", code, e);
            throw new BizException("获取流水号失败");
        }

    }

    private CodeGeneratorCfg getCfg(String code) {
        CodeGeneratorCfg entity = new CodeGeneratorCfg();
        entity.setCode(code);
        return this.codeGeneratorCfgMapper.selectOne(entity);
    }

    private String getHeadStr(String rule) {
        String[] split = rule.split("\\" + RULE_GLIDE_SEPARATE);
        if (split.length < 2) {
            throw new BizException("流水号规则格式不合法，规则内容：" + rule);
        }
        String headStr = split[0];
        String timeRule = split[1];
        String timeStr = "";
        if (!StringUtils.isBlank(timeRule)) {
            timeStr = DateUtil.format(new Date(), timeRule);
        }
        headStr += timeStr;

        return headStr;
    }


    private String getSequenceNo(CodeGeneratorCfg cfg) {
        String returnSequence = "";
        String rule = cfg.getRule().trim();
        String dbMaxNum = cfg.getMaxValue();
        String dbMaxSequenceNo = "0";
        String dbMaxKey = "";
        String headStr = getHeadStr(rule);
        if (StringUtils.isNotBlank(dbMaxNum) && dbMaxNum.length() > headStr.length()) {
            dbMaxKey = dbMaxNum.substring(0, headStr.length());
            dbMaxSequenceNo = dbMaxNum.substring(headStr.length());
        } else {
            dbMaxKey = headStr;
        }

        String[] split = rule.split("\\" + RULE_GLIDE_SEPARATE);
        if (split.length < 3) {
            throw new BizException("流水号规则格式不合法（需要3段），规则内容：" + rule);
        }
        String lengthStr = split[2];
        if (NumberUtils.isCreatable(lengthStr)) {
            if (cfg.getIsCache() == 1) {
                ConcurrentLinkedQueue<String> queue = new ConcurrentLinkedQueue<>();
                this.generate(cfg.getCacheNum(), headStr, dbMaxSequenceNo, dbMaxKey, lengthStr, cfg, queue);
                returnSequence = queue.poll();
                ConcurrentHashMap<String,ConcurrentLinkedQueue<String>> queueMap = new ConcurrentHashMap<>();
                queueMap.put(headStr, queue);
                noCacheMap.put(cfg.getCode(), queueMap);
            } else {
                returnSequence = this.generate(1, headStr, dbMaxSequenceNo, dbMaxKey, lengthStr, cfg, null);
            }
        } else {
            //旧实现lengthStr非合法数字时静默返回空串，根因不可追溯；改为显式抛出规则错误
            throw new BizException("流水号规则长度段不合法（非数字），规则内容：" + rule);
        }

        return returnSequence;
    }

    private String generate(Integer cacheNum, String key, String dbMaxSequenceNo, String dbMaxKey, String sequenceLen,
                            CodeGeneratorCfg mod, ConcurrentLinkedQueue<String> queue) {
        String currentMaxNo = "";

        for (int i = 1; i < cacheNum + 1; ++i) {
            Long sequenceNo = Long.valueOf(dbMaxSequenceNo) + (long) i;
            //先判断日期(key)是否翻越：翻越则本周期从1重新计数，并把dbMaxKey更新为当前key
            if (StringUtils.isNotBlank(dbMaxKey) && !dbMaxKey.equals(key)) {
                //如果key不同了，说明时间走到了下一个节点，开始重新计数
                sequenceNo = 1L;
                dbMaxSequenceNo = "0";
                dbMaxKey = key;
            }
            //溢出检查必须在日期翻越重置之后：旧实现先用上一周期最大编号判断，
            //导致翻越当天首个号码在上一周期编号已达位宽上限时(如上一周期99+1=100超过2位)被误判溢出
            if (sequenceNo.toString().length() > Integer.parseInt(sequenceLen)) {
                if (log.isWarnEnabled()) {
                    log.warn("{}生成的号码已经超过最大限度，当前最大编号为{},生成号码失败", mod.getCode(), dbMaxSequenceNo);
                }
                // 序列溢出时拒绝生成：旧实现break后setMaxValue("")会把maxValue清零，导致号码从头重复
                // （design决策9：溢出进位或抛异常，禁止清零；抛异常侵入最小，进位需变更位宽语义与DB列约束）
                throw new BizException("流水号已超过最大限度，code:" + mod.getCode() + "，当前最大编号:" + dbMaxSequenceNo);
            }
            if (StringUtils.isNotBlank(dbMaxKey)) {
                currentMaxNo =
                        dbMaxKey + fillLeft(sequenceNo.toString(), DEFAULT_FILL_CHAR, Integer.parseInt(sequenceLen));
            } else {
                currentMaxNo = fillLeft(sequenceNo.toString(), DEFAULT_FILL_CHAR, Integer.parseInt(sequenceLen));
            }

            if (cacheNum > 1 && queue != null) {
                queue.offer(currentMaxNo);
            }
        }

        mod.setMaxValue(currentMaxNo);
        return currentMaxNo;
    }

    private static String fillLeft(String source, char fillChar, int len) {
        if (source == null) {
            source = "";
        }
        StringBuilder ret = new StringBuilder();

        if (source.length() > len) {
            ret.append(source);
        } else {
            int slen = source.length();

            while (ret.toString().length() + slen < len) {
                ret.append(fillChar);
            }

            ret.append(source);
        }

        return ret.toString();
    }

    private void setDefaultValue(CodeGeneratorCfg cfg) {
        if (StringUtils.isBlank(cfg.getRule())) {
            cfg.setRule(DEFAULT_RULE);
        }

        if (cfg.getIsCache() == null) {
            cfg.setIsCache(0);
        }

        if (cfg.getIsCache() != null && cfg.getIsCache() == 1 && cfg.getCacheNum() == null) {
            cfg.setCacheNum(DEFAULT_CACHE_NUM);
        }

        if (cfg.getMaxValue() == null) {
            cfg.setMaxValue("");
        }

    }
}
