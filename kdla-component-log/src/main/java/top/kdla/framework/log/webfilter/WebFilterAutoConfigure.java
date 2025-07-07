/**
 * llkang.com Inc.
 * Copyright (c) 2010-2023 All Rights Reserved.
 */
package top.kdla.framework.log.webfilter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author kanglele
 * @version $Id: WebFilterAutoConfigure, v 0.1 2023/8/18 14:32 kanglele Exp $
 */
@Configuration
@Slf4j
public class WebFilterAutoConfigure {

    @Bean
    public KdlaBodyReaderRequestFilter requestFilter() {
        if (log.isInfoEnabled()) {
            log.info("KdlaBodyReaderRequestFilter init Bean");
        }
        return new KdlaBodyReaderRequestFilter();
    }

}
