package top.kdla.framework.dto.biz;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import top.kdla.framework.dto.ErrorCodeI;

/**
 * @author dongguo.tao
 * @description
 * @date 2021-12-21 10:51:19
 */
@AllArgsConstructor
@NoArgsConstructor
public enum QrCodeTypeEnum implements ErrorCodeI {
    /**
     * 二维码
     */
    QR_CODE("QR_CODE","二维码"),

    /**
     * 条形码
     */
    CODE_128("CODE_128","条形码");

    @Getter
    private String code;

    @Getter
    private String msg;
}
