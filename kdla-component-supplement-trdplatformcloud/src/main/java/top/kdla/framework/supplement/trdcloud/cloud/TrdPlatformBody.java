/**
 * llkang.com Inc.
 * Copyright (c) 2010-2024 All Rights Reserved.
 */
package top.kdla.framework.supplement.trdcloud.cloud;

import lombok.*;

import java.util.Map;

/**
 * @author kanglele
 * @version $Id: TrdPlatformReq, v 0.1 2024/3/13 18:02 kanglele Exp $
 */
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Builder
public class TrdPlatformBody {

    /**
     * 请求URL
     * */
    private String url;

    /**
     * 请求方法
     * */
    private String method;

    /**
     * 请求头参数
     * */
    private Map<String, String> header;

    /**
     * 请求体参数
     * */
    private Object body;

    private Integer bodyAnalysisType;

    private String bodyAnalysisCode;

}
