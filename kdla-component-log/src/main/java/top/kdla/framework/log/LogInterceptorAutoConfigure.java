/**
 * llkang.com Inc.
 * Copyright (c) 2010-2023 All Rights Reserved.
 */
package top.kdla.framework.log;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author kanglele
 * @version $Id: LogInterceptorAutoConfigure, v 0.1 2023/8/18 14:52 kanglele Exp $
 */
@Configuration
public class LogInterceptorAutoConfigure {

    @Bean
    @ConditionalOnMissingBean
    public LogInterceptor logInterceptor(){
        return new LogInterceptor();
    }

}
