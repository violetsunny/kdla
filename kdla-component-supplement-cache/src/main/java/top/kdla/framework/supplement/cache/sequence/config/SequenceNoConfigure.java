package top.kdla.framework.supplement.cache.sequence.config;

import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import top.kdla.framework.supplement.cache.lock.KRedissonRedDisLock;
import top.kdla.framework.supplement.cache.lock.config.KRedissonAutoConfigure;
import top.kdla.framework.supplement.cache.sequence.SequenceNoGenerator;
import top.kdla.framework.supplement.cache.sequence.SequenceNoGeneratorV2;
import top.kdla.framework.supplement.cache.sequence.mapper.CodeGeneratorCfgMapper;
import top.kdla.framework.supplement.cache.sequence.mapper.CodeGeneratorCfgV2Mapper;

import jakarta.annotation.Resource;

/**
 * @author kll
 * @date 2022/1/11
 */
@Slf4j
@Configuration
@MapperScan(basePackages = "top.kdla.framework.supplement.cache.sequence")
@ConditionalOnExpression(
        "${top.kdla.framework.supplement.cache.sequence.enable:false} or " +
        "${top.kdla.framework.supplement.sequence.enable:false} or " +
        "${kdla.sequence.enabled:false}"
)
@ConditionalOnClass({RedissonClient.class})
@AutoConfigureAfter(KRedissonAutoConfigure.class)
@ConditionalOnBean(KRedissonRedDisLock.class)
public class SequenceNoConfigure {

    @Value("${kdla.sequence.lock.key.prefix:${kdla.sequence.lock-key-prefix:top::sequenceNo::}}")
    private String sequenceNoLockKey;
    @Resource
    private CodeGeneratorCfgMapper codeGeneratorCfgMapper;
    @Resource
    private CodeGeneratorCfgV2Mapper codeGeneratorCfgV2Mapper;
    @Resource
    private KRedissonRedDisLock redissonRedDisLock;

    @Bean
    @ConditionalOnMissingBean(SequenceNoGenerator.class)
    public SequenceNoGenerator sequenceNoGenerator() {
        SequenceNoGenerator sequenceNoGenerator = new SequenceNoGenerator();
        sequenceNoGenerator.setCodeGeneratorCfgMapper(codeGeneratorCfgMapper);
        sequenceNoGenerator.setRedissonClient(redissonRedDisLock);
        sequenceNoGenerator.setSequenceNoLockKey(sequenceNoLockKey);
        return sequenceNoGenerator;
    }

    @Bean
    @ConditionalOnMissingBean(SequenceNoGeneratorV2.class)
    public SequenceNoGeneratorV2 sequenceNoGeneratorV2() {
        SequenceNoGeneratorV2 generatorV2 = new SequenceNoGeneratorV2();
        generatorV2.setCodeGeneratorCfgMapper(codeGeneratorCfgV2Mapper);
        generatorV2.setRedissonClient(redissonRedDisLock);
        generatorV2.setSequenceNoLockKey(sequenceNoLockKey);
        return generatorV2;
    }


}
