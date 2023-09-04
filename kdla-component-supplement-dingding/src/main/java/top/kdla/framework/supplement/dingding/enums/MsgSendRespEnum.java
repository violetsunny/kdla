/**
 * llkang.com Inc.
 * Copyright (c) 2010-2023 All Rights Reserved.
 */
package top.kdla.framework.supplement.dingding.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import top.kdla.framework.dto.IEnum;

/**
 * @author kanglele
 * @version $Id: MsgSendRespEnum, v 0.1 2023/2/2 14:27 kanglele Exp $
 */
@AllArgsConstructor
@Getter
public enum MsgSendRespEnum implements IEnum<String> {
    OK("ok", "成功"),
    NO_NEED("no_need", "无需发送"),
    FAIL("fail", "失败");

    private final String code;
    private final String desc;
}
