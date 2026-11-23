package top.kdla.framework.infra.dal.mybatis.util;

import org.junit.jupiter.api.Test;
import top.kdla.framework.exception.BizException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 回归测试：sqlInject 曾用黑名单子串匹配（update/delete/select等关键词），
 * 误杀 update_time、is_deleted 等合法列名，且未防 union、注释、空格等注入载荷
 * （分号先被剥离再匹配，"id;drop table"剥离后仍可能漏防）。
 * 修复后改为白名单：仅允许 ^[a-zA-Z0-9_]+$ 且长度<=64，API签名与异常类型（BizException）保持不变。
 */
class SqlFilterWhitelistTest {

    @Test
    void legalColumnNames_pass() {
        // 修复前："update_time"含"update"、"is_deleted"含"delete"，均被误杀
        assertEquals("update_time", SqlFilter.sqlInject("update_time"),
                "合法列名update_time不得被误杀");
        assertEquals("is_deleted", SqlFilter.sqlInject("is_deleted"),
                "合法列名is_deleted不得被误杀");
        assertEquals("create_time", SqlFilter.sqlInject("create_time"));
    }

    @Test
    void injectionPayloads_rejected() {
        // 分号+DDL
        assertThrows(BizException.class, () -> SqlFilter.sqlInject("id;drop table"),
                "含分号/空格的注入载荷必须拒绝");
        // union注入（修复前黑名单没有union关键词，分号剥离后可能漏防）
        assertThrows(BizException.class, () -> SqlFilter.sqlInject("a union select"),
                "union注入载荷必须拒绝");
        // SQL注释
        assertThrows(BizException.class, () -> SqlFilter.sqlInject("id--"),
                "注释符必须拒绝");
        assertThrows(BizException.class, () -> SqlFilter.sqlInject("id/**/"),
                "块注释必须拒绝");
        // 引号与反斜杠
        assertThrows(BizException.class, () -> SqlFilter.sqlInject("id'"),
                "引号必须拒绝");
    }

    @Test
    void overLengthColumn_rejected() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 65; i++) {
            sb.append("a");
        }
        assertThrows(BizException.class, () -> SqlFilter.sqlInject(sb.toString()),
                "超过64字符的列名必须拒绝");
    }

    @Test
    void blankInput_returnsNull() {
        // 原有空白入参语义保持不变
        assertNull(SqlFilter.sqlInject(" "));
        assertNull(SqlFilter.sqlInject(null));
    }
}
