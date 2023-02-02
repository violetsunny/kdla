package top.kdla.framework.sequence.no;

import cn.hutool.core.date.DateUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import top.kdla.framework.common.utils.JacksonUtil;
import top.kdla.framework.exception.BizException;
import top.kdla.framework.lock.RedissonRedDisLock;
import top.kdla.framework.sequence.no.mapper.CodeGeneratorCfgV2Mapper;
import top.kdla.framework.sequence.no.model.rules.*;
import top.kdla.framework.sequence.no.model.entity.CodeGeneratorCfgV2;
import top.kdla.framework.sequence.no.model.rules.epoch.EpochRule;
import top.kdla.framework.sequence.no.model.rules.random.RandomTypeEnum;
import top.kdla.framework.sequence.no.model.rules.random.RandomTypeRule;
import top.kdla.framework.sequence.no.model.rules.epoch.EpochRuleTypeEnum;
import top.kdla.framework.sequence.no.model.rules.sequence.SequenceTypeRule;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 序列号生成器
 *
 * @author kll
 */
@Slf4j
public class SequenceNoGeneratorV2 {

    private CodeGeneratorCfgV2Mapper codeGeneratorCfgMapper;

    private RedissonRedDisLock redissonRedDisLock;

    private String sequenceNoLockKey;

    public void setCodeGeneratorCfgMapper(CodeGeneratorCfgV2Mapper codeGeneratorCfgMapper) {
        this.codeGeneratorCfgMapper = codeGeneratorCfgMapper;
    }

    public void setRedissonClient(RedissonRedDisLock redissonRedDisLock) {
        this.redissonRedDisLock = redissonRedDisLock;
    }

    public void setSequenceNoLockKey(String sequenceNoLockKey) {
        this.sequenceNoLockKey = sequenceNoLockKey;
    }

    private static Map<String, Map<String, ConcurrentLinkedQueue<String>>> noCacheMap = new ConcurrentHashMap();
    private static Map<String, GenerateNoRuleContainer> ruleContainerCacheMap = new ConcurrentHashMap();

    private static final long WAITE_TIME = 1_000;
    private static final long LEASE_TIME = 10_000;

    public String getMaxNo(String code) throws Exception {
        if (StringUtils.isBlank(code)) {
            throw new BizException("code不能为空");
        }
        try {
            //获取该规则在当前环境条件下的编号池的纪元
            Long currentEpoch = generateCurrentEpoch(code);
            //尝试从缓存中获取编号
            String sequenceNoFromLocalCache = getSequenceNoFromLocalCache(code, currentEpoch);
            if (!StringUtils.isBlank(sequenceNoFromLocalCache)) {
                return sequenceNoFromLocalCache;
            }
            //创建新编号
            return createNo(code, currentEpoch);
        } catch (BizException e) {
            log.error("[SequenceNoGenerator.createMaxNo.error]业务处理异常", e);
            throw e;
        } catch (Exception e) {
            log.error("[SequenceNoGenerator.createMaxNo.error]未知异常", e);
            throw e;
        }
    }

    private Long generateCurrentEpoch(String code) {
        String content = getRuleContainer(code).getEpochRule().getContent();
        EpochRuleTypeEnum epochRuleTypeEnum = getRuleContainer(code).getEpochRule().getEpochRuleTypeEnum();
        Long epoch = null;
        switch (epochRuleTypeEnum) {
            case NONE:
                epoch = 0L;
                break;
            case DATE_TIME:
                epoch = Long.valueOf(DateUtil.format(new Date(), content));
                break;
            default:
                throw new BizException("不合法的EpochRuleTypeEnum,code为:" + code);
        }
        return epoch;
    }

    private GenerateNoRuleContainer getRuleContainer(String code) {
        GenerateNoRuleContainer ruleContainer = ruleContainerCacheMap.get(code);
        if (ruleContainer != null) {
            return ruleContainer;
        }
        CodeGeneratorCfgV2 cfg = codeGeneratorCfgMapper.selectByCode(code);
        if (cfg == null) {
            throw new BizException("通过code查找配置规则失败，code:" + code);
        }
        ruleContainer = buildGenerateNoRuleContainer(cfg);
        ruleContainerCacheMap.put(code, ruleContainer);
        return ruleContainer;
    }
    private GenerateNoRuleContainer buildGenerateNoRuleContainer(CodeGeneratorCfgV2 cfg) {
        Map<String, Object> map = JacksonUtil.toMap(cfg.getRule(), String.class, Object.class);
        GenerateNoRuleContainer container = new GenerateNoRuleContainer();
        container.setEpochRule(JacksonUtil.toBean(JacksonUtil.toJson(map.get("epochRule")), EpochRule.class));
        container.setRules(new ArrayList<>());
        List rules = JacksonUtil.toList(JacksonUtil.toJson(map.get("rules")),Object.class);
        for (Object rule : rules) {
            Map ruleMap = JacksonUtil.toMap(JacksonUtil.toJson(rule), String.class, Object.class);
            String ruleType = (String) ruleMap.get("ruleType");
            RuleTypeEnum ruleTypeEnum = RuleTypeEnum.getByCode(ruleType);
            if (ruleTypeEnum == null) {
                throw new BizException("不合法的ruleType:"+ruleType);
            }
            GenerateNoRule generateNoRule = new GenerateNoRule();
            generateNoRule.setRuleType(ruleTypeEnum);
            generateNoRule.setOrder((Integer) ruleMap.get("order"));
            GenerateNoRuleContent content = (GenerateNoRuleContent) JacksonUtil.toBean(JacksonUtil.toJson(ruleMap.get("ruleContent")), ruleTypeEnum.getRuleClass());
            generateNoRule.setRuleContent(content);
            container.getRules().add(generateNoRule);
        }
        container.setRules(container.getRules().stream().sorted().collect(Collectors.toList()));

        return container;
    }

    private List<GenerateNoRule> getRules(String code) {
        return getRuleContainer(code).getRules();
    }

    private String getSequenceNoFromLocalCache(String code, Long epoch) {
        String sequenceNo = null;
        Map<String, ConcurrentLinkedQueue<String>> queueMap = noCacheMap.get(code);
        if (queueMap != null) {
            ConcurrentLinkedQueue<String> sequenceNolist = queueMap.get(epoch.toString());
            if (sequenceNolist != null && !sequenceNolist.isEmpty()) {
                sequenceNo = sequenceNolist.poll();
            }
        }
        return sequenceNo;
    }
    private String createNo(String code,Long epoch) throws Exception {
        synchronized (this) {
            //进入同步块后，再次尝试从缓存中获取编号
            String sequenceNo = getSequenceNoFromLocalCache(code, epoch);
            if (StringUtils.isNotBlank(sequenceNo)) {
                return sequenceNo;
            }

            RLock lock = redissonRedDisLock.lock(sequenceNoLockKey + code);
            boolean isLocked = false;
            try {
                isLocked = lock.tryLock(WAITE_TIME, LEASE_TIME, TimeUnit.MILLISECONDS);
                if (!isLocked) {
                    log.warn("createNo failed,get lock failed");
                    throw new BizException("生成流水号失败");
                }
                //各种锁加上后，开始生成编号,这里开始会和数据库打交道，导致数据变更
                sequenceNo = generateSequenceNo(code);
            } catch (Exception e) {
                log.error("createNo exception:", e);
                noCacheMap.clear();
                throw new BizException("获取流水号失败");
            } finally {
                try {
                    lock.unlock();
                } catch (Exception e) {
                    log.warn("createNo,unlock failed,exception is:", e);
                }
            }

            return sequenceNo;
        }
    }

    private String generateSequenceNo(String code) {
        CodeGeneratorCfgV2 cfg = codeGeneratorCfgMapper.selectByCode(code);
        Long currentEpoch = generateCurrentEpoch(code);
        if (StringUtils.isBlank(cfg.getMaxValue()) || cfg.getEpoch() == null) {
            //如果cfg中没有，那么需要弄新的
            cfg.setEpoch(currentEpoch);
            cfg.setMaxValue("0");
        } else {
            //如果当前的纪元居然比cfg中的还要小，属于异常情况，可能由于当前机器时钟问题导致，因此使用最新的cfg中的数据
            if (currentEpoch.compareTo(cfg.getEpoch()) < 0) {
                currentEpoch = cfg.getEpoch();
            }
            //说明cfg中的数据已经过时了，需要抛弃，从头开始
            if (currentEpoch.compareTo(cfg.getEpoch()) > 0) {
                cfg.setEpoch(currentEpoch);
                cfg.setMaxValue("0");
            }
        }
        GenerateNoInfoContext generateNoInfoContext = GenerateNoInfoContext.builder()
                .code(code).cfg(cfg).epoch(currentEpoch)
                .date(new Date()).build();
        //不缓存的话，直接生成编号返回
        if (cfg.getIsCache() == 0) {
            return generateOneSequenceNo(generateNoInfoContext);
        }
        //缓存的话，生成一堆编号缓存后，从缓存中拿走一个编号返回
        ConcurrentLinkedQueue<String> cacheQueue = new ConcurrentLinkedQueue();
        ConcurrentHashMap<String, ConcurrentLinkedQueue<String>> queueMap = new ConcurrentHashMap();
        queueMap.put(currentEpoch.toString(), cacheQueue);
        noCacheMap.put(cfg.getCode(), queueMap);

        Integer cacheNum = cfg.getCacheNum();

        while (cacheNum > 0) {
            String newSequenceNo = generateOneSequenceNo(generateNoInfoContext);
            cacheQueue.offer(newSequenceNo);
            cacheNum--;
        }
        codeGeneratorCfgMapper.updateById(generateNoInfoContext.getCfg());
        return cacheQueue.poll();
    }


