/**
 * llkang.com Inc.
 * Copyright (c) 2010-2023 All Rights Reserved.
 */
package top.kdla.framework.supplement.dingding.model.res;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * @author kanglele
 * @version $Id: DingDingAlertResult, v 0.1 2023/2/2 14:24 kanglele Exp $
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DingDingAlertResult implements Serializable {
    private static final String SUCCEED = "0";
    private String errcode;
    private String errmsg;

    public boolean isSuccess() {
        return "0".equals(this.errcode);
    }
}
