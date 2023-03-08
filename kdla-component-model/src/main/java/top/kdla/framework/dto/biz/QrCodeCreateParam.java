package top.kdla.framework.dto.biz;

import lombok.Data;

import java.io.Serializable;

/**
 * @author dongguo.tao
 * @description
 * @date 2021-12-22 09:48:58
 */
@Data
public class QrCodeCreateParam implements Serializable {

    private static final long serialVersionUID = 7261117321811210548L;

    /**
     * 码类型
     */
    private QrCodeTypeEnum codeType;

    /**
     * 码内容
     */
    private String codeData;

    /**
     * 码高度
     */
    private Integer height;

    /**
     * 码宽度
     */
    private Integer width;

    /**
     * 图片需要加的文字内容
     */
    private QrCodeInsertWordParam insertWordParam;
}
