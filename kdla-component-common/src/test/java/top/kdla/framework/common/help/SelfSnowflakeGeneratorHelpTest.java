package top.kdla.framework.common.help;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verifies that getAutoIncreaseNumber() produces no duplicate values under concurrent load.
 */
class SelfSnowflakeGeneratorHelpTest {

    private static final int THREAD_COUNT = 50;
    private static final int CALLS_PER_THREAD = 20; // 1000 total calls

    @Test
    void getAutoIncreaseNumber_noDuplicatesUnderConcurrentLoad() throws Exception {
        // Access the private static method via reflection
        Method method = SelfSnowflakeGeneratorHelp.class
                .getDeclaredMethod("getAutoIncreaseNumber");
        method.setAccessible(true);

        Set<Integer> results = ConcurrentHashMap.newKeySet();
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(THREAD_COUNT);

        ExecutorService pool = Executors.newFixedThreadPool(THREAD_COUNT);
        try {
            for (int i = 0; i < THREAD_COUNT; i++) {
                pool.submit(() -> {
                    try {
                        startLatch.await(); // all threads start simultaneously
                        for (int j = 0; j < CALLS_PER_THREAD; j++) {
                            int value = (int) method.invoke(null);
                            results.add(value);
                        }
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }
            startLatch.countDown(); // fire!
            doneLatch.await();
        } finally {
            pool.shutdown();
        }

        // Each call must produce a unique counter value in [1000, 10000)
        // 1000 total calls → exactly 1000 unique values (counter wraps but never repeats within a cycle)
        // Strictly: within 9000 possible values (1001..10000 cycling back to 1000),
        // 1000 concurrent calls must all be distinct.
        assertEquals(THREAD_COUNT * CALLS_PER_THREAD, results.size(),
                "Expected no duplicate counter values from 1000 concurrent calls");

        // All values must be in valid range [1000, 10000]
        for (int v : results) {
            assert v >= 1000 && v <= 10000 : "Counter value out of range: " + v;
        }
    }
}
