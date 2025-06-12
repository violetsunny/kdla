/**
 * llkang.com Inc.
 * Copyright (c) 2010-2024 All Rights Reserved.
 */
package top.kdla.framework.supplement.trdcloud.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author kanglele
 * @version $Id: TrdPlatformEnum, v 0.1 2024/3/15 10:41 kanglele Exp $
 */
@Getter
@AllArgsConstructor
public enum TrdPlatformEnum {

    ADD("添加", 1),

    UPDATE("修改", 2),

    REMOVE("删除", 3);

    final String name;
    final int code;

    @Getter
    @AllArgsConstructor
    public enum ApiTypeEnum {
        AUTH("认证", 1),

        DATA("数据", 2),

        PAGE("分页", 3);

        final String name;
        final int code;
    }

    @Getter
    @AllArgsConstructor
    public enum AuthWayEnum {

        NO("无", 1),

        TOKEN("TOKEN", 2);

        final String name;
        final int code;
    }

    @Getter
    @AllArgsConstructor
    public enum BodyParsingMethodEnum {

        NO("无", 1),

        JSON("jsonPath", 2),

        GROOVY("groovy脚本", 3),

        PROTOCOL("协议解析", 4);

        final String name;
        final int code;

    }

    @Getter
    @AllArgsConstructor
    public enum FunctionTypeEnum {

        UP("上数", 1),

        DOWN("下控", 2),
        ;

        final String name;
        final int code;

    }

    @Getter
    @AllArgsConstructor
    public enum HttpMethodEnum {

        GET("GET", 1),

        POST("POST", 2);

        final String name;
        final int code;

        public static Integer getCode(String name) {
            for (HttpMethodEnum e : HttpMethodEnum.values()) {
                if (e.getName().equals(name)) {
                    return e.getCode();
                }
            }
            return null;
        }

        public static String getName(int code) {
            for (HttpMethodEnum e : HttpMethodEnum.values()) {
                if (e.getCode()==code) {
                    return e.getName();
                }
            }
            return null;
        }

    }

    @Getter
    @AllArgsConstructor
    public enum ParamPositionEnum {

        HEAD("Head", 1),

        QUERY("Query", 2),

        PATH("Path", 3),

        FORM("Form表单", 4),

        BODY("Body", 5);

        final String name;

        final int code;

        public static ParamPositionEnum getParamPositionByCode(int code) {
            for (ParamPositionEnum e : ParamPositionEnum.values()) {
                if (e.getCode() == code) {
                    return e;
                }
            }
            return null;
        }

        public static String getName(Integer code) {
            for (ParamPositionEnum e : ParamPositionEnum.values()) {
                if (e.getCode() == code) {
                    return e.getName();
                }
            }
            return null;
        }

    }

    @Getter
    @AllArgsConstructor
    public enum ParamTypeEnum {

        NO("无", 1),

        FIXED("固定值", 2),

        MAPPING_PASS("参数替换", 3),

        GROOVY("动态groovy脚本", 4),

        SPECIAL_PASS("特殊参数替换", 5);

        final String name;
        final int code;

    }

    @Getter
    @AllArgsConstructor
    public enum TotalDataGetWayEnum {

        FIXED("固定值", 1),

        ORIGINAL_API("原始接口获取", 2),

        NEW_API("单独接口获取", 3);

        final String name;
        final int code;

    }

    @Getter
    @AllArgsConstructor
    public enum TaskStatusEnum {

        NO_START("未启动", 1),

        START("启动", 2),

        PAUSE("暂停", 3),
        ;

        final String name;

        final int code;

    }
}
