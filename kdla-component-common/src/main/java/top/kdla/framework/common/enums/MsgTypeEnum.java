/**
 * llkang.com Inc.
 * Copyright (c) 2010-2023 All Rights Reserved.
 */
package top.kdla.framework.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import top.kdla.framework.dto.IEnum;

/**
 * @author kanglele
 * @version $Id: MsgTypeEnum, v 0.1 2023/2/2 14:16 kanglele Exp $
 */
@AllArgsConstructor
@Getter
public enum MsgTypeEnum implements IEnum<String> {

    TEXT("text", "文本"),

    MARKDOWN("markdown", "markdown"),
    ;

    String code;
    String desc;
}
