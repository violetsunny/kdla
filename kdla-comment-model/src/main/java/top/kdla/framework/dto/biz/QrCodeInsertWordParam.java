package top.kdla.framework.dto.biz;

import lombok.Data;

import java.io.Serializable;

/**
 * @author dongguo.tao
 * @description
 * @date 2021-12-22 09:48:58
 */
@Data
public class QrCodeInsertWordParam implements Serializable {

    private static final long serialVersionUID = 1659433598513802721L;
    /**
     * 条形码/二维码下方文字
     */
    private String word;

    /**
     * 文字高度
     */
    private Integer wordHeight;

}
