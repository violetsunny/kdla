/**
 * llkang.com Inc.
 * Copyright (c) 2010-2023 All Rights Reserved.
 */
package top.kdla.framework.supplement.cache.lock;

import org.redisson.api.RLock;

/**
 * @author kanglele
 * @version $Id: DistributeLockFactory, v 0.1 2023/2/28 14:36 kanglele Exp $
 */
public interface DistributeLockFactory {

    /**
     * 获取锁
     * @param key
     * @return
     */
    RLock getLock(String key);

}
