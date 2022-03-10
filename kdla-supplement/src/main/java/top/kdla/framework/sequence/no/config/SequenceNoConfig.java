package top.kdla.framework.sequence.no.config;

import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import top.kdla.framework.sequence.no.SequenceNoGeneratorV2;
import top.kdla.framework.sequence.no.mapper.CodeGeneratorCfgMapper;
import top.kdla.framework.sequence.no.SequenceNoGenerator;
import top.kdla.framework.sequence.no.mapper.CodeGeneratorCfgV2Mapper;

/**
 * @author hjs
 * @date 2022/1/11
 */
@Slf4j
@Configuration
@MapperScan(basePackages = "top.kdla.framework.sequence.no")
@ConditionalOnProperty(name = "top.kdla.framework.sequence.no.enable", havingValue = "true")
public class SequenceNoConfig {

    @Value("${top.kdla.framework.sequence.lock.key.prefix:enn::sequenceNo::}")
    private String sequenceNoLockKey;

    @Bean
    public SequenceNoGenerator sequenceNoGenerator(@Autowired CodeGeneratorCfgMapper codeGeneratorCfgMapper, @Autowired RedissonClient redissonClient) {
        SequenceNoGenerator sequenceNoGenerator = new SequenceNoGenerator();
        sequenceNoGenerator.setCodeGeneratorCfgMapper(codeGeneratorCfgMapper);
        sequenceNoGenerator.setRedissonClient(redissonClient);
        sequenceNoGenerator.setSequenceNoLockKey(sequenceNoLockKey);

        log.info("sequenceNoGenerator injeted...");
        return sequenceNoGenerator;
    }

    @Bean
    public SequenceNoGeneratorV2 sequenceNoGeneratorV2(@Autowired CodeGeneratorCfgV2Mapper codeGeneratorCfgMapper, @Autowired RedissonClient redissonClient) {
        SequenceNoGeneratorV2 generatorV2 = new SequenceNoGeneratorV2();
        generatorV2.setCodeGeneratorCfgMapper(codeGeneratorCfgMapper);
        generatorV2.setRedissonClient(redissonClient);
        generatorV2.setSequenceNoLockKey(sequenceNoLockKey);

        log.info("generatorV2 injeted...");
        return generatorV2;
    }

}
