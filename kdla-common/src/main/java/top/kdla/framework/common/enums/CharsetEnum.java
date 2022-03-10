package top.kdla.framework.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * 字符编码枚举
 * @author vincent.li
 */
@AllArgsConstructor
@Getter
public enum CharsetEnum {
    /**
     * 编码UTF_8
     * */
    UTF8(StandardCharsets.UTF_8.name(), StandardCharsets.UTF_8),
    /**
     * 编码GBK
     * */
    GBK("GBK", Charset.forName("GBK")),
    /**
     * 编码ISO_8859_1
     * */
    ISO(StandardCharsets.ISO_8859_1.name(), StandardCharsets.ISO_8859_1);
    /**
     * 编码
     * */
    private String name;
    /**
     * 字符类型
     * */
    private Charset charset;

}
