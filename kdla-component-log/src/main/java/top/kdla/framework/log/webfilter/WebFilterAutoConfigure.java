/**
 * llkang.com Inc.
 * Copyright (c) 2010-2023 All Rights Reserved.
 */
package top.kdla.framework.log.webfilter;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.servlet.Filter;

/**
 * @author kanglele
 * @version $Id: WebFilterAutoConfigure, v 0.1 2023/8/18 14:32 kanglele Exp $
 */
@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(Filter.class)
@ConditionalOnProperty(name = "kdla.config.web-filter.enabled",
        havingValue = "true", matchIfMissing = false)
public class WebFilterAutoConfigure {

    @Bean
    @ConditionalOnMissingBean(KdlaBodyReaderRequestFilter.class)
    public KdlaBodyReaderRequestFilter requestFilter(){
        return new KdlaBodyReaderRequestFilter();
    }

}
