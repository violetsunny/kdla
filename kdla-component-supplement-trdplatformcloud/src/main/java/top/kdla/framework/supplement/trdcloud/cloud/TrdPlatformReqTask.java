/**
 * llkang.com Inc.
 * Copyright (c) 2010-2024 All Rights Reserved.
 */
package top.kdla.framework.supplement.trdcloud.cloud;

import lombok.*;

import java.util.List;

/**
 * @author kanglele
 * @version $Id: TrdPlatformReq, v 0.1 2024/3/13 18:02 kanglele Exp $
 */
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Builder
public class TrdPlatformReqTask {

    private String code;

    private Long apiId;

    private Long authApi;

    private Integer apiType;

    private List<TrdPlatformBody> bodies;

    private Double limit;

}
