/**
 * LY.com Inc.
 * Copyright (c) 2004-2017 All Rights Reserved.
 */
package top.kdla.framework.common.help;

import org.apache.commons.lang3.StringUtils;

/**
 * 证件脱敏工具
 *
 * @author zyy43688
 * @version $Id: CertificateUtil.java, v 0.1 2017年11月13日 下午5:36:03 zyy43688 Exp $
 */
public class CertificateHelp {
    /**
     * 脱敏
     *
     * @param certificateTypeCode 证件类型code
     * @param certificateCode     证件号
     */
    public static String coverCertificate(String certificateTypeCode, String certificateCode) {
        /*获取证件枚举*/
        CertificateTypeEnum certificateTypeEnum = CertificateTypeEnum.getCertificateTypeEnum(certificateTypeCode);

        /*证件脱敏*/
        switch (certificateTypeEnum) {

            case ID: {/* 身份证,保留前3位,后4位 */
            	return transfer(certificateCode, 3, 4);
            }
            case OFFICIAL_PASSPORTS:/* 公务护照,保留前1位,后1位 */
            case ORDINARY_PASSPORT:/* 普通护照,保留前1位,后1位  */
            case HONG_KONG_AND_MACAO_PASS:/* 港澳通行证,保留前1位,后1位  */
            case MTPs:/* 台胞证,保留前1位,后1位  */
            case HVPS:/* 回乡证,保留前1位,后1位  */
            case MILITARY_CARD:/* 军人证,保留前1位,后1位  */
            case SEAMAN_CERTIFICATE:/* 海员证,保留前1位,后1位  */
            case OTHER: {/*其他*/
            	return transfer(certificateCode, 1, 1);
            }
            default:
                return "";
        }
    }

    /**
	 * 证件脱敏
	 *
	 * @param certificateCode
	 * @return
	 */
	private static String transfer(String certificateCode, int startCharLength, int endCharLength) {
		int retainCharLength = startCharLength + endCharLength;

		if (certificateCode != null && certificateCode.length() >= retainCharLength) {
			StringBuffer sb = new StringBuffer(StringUtils.left(certificateCode, startCharLength));
			for (int i = 0; i < certificateCode.length() - retainCharLength; i++) {
				sb.append("*");
			}
			return sb.append(StringUtils.right(certificateCode, endCharLength)).toString();
		} else {
			return certificateCode;
		}
	}

    /**
     * 证件类型枚举
     */
    private enum CertificateTypeEnum {
                                      /*身份证*/
                                      ID("1", "身份证"),
                                      /*公务护照*/
                                      OFFICIAL_PASSPORTS("2", "公务护照"),
                                      /*普通护照*/
                                      ORDINARY_PASSPORT("3", "普通护照"),
                                      /*港澳通行证*/
                                      HONG_KONG_AND_MACAO_PASS("4", "港澳通行证"),
                                      /*台胞证*/
                                      MTPs("5", "台胞证"),
                                      /*回乡证*/
                                      HVPS("6", "回乡证"),
                                      /*军人证*/
                                      MILITARY_CARD("7", "军人证"),
                                      /*海员证*/
                                      SEAMAN_CERTIFICATE("8", "海员证"),
                                      /*其他*/
                                      OTHER("9", "其他");
        /*证件类型code*/
        private String certificateTypeCode;
        /*证件类型*/
        private String certificateType;

        CertificateTypeEnum(String certificateTypeCode, String certificateType) {
            this.certificateTypeCode = certificateTypeCode;
            this.certificateType = certificateType;
        }

        /**
         * 根据 certificateTypeCode 获取枚举
         *
         * @param certificateTypeCode 证件类型code
         * @return 如果code找到匹配值则返回匹配值，否则返回 CertificateTypeEnum.OTHER
         */
        public static CertificateTypeEnum getCertificateTypeEnum(String certificateTypeCode) {

            for (CertificateTypeEnum certificateTypeEnum : CertificateTypeEnum.values()) {
                if (certificateTypeEnum.certificateTypeCode.equals(certificateTypeCode)) {
                    return certificateTypeEnum;
                }
            }

            return CertificateTypeEnum.OTHER;
        }

        public String getCertificateTypeCode() {
            return certificateTypeCode;
        }

        public String getCertificateType() {
            return certificateType;
        }
    }
}