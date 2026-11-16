/**
 * llkang.com Inc.
 * Copyright (c) 2010-2023 All Rights Reserved.
 */
package top.kdla.framework.log;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.AsyncHandlerInterceptor;

/**
 * @author kanglele
 * @version $Id: LogInterceptorAutoConfigure, v 0.1 2023/8/18 14:52 kanglele Exp $
 */
@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(AsyncHandlerInterceptor.class)
@ConditionalOnProperty(name = "kdla.config.log-interceptor.enabled",
        havingValue = "true", matchIfMissing = false)
public class LogInterceptorAutoConfigure {

    @Bean
    @ConditionalOnMissingBean
    public LogInterceptor logInterceptor(){
        return new LogInterceptor();
    }

}
