/**
 * llkang.com Inc.
 * Copyright (c) 2010-2023 All Rights Reserved.
 */
package top.kdla.framework.supplement.vertx;

import io.vertx.core.Vertx;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 统一提供 Vertx 实例，避免多个配置类各自注册同名 bean 导致 Spring Boot 3 启动冲突。
 *
 * @author kanglele
 * @version $Id: VertxConfigure, v 0.1 2023/5/18 kanglele Exp $
 */
@Configuration
@ConditionalOnClass(Vertx.class)
@ConditionalOnProperty(name = "kdla.config.vertx.enabled", havingValue = "true", matchIfMissing = true)
public class VertxConfigure {

    @Bean
    @ConditionalOnMissingBean(Vertx.class)
    public Vertx vertx() {
        return Vertx.vertx();
    }

}
