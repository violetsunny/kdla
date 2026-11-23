package top.kdla.framework.infra.dal.mybatis.util;

import org.apache.commons.lang3.StringUtils;
import top.kdla.framework.exception.ExceptionFactory;

import java.util.regex.Pattern;

/**
 * SQL过滤
 *
 * @author haoxin
 * @date 2021-02-02
 **/
public class SqlFilter {

    /**
     * 排序列名白名单：仅允许字母、数字、下划线
     */
    private static final Pattern ORDER_COLUMN_PATTERN = Pattern.compile("^[a-zA-Z0-9_]+$");

    /**
     * 排序列名最大长度
     */
    private static final int MAX_COLUMN_LENGTH = 64;

    /**
     * SQL注入过滤
     * @param str  待验证的字符串
     */
    public static String sqlInject(String str){
        if(StringUtils.isBlank(str)){
            return null;
        }

        //白名单校验：仅允许字母数字下划线且长度<=64，
        //从根上拦截 union/注释/分号/引号等注入载荷，同时避免黑名单子串匹配误杀 update_time/is_deleted 等合法列名
        if (str.length() > MAX_COLUMN_LENGTH || !ORDER_COLUMN_PATTERN.matcher(str).matches()) {
            throw ExceptionFactory.buildBizException("包含非法字符");
        }

        //转换成小写
        return str.toLowerCase();
    }
}
