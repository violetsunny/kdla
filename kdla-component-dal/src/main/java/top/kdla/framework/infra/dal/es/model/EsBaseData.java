/**
 * llkang.com Inc.
 * Copyright (c) 2010-2023 All Rights Reserved.
 */
package top.kdla.framework.infra.dal.es.model;

import lombok.Data;

import java.io.Serializable;

/**
 * es基础
 *
 * @author kanglele
 * @version $Id: EsBaseData, v 0.1 2023/1/13 15:01 kanglele Exp $
 */
@Data
public class EsBaseData implements ElasticIndex {

    private String id;

    private String index;

}
