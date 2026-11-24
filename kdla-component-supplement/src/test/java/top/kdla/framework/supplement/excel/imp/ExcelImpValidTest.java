package top.kdla.framework.supplement.excel.imp;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 安全审计 C-08/C-09 回归测试。
 *
 * <p>C-08：纯空白单元格绕过 NotNull/长度/正则三个校验注解。
 * 修复后 NotNull 拒绝 blank 字符串，长度与正则不再无条件跳过 blank。
 *
 * <p>C-09：长度违规误取 ExcelNotNullValid 的 message，单挂 @ExcelStrLengthValid 时 NPE。
 * 修复后取 ExcelStrLengthValid 自身的 message。
 *
 * <p>无修复则失败：旧代码空白值全部返回 ""，长度单挂注解抛 NPE。
 */
class ExcelImpValidTest {

    @Test
    void notNull_blankString_rejected() {
        NotNullRow row = new NotNullRow();
        row.name = "   ";
        assertEquals("姓名必填", ExcelImpValid.valid(row));
    }

    @Test
    void notNull_emptyString_rejected() {
        NotNullRow row = new NotNullRow();
        row.name = "";
        assertEquals("姓名必填", ExcelImpValid.valid(row));
    }

    @Test
    void notNull_null_rejected() {
        assertEquals("姓名必填", ExcelImpValid.valid(new NotNullRow()));
    }

    @Test
    void notNull_nonBlank_passes() {
        NotNullRow row = new NotNullRow();
        row.name = "张三";
        assertEquals("", ExcelImpValid.valid(row));
    }

    @Test
    void lengthOnly_overlong_usesLengthAnnotationMessage() {
        LengthRow row = new LengthRow();
        row.code = "abcdef";
        assertEquals("编码超长", ExcelImpValid.valid(row));
    }

    @Test
    void length_blankString_checkedAgainstLimit() {
        LengthRow row = new LengthRow();
        row.code = "   ";
        assertEquals("编码超长", ExcelImpValid.valid(row));
    }

    @Test
    void length_withinLimit_passes() {
        LengthRow row = new LengthRow();
        row.code = "ab";
        assertEquals("", ExcelImpValid.valid(row));
    }

    @Test
    void pattern_blankString_rejected() {
        PatternRow row = new PatternRow();
        row.mobile = "   ";
        assertEquals("手机号格式错误", ExcelImpValid.valid(row));
    }

    @Test
    void pattern_mismatch_rejected() {
        PatternRow row = new PatternRow();
        row.mobile = "abc";
        assertEquals("手机号格式错误", ExcelImpValid.valid(row));
    }

    @Test
    void pattern_match_passes() {
        PatternRow row = new PatternRow();
        row.mobile = "13800138000";
        assertEquals("", ExcelImpValid.valid(row));
    }

    private static class NotNullRow {
        @ExcelNotNullValid(message = "姓名必填")
        private String name;
    }

    private static class LengthRow {
        @ExcelStrLengthValid(length = 2, message = "编码超长")
        private String code;
    }

    private static class PatternRow {
        @ExcelPatternValid(regexp = "^1\\d{10}$", message = "手机号格式错误")
        private String mobile;
    }
}
