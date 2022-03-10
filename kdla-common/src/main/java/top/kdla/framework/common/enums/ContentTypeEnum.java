package top.kdla.framework.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.MediaType;

/**
 * 字符编码枚举
 * @author vincent.li
 */
@AllArgsConstructor
@Getter
public enum ContentTypeEnum {
    /**
     * APPLICATION_FORM_URLENCODED
     * */
    APPLICATION_FORM_URLENCODED(MediaType.APPLICATION_FORM_URLENCODED, MediaType.APPLICATION_FORM_URLENCODED_VALUE),
    /**
     * APPLICATION_FORM_URLENCODED
     * */
    APPLICATION_JSON( MediaType.APPLICATION_JSON, MediaType.APPLICATION_JSON_VALUE),
    /**
     * TEXT_PLAIN
     * */
    TEXT_PLAIN(MediaType.TEXT_PLAIN, MediaType.TEXT_PLAIN_VALUE),
    /**
     * TEXT_XML
     * */
    TEXT_XML(MediaType.TEXT_XML, MediaType.TEXT_XML_VALUE),
    /**
     * TEXT_HTML
     * */
    TEXT_HTML(MediaType.TEXT_HTML, MediaType.TEXT_HTML_VALUE);

    /**
     * 媒介类型
     * */
    private MediaType mediaType;
    /**
     * 值
     * */
    private String value;


}
