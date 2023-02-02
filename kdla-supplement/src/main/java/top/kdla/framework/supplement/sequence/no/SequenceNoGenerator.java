package top.kdla.framework.supplement.sequence.no;

import cn.hutool.core.date.DateUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.redisson.api.RLock;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StopWatch;
import top.kdla.framework.exception.BizException;
import top.kdla.framework.supplement.lock.RedissonRedDisLock;
import top.kdla.framework.supplement.sequence.no.mapper.CodeGeneratorCfgMapper;
import top.kdla.framework.supplement.sequence.no.model.entity.CodeGeneratorCfg;

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

    public void setRedissonClient(RedissonRedDisLock redDisLock) {
        this.redissonRedDisLock = redDisLock;
    }

    public void setSequenceNoLockKey(String sequenceNoLockKey) {
        this.sequenceNoLockKey = sequenceNoLockKey;
    }

    private static Map<String, Map<String, ConcurrentLinkedQueue<String>>> noCacheMap = new ConcurrentHashMap();
    private static Map<String, String> cfgRuleCacheMap = new ConcurrentHashMap();

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

    private String createNo(String code,String key) throws Exception {
        if (StringUtils.isBlank(code)) {
            throw new BizException("参数code为空");
        }

        String sequenceNo = null;
        synchronized (this) {
            sequenceNo = getSequenceNoFromLocalCache(code, key);
            if (sequenceNo == null) {
                RLock lock = redissonRedDisLock.lock(sequenceNoLockKey + code);
                boolean isLocked = false;
                try {
                    isLocked = lock.tryLock(WAITE_TIME, LEASE_TIME, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    log.error("createNo,tryLock exception:", e);
                }
                if (!isLocked) {
                    log.warn("createNo failed,get lock failed");
                    throw new BizException("生成流水号失败");
                }
                try {
                    if (sequenceNo == null) {
                        sequenceNo = generateSequenceNo(code);
                    }
                } finally {
                    try {
                        lock.unlock();
                    } catch (Exception e) {
                        log.warn("createNo,unlock failed,exception is:", e);
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
        log.info("getKey consume time:{}", stopWatch);
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
        } catch (Exception var5) {
            noCacheMap.clear();
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
        String lengthStr = split[2];
        if (NumberUtils.isNumber(lengthStr)) {
            if (cfg.getIsCache() == 1) {
                ConcurrentLinkedQueue<String> queue = new ConcurrentLinkedQueue();
                this.generate(cfg.getCacheNum(), headStr, dbMaxSequenceNo, dbMaxKey, lengthStr, cfg, queue);
                returnSequence = queue.poll();
                ConcurrentHashMap queueMap = new ConcurrentHashMap();
                queueMap.put(headStr, queue);
                noCacheMap.put(cfg.getCode(), queueMap);
            } else {
                returnSequence = this.generate(1, headStr, dbMaxSequenceNo, dbMaxKey, lengthStr, cfg, null);
            }
        }

        return returnSequence;
    }

    private String generate(Integer cacheNum, String key, String dbMaxSequenceNo, String dbMaxKey, String sequenceLen,
                            CodeGeneratorCfg mod, ConcurrentLinkedQueue<String> queue) {
        String currentMaxNo = "";

        for (int i = 1; i < cacheNum + 1; ++i) {
            Long sequenceNo = new Long(dbMaxSequenceNo) + (long)i;
            if (sequenceNo.toString().length() > Integer.valueOf(sequenceLen)) {
                log.warn("{}生成的号码已经超过最大限度，当前最大编号为{},生成号码失败", mod.getCode(), dbMaxSequenceNo);
                break;
            }
            if (StringUtils.isNotBlank(dbMaxKey)) {
                if (dbMaxKey.equals(key)) {
                    currentMaxNo =
                            dbMaxKey + fillLeft(sequenceNo.toString(), DEFAULT_FILL_CHAR, Integer.valueOf(sequenceLen));
                } else {
                    //如果key不同了，说明时间走到了下一个节点，开始重新计数
                    sequenceNo = 1L;
                    dbMaxSequenceNo = "0";
                    dbMaxKey = key;
                    currentMaxNo =
                            key + fillLeft(sequenceNo.toString(), DEFAULT_FILL_CHAR, Integer.valueOf(sequenceLen));
                }
            } else {
                currentMaxNo = fillLeft(sequenceNo.toString(), DEFAULT_FILL_CHAR, Integer.valueOf(sequenceLen));
            }

            if (cacheNum > 1 && queue != null) {
                queue.offer(currentMaxNo);
            }
        }

        mod.setMaxValue(currentMaxNo);
        return currentMaxNo;
    }

    private static String fillLeft(String source, char fillChar, int len) {
        StringBuffer ret = new StringBuffer();
        if (source == null) {
            ret.append("");
        }

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
