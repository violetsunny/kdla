/**
 * llkang.com Inc.
 * Copyright (c) 2010-2023 All Rights Reserved.
 */
package top.kdla.framework.supplement.dingding.model.req;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import top.kdla.framework.dto.BaseModel;

import java.io.Serializable;
import java.util.List;

/**
 * @author kanglele
 * @version $Id: DingMessageAt, v 0.1 2023/2/2 14:19 kanglele Exp $
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DingMessageAt extends BaseModel {
    private List<String> atMobiles;
    private boolean isAtAll;
}
