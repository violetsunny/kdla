/**
 * llkang.com Inc.
 * Copyright (c) 2010-2023 All Rights Reserved.
 */
package top.kdla.framework.common.aspect.mdc;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author kanglele
 * @version $Id: MdcAutoConfig, v 0.1 2023/8/18 10:19 kanglele Exp $
 */
@Configuration
public class MdcAutoConfigure {

    @Bean
    @ConditionalOnMissingBean
    public MdcAspect mdcAspect() {
        return new MdcAspect();
    }

}
