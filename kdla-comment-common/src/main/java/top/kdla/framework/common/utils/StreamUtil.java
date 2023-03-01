package top.kdla.framework.common.utils;

import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * The type Stream utils.
 *
 * @author zjx18216
 * @version Id : StreamUtils, v 0.1 2018/3/13 17:31 zjx18216 Exp $
 */
public class StreamUtil {
    /**
     * Map list.
     *
     * @param <T>       the type parameter
     * @param data      the data
     * @param predicate the predicate
     * @return the list
     */
    public static <T> T filter(List<T> data, Predicate<T> predicate) {
        return data.stream().filter(predicate).findFirst().orElse(null);
    }

    /**
     * Filter list list.
     *
     * @param <T>       the type parameter
     * @param data      the data
     * @param predicate the predicate
     * @return the list
     */
    public static <T> List<T> filterList(List<T> data, Predicate<T> predicate) {
        return data.stream().filter(predicate).collect(Collectors.toList());
    }

    /**
     * Sort list.
     *
     * @param <T>        the type parameter
     * @param data       the data
     * @param comparator the comparator
     * @param limit      the limit
     * @return the list
     */
    public static <T> List<T> sort(List<T> data, Comparator<T> comparator, int limit) {
        return data.stream().sorted(comparator).limit(limit).collect(Collectors.toList());
    }

    /**
     * Find first t.
     *
     * @param <T>        the type parameter
     * @param data       the data
     * @param comparator the comparator
     * @return the t
     */
    public static <T> T findFirst(List<T> data, Comparator<T> comparator) {
        return data.stream().sorted(comparator).findFirst().orElse(null);
    }

    /**
     * Sort list.
     *
     * @param <T>        the type parameter
     * @param data       the data
     * @param comparator the comparator
     * @return the list
     */
    public static <T> List<T> sort(List<T> data, Comparator<T> comparator) {
        return data.stream().sorted(comparator).collect(Collectors.toList());
    }

    /**
     * Ternary res t.
     *
     * @param <T>   the type parameter
     * @param state the state
     * @param obj   the obj
     * @return the t
     */
    public static <T> T ternaryRes(boolean state, T obj) {
        return state ? obj : null;
    }

    /**
     * Map list.
     * 从对象列表中,按照给定的规则,提取对象中指定的属性,并以列表返回.
     *
     * @param <T>      the type parameter
     * @param <R>      the type parameter
     * @param list     the list
     * @param function the function
     * @return the list
     */
    public static <T, R> List<R> map(List<T> list, Function<T, R> function) {
        return list.stream().map(function).collect(Collectors.toList());
    }
}