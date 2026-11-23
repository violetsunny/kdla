/**
 * llkang.com Inc.
 * Copyright (c) 2010-2023 All Rights Reserved.
 */
package top.kdla.framework.supplement.dingding.config;

import io.vertx.core.Vertx;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import top.kdla.framework.supplement.dingding.properties.DingAlertProperties;

import jakarta.annotation.Resource;

/**
 * @author kanglele
 * @version $Id: DingAlertConfig, v 0.1 2023/2/2 14:06 kanglele Exp $
 */
@Configuration
@ConditionalOnClass(Vertx.class)
@EnableConfigurationProperties({DingAlertProperties.class})
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DingAlertConfigure {
    @Resource
    private DingAlertProperties dingAlertProperties;
    private String baseDingUrl = "https://oapi.dingtalk.com/robot/send?access_token=%s";

    public String getDingUrl() {
        return String.format(baseDingUrl, dingAlertProperties.getAccessToken());
    }

    public boolean isDingAlertEnable() {
        return dingAlertProperties.isEnable();
    }

    public String getEnv() {
        return dingAlertProperties.getEnv();
    }
}
