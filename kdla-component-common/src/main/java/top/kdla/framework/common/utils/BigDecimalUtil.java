package top.kdla.framework.common.utils;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * BigDecimalUtils filter
 *
 * @author kll
 */
public class BigDecimalUtil {

    private BigDecimalUtil() {
    }

    /**
     * add --> filter.add
     *
     * @param bd1
     * @param bd2
     * @return
     */
    public static BigDecimal add(BigDecimal bd1, BigDecimal bd2) {
        return filter(bd1).add(filter(bd2));
    }

    /**
     * divide --> filter.divide
     *
     * @param bd1
     * @param bd2
     * @return
     */
    public static BigDecimal divide(BigDecimal bd1, BigDecimal bd2) {
        if (BigDecimal.ZERO.equals(filter(bd1)) || BigDecimal.ZERO.equals(filter(bd2))) {
            return BigDecimal.ZERO;
        }
        return filter(bd1).divide(filter(bd2), 4, BigDecimal.ROUND_HALF_UP);
    }

    /**
     * toBigDecimal, null --> 0
     *
     * @param val
     * @return new
     */
    public static BigDecimal filter(String val) {
        return null == val || val.trim().length() == 0 ? new BigDecimal("0") : new BigDecimal(val);
    }

    public static BigDecimal filter(BigDecimal val) {
        return null == val ? new BigDecimal(0) : val;
    }

    public static BigDecimal filter(Integer val) {
        return null == val ? new BigDecimal(0) : new BigDecimal(val);
    }

    public static BigDecimal filter(BigInteger val) {
        return null == val ? new BigDecimal(0) : new BigDecimal(val);
    }

    public static BigDecimal filter(Double val) {
        return null == val ? new BigDecimal(0) : new BigDecimal(Double.toString(val));
    }

    public static BigDecimal filter(Float val) {
        return null == val ? new BigDecimal(0) : new BigDecimal(Float.toString(val));
    }

    public static BigDecimal filter(Long val) {
        return null == val ? new BigDecimal(0) : BigDecimal.valueOf(val);
    }

    /**
     * default --> ROUND_HALF_UP 四舍五入
     *
     * @param val
     * @param scale 指定后几位
     * @return
     */
    public static BigDecimal filterScale(BigDecimal val,int scale) {
        return null == val ? new BigDecimal(0) : val.setScale(scale, BigDecimal.ROUND_HALF_UP);
    }

    /**
     * 小于0
     *
     * @param val
     * @return
     */
    public static Boolean lt0(BigDecimal val) {
        return BigDecimal.ZERO.compareTo(filter(val)) > 0;
    }

    /**
     * 大于0
     *
     * @param val
     * @return
     */
    public static Boolean gt0(BigDecimal val) {
        return BigDecimal.ZERO.compareTo(filter(val)) < 0;
    }

    /**
     * 等于0
     *
     * @param val
     * @return
     */
    public static Boolean eq0(BigDecimal val) {
        return BigDecimal.ZERO.compareTo(filter(val)) == 0;
    }

    /**
     * 不等于0
     *
     * @param val
     * @return
     */
    public static Boolean ne0(BigDecimal val) {
        return BigDecimal.ZERO.compareTo(filter(val)) != 0;
    }

    /**
     * 大于等于0
     *
     * @param val
     * @return
     */
    public static Boolean ge0(BigDecimal val) {
        return BigDecimal.ZERO.compareTo(filter(val)) <= 0;
    }

    /**
     * 小于等于0
     *
     * @param val
     * @return
     */
    public static Boolean le0(BigDecimal val) {
        return BigDecimal.ZERO.compareTo(filter(val)) >= 0;
    }
}
