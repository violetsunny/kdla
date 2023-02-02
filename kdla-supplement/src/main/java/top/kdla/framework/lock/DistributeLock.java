/**
 * kll Inc.
 * Copyright (c) 2021 All Rights Reserved.
 */
package top.kdla.framework.lock;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

/**
 * @author kll
 * @version $Id: DistributeLock, v 0.1 2021/7/13 9:42 Exp $
 */
public interface DistributeLock {
    Lock lock(String var1) throws Exception;

    Lock lock(String var1, long var2) throws Exception;

    Lock lock(String var1, TimeUnit var2, long var3) throws Exception;

    Lock lock(String var1, TimeUnit var2, long var3, long var5) throws Exception;

    boolean tryLock(String var1, TimeUnit var2, long var3, long var5) throws Exception;

    void unlock(String var1);

    void unlock(Lock var1);
}
