/**
 * llkang.com Inc.
 * Copyright (c) 2010-2023 All Rights Reserved.
 */
package top.kdla.framework.supplement.dingding.model.req;

import lombok.*;
import top.kdla.framework.dto.BaseModel;

/**
 * @author kanglele
 * @version $Id: TextMessage, v 0.1 2023/2/2 14:21 kanglele Exp $
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TextMessage extends BaseModel {
    private String content;
}
