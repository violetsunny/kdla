package top.kdla.framework.supplement.cache.sequence.config;

import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import top.kdla.framework.supplement.cache.lock.RedissonRedDisLock;
import top.kdla.framework.supplement.cache.sequence.SequenceNoGenerator;
import top.kdla.framework.supplement.cache.sequence.SequenceNoGeneratorV2;
import top.kdla.framework.supplement.cache.sequence.mapper.CodeGeneratorCfgMapper;
import top.kdla.framework.supplement.cache.sequence.mapper.CodeGeneratorCfgV2Mapper;

/**
 * @author kll
 * @date 2022/1/11
 */
@Slf4j
@Configuration
@MapperScan(basePackages = "top.kdla.framework.supplement.cache.sequence")
@ConditionalOnProperty(name = "top.kdla.framework.supplement.cache.sequence.enable", havingValue = "true")
@ConditionalOnClass({RedissonRedDisLock.class})
public class SequenceNoConfigure {

    @Value("${kdla.sequence.lock.key.prefix:top::sequenceNo::}")
    private String sequenceNoLockKey;

    @Bean
    public SequenceNoGenerator sequenceNoGenerator(@Autowired CodeGeneratorCfgMapper codeGeneratorCfgMapper,@Autowired RedissonRedDisLock redissonRedDisLock) {
        SequenceNoGenerator sequenceNoGenerator = new SequenceNoGenerator();
        sequenceNoGenerator.setCodeGeneratorCfgMapper(codeGeneratorCfgMapper);
        sequenceNoGenerator.setRedissonClient(redissonRedDisLock);
        sequenceNoGenerator.setSequenceNoLockKey(sequenceNoLockKey);

        log.info("sequenceNoGenerator injeted...");
        return sequenceNoGenerator;
    }

    @Bean
    public SequenceNoGeneratorV2 sequenceNoGeneratorV2(@Autowired CodeGeneratorCfgV2Mapper codeGeneratorCfgMapper,@Autowired RedissonRedDisLock redissonRedDisLock) {
        SequenceNoGeneratorV2 generatorV2 = new SequenceNoGeneratorV2();
        generatorV2.setCodeGeneratorCfgMapper(codeGeneratorCfgMapper);
        generatorV2.setRedissonClient(redissonRedDisLock);
        generatorV2.setSequenceNoLockKey(sequenceNoLockKey);

        log.info("generatorV2 injeted...");
        return generatorV2;
    }

}
