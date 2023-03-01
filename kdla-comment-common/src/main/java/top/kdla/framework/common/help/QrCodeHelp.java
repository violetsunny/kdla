package top.kdla.framework.common.help;

import com.alibaba.fastjson.JSON;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import top.kdla.framework.dto.biz.QrCodeCreateParam;
import top.kdla.framework.dto.biz.QrCodeInsertWordParam;
import top.kdla.framework.dto.biz.QrCodeTypeEnum;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 二维码&条形码操作工具
 *
 * @author dongguo.tao
 * @date 2021-12-21 10:48:20
 */
@Slf4j
public class QrCodeHelp {

    private static final String DEFAULT_SUFFIX = "png";

    private static final String DEFAULT_CHARSET = "UTF-8";


    /**
     * 生成条形码/二维码
     *
     * @param param 参数
     * @return
     */
    public static String createQRCode(QrCodeCreateParam param) {
        log.info("QrCodeUtil.createQRCode param={}", JSON.toJSONString(param));
        try {
            Map<EncodeHintType, Object> hintMap = createHintMap(4);
            BarcodeFormat type = getBarcodeFormat(param.getCodeType());
            String contents = new String(param.getCodeData().getBytes(DEFAULT_CHARSET), DEFAULT_CHARSET);
            //用Zxing生成二维码或者条形码
            BitMatrix matrix = new MultiFormatWriter().encode(contents, type, param.getWidth(), param.getHeight(), hintMap);
            BufferedImage image = MatrixToImageWriter.toBufferedImage(matrix);

            if (Objects.nonNull(param.getInsertWordParam())) {
                //如果需要增加文字描述，增需要用Graphics2D进行重新绘图
                QrCodeInsertWordParam wordParam = param.getInsertWordParam();
                Integer wordHeight = param.getHeight() + wordParam.getWordHeight();
                //新的图片，把带logo的二维码下面加上文字
                image = insertWords(image, wordParam.getWord(), param.getWidth(), param.getHeight(), wordHeight);
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(image, DEFAULT_SUFFIX, outputStream);
            //转换为base64
            Base64.Encoder encoder = Base64.getEncoder();
            String qrCodeImage = "data:image/jpeg;base64," + encoder.encodeToString(outputStream.toByteArray());
            return qrCodeImage;
        } catch (Exception e) {
            log.error("QrCodeUtil.createQRCode error. param={}", JSON.toJSONString(param), e);
            return null;
        }
    }

    /**
     * 把条形码/二维码下面加上文字
     *
     * @param image      码图片
     * @param words      文字
     * @param width      图片宽度
     * @param height     图片高度
     * @param wordHeight 文字高度
     * @return 返回BufferedImage
     */
    private static BufferedImage insertWords(BufferedImage image, String words, int width, int height, int wordHeight) {
        // 新的图片，把带logo的二维码下面加上文字
        if (StringUtils.isBlank(words)) {
            return image;
        }
        BufferedImage outImage = new BufferedImage(width, wordHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = outImage.createGraphics();
        // 抗锯齿
        setGraphics2D(g2d);
        // 设置白色
        setColorWhite(g2d);

        // 画条形码到新的面板
        g2d.drawImage(image, 0, 5, image.getWidth(), image.getHeight(), null);
        // 画文字到新的面板
        Color color = new Color(0, 0, 0);
        g2d.setColor(color);
        // 字体、字型、字号
        g2d.setFont(new Font("微软雅黑", Font.PLAIN, 16));
        //文字长度
        int strWidth = g2d.getFontMetrics().stringWidth(words);
        //总长度减去文字长度的一半  （居中显示）
        int wordStartX = (width - strWidth) / 2;
        //height + (outImage.getHeight() - height) / 2 + 12
        int wordStartY = height + 20;
        // 画文字
        g2d.drawString(words, wordStartX, wordStartY);
        g2d.dispose();
        outImage.flush();
        return outImage;

    }


    /**
     * 设置背景为白色
     *
     * @param g2d Graphics2D提供对几何形状、坐标转换、颜色管理和文本布局更为复杂的控制
     */
    private static void setColorWhite(Graphics2D g2d) {
        g2d.setColor(Color.WHITE);
        //填充整个屏幕
        g2d.fillRect(0, 0, 1000, 1000);
        //设置笔刷
        g2d.setColor(Color.BLACK);
    }


    /**
     * 设置 Graphics2D 属性  （抗锯齿）
     *
     * @param graphics2D
     */
    private static void setGraphics2D(Graphics2D graphics2D) {
        graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics2D.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_DEFAULT);
        Stroke s = new BasicStroke(1, BasicStroke.CAP_ROUND, BasicStroke.JOIN_MITER);
        graphics2D.setStroke(s);
    }


    /**
     * 参数处理，错误修正级别
     *
     * @param correctionLevel
     * @return
     */
    private static Map<EncodeHintType, Object> createHintMap(int correctionLevel) {
        Map<EncodeHintType, Object> hintMap = new HashMap<EncodeHintType, Object>();
        //空白填充
        hintMap.put(EncodeHintType.MARGIN, 1);
        if (correctionLevel == 2) {
            hintMap.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
        } else if (correctionLevel == 3) {
            hintMap.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.Q);
        } else if (correctionLevel == 4) {
            hintMap.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
        } else {
            hintMap.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.L);
        }
        return hintMap;
    }

    private static BarcodeFormat getBarcodeFormat(QrCodeTypeEnum codeType) {
        if (QrCodeTypeEnum.QR_CODE == codeType) {
            return BarcodeFormat.QR_CODE;
        } else if (QrCodeTypeEnum.CODE_128 == codeType) {
            return BarcodeFormat.CODE_128;
        } else {
            return BarcodeFormat.QR_CODE;
        }
    }

}
