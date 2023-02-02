/**
 * llkang.com Inc.
 * Copyright (c) 2010-2023 All Rights Reserved.
 */
package top.kdla.framework.supplement.dingding.model.req;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * @author kanglele
 * @version $Id: DingDingMessage, v 0.1 2023/2/2 14:18 kanglele Exp $
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DingDingMessage implements Serializable {
    private String msgtype;
    private TextMessage text;
    private MarkdownMessage markdown;
    private DingMessageAt at;

    public DingDingMessage(String msgtype, TextMessage text, DingMessageAt at) {
        this.msgtype = msgtype;
        this.text = text;
        this.at = at;
    }

    public DingDingMessage(String msgtype, MarkdownMessage markdown, DingMessageAt at) {
        this.msgtype = msgtype;
        this.markdown = markdown;
        this.at = at;
    }
}
