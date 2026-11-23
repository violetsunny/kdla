/**
 * llkang.com Inc.
 * Copyright (c) 2010-2023 All Rights Reserved.
 */
package top.kdla.framework.common.aspect.mdc;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author kanglele
 * @version $Id: MdcAutoConfig, v 0.1 2023/8/18 10:19 kanglele Exp $
 */
@Configuration
@ConditionalOnClass(Aspect.class)
@ConditionalOnProperty(name = "kdla.config.mdc.enabled",
        havingValue = "true", matchIfMissing = true)
@Slf4j
public class MdcAutoConfigure {

    @Bean
    @ConditionalOnMissingBean
    public MdcAspect mdcAspect() {
        if (log.isInfoEnabled()) {
            log.info("MdcAspect init Bean");
        }
        return new MdcAspect();
    }

}