    private String generateOneSequenceNo(GenerateNoInfoContext generateNoInfoContext) {
        List<GenerateNoRule> rules = getRules(generateNoInfoContext.getCode());
        StringBuilder sb = new StringBuilder();
        for (GenerateNoRule rule : rules) {
            sb.append(generateFromRule(generateNoInfoContext, rule));
        }
        return sb.toString();
    }

    private String generateFromRule(GenerateNoInfoContext generateNoInfoContext, GenerateNoRule rule) {

        RuleTypeEnum ruleType = rule.getRuleType();
        String newSequenceNo = null;
        switch (ruleType) {
            case SEQ:
                newSequenceNo = generateFromSEQRule(generateNoInfoContext, rule);
                break;
            case DT:
                newSequenceNo = generateFromDTRule(generateNoInfoContext, rule);
                break;
            case FIX:
                newSequenceNo = generateFromFIXRule(generateNoInfoContext, rule);
                break;
            case RAN:
                newSequenceNo = generateFromRANRule(generateNoInfoContext, rule);
                break;
            default:
                throw new BizException("不合法的rule规则:" + ruleType);
        }
        return newSequenceNo;
    }

    private String generateFromSEQRule(GenerateNoInfoContext generateNoInfoContext, GenerateNoRule rule) {
        SequenceTypeRule sequenceTypeRule = (SequenceTypeRule) rule.getRuleContent();
        Long newMaxValue = Long.valueOf(generateNoInfoContext.getCfg().getMaxValue(), ((SequenceTypeRule) rule.getRuleContent()).getRadix()) +1;
        String newMaxValueStr = Long.toString(newMaxValue, sequenceTypeRule.getRadix());
        newMaxValueStr = StringUtils.leftPad(newMaxValueStr, sequenceTypeRule.getSize(), "0");
        generateNoInfoContext.getCfg().setMaxValue(newMaxValueStr);
        return newMaxValueStr;
    }


    private String generateFromDTRule(GenerateNoInfoContext generateNoInfoContext, GenerateNoRule rule) {
        DateTimeTypeRule dateTimeTypeRule = (DateTimeTypeRule) rule.getRuleContent();
        String dateFormatResult = generateNoInfoContext.getDateFormatResult();
        if (StringUtils.isBlank(dateFormatResult)) {
            dateFormatResult = DateUtil.format(generateNoInfoContext.getDate(), dateTimeTypeRule.getFormat());
            generateNoInfoContext.setDateFormatResult(dateFormatResult);
        }
        return dateFormatResult;
    }


    private String generateFromFIXRule(GenerateNoInfoContext generateNoInfoContext, GenerateNoRule rule) {
        FixedTypeRule fixedTypeRule = (FixedTypeRule) rule.getRuleContent();
        return fixedTypeRule.getContent();
    }

    private String generateFromRANRule(GenerateNoInfoContext generateNoInfoContext, GenerateNoRule rule) {
        RandomTypeRule randomTypeRule = (RandomTypeRule) rule.getRuleContent();
        List<String> poolList = randomTypeRule.getTypeList().stream()
                .map(RandomTypeEnum::getPool)
                .flatMap(List::stream)
                .collect(Collectors.toList());
        poolList.addAll(randomTypeRule.getCustomizePool());
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        int i = 1;
        while (randomTypeRule.getSize() >= i) {
            sb.append(poolList.get(random.nextInt(poolList.size())));
            i++;
        }
        return sb.toString();
    }

}
