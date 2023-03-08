/**
 * llkang.com Inc.
 * Copyright (c) 2010-2023 All Rights Reserved.
 */
package top.kdla.framework.supplement.dingding.properties;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author kanglele
 * @version $Id: DingAlertProperties, v 0.1 2023/2/2 14:08 kanglele Exp $
 */
@ConfigurationProperties(
        prefix = "kdfa.alert.ding"
)
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DingAlertProperties {
    private String accessToken;
    private String dingKeywords;
    private String appName;
    private boolean enable = true;
    private String env;
}
