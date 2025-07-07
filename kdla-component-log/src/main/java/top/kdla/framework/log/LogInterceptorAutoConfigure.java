/**
 * llkang.com Inc.
 * Copyright (c) 2010-2023 All Rights Reserved.
 */
package top.kdla.framework.log;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author kanglele
 * @version $Id: LogInterceptorAutoConfigure, v 0.1 2023/8/18 14:52 kanglele Exp $
 */
@Configuration
@Slf4j
public class LogInterceptorAutoConfigure {

    @Bean
    @ConditionalOnMissingBean
    public LogInterceptor logInterceptor(){
        if (log.isInfoEnabled()) {
            log.info("LogInterceptor init Bean");
        }
        return new LogInterceptor();
    }

}
