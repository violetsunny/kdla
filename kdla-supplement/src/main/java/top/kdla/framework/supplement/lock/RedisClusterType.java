/**
 * kll Inc.
 * Copyright (c) 2021 All Rights Reserved.
 */
package top.kdla.framework.supplement.lock;

import lombok.Getter;

/**
 * @author kll
 * @version $Id: RedisClusterType, v 0.1 2021/7/13 9:49 Exp $
 */
@Getter
public enum RedisClusterType {
    SINGLE,
    MASTERSLAVE,
    SENTINEL,
    CLUSTER,
    REPLICATE,
    ;

    private RedisClusterType(){}
}
