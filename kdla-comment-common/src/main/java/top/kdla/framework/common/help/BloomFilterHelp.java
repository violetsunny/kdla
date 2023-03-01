/**
 * kanglele Inc. Copyright (c) 2022 All Rights Reserved.
 */
package top.kdla.framework.common.help;

import java.nio.charset.Charset;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;

/**
 * 布隆过滤器
 * 
 * @author kanglele
 * @version $Id: BloomFilterUtils, v 0.1 2022/1/12 15:02 Exp $
 */
public class BloomFilterHelp {

    /**
     * 存储int值的布隆过滤器
     * 
     * @param insertions
     *            存储数据大小
     * @return
     */
    public static BloomFilter<Integer> createInt(int insertions) {
        return BloomFilter.create(Funnels.integerFunnel(), insertions << 1, 0.01);
    }

    /**
     * 存储long值的布隆过滤器
     *
     * @param insertions
     *            存储数据大小
     * @return
     */
    public static BloomFilter<Long> createLong(int insertions) {
        return BloomFilter.create(Funnels.longFunnel(), insertions << 1, 0.01);
    }

    /**
     * 存储String值的布隆过滤器
     * 
     * @param insertions
     *            存储数据大小
     * @return
     */
    public static BloomFilter<String> createStr(int insertions) {
        return BloomFilter.create(Funnels.stringFunnel(Charset.defaultCharset()), insertions << 1, 0.01);
    }

    /**
     * 是否存在
     * 
     * @param bloomFilter
     * @param oj
     * @return false 不存在
     */
    public static Boolean exist(BloomFilter bloomFilter, Object oj) {
        if (bloomFilter == null || oj == null) {
            return false;
        }
        return bloomFilter.mightContain(oj);
    }
}
