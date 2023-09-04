/**
 * llkang.com Inc.
 * Copyright (c) 2010-2023 All Rights Reserved.
 */
package top.kdla.framework.supplement.dingding.model.req;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import top.kdla.framework.dto.BaseModel;

import java.io.Serializable;

/**
 * @author kanglele
 * @version $Id: MarkdownMessage, v 0.1 2023/2/2 14:20 kanglele Exp $
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MarkdownMessage extends BaseModel {
    private String title;
    @JsonIgnore
    private String content;
    private String text;

    public MarkdownMessage(String title, String content) {
        this.title = title;
        this.content = content;
    }

    public String getText() {
        return "#### " + this.getTitle() + "\n> " + this.getContent() + " 请及时处理\n ";
    }
}
