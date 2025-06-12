package top.kdla.framework.supplement.trdcloud.cloud;

import lombok.*;

/**
 * @Author: alec
 * Description: 返回认证数据
 * @date: 下午1:58 2023/5/25
 */
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Builder
public class TrdPlatformAuthToken {

    private Long createTime;

    //必须是秒
    private String expirationTime;

    private String paramKey;

    private String paramName;

    private String paramValue;
    //TODO 没有用上。需要修改现有的赋值逻辑
    private Integer paramPosition;
}
