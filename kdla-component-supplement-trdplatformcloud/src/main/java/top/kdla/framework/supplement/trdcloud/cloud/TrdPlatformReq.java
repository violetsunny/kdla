/**
 * llkang.com Inc.
 * Copyright (c) 2010-2024 All Rights Reserved.
 */
package top.kdla.framework.supplement.trdcloud.cloud;

import com.fasterxml.jackson.annotation.JsonProperty;
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
public class TrdPlatformReq {
    @JsonProperty("pcode")
    private String pCode;

    private List<TrdPlatformReqTask> reqChildren;

    private String productId;

}
