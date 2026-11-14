package top.kdla.framework.common.help;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 回归测试：CH2PY 旧实现对空串入参会构造长度为 0 的 pinYinArray，
 * getPinYin 递归访问 pinYinArray[0] 抛 ArrayIndexOutOfBoundsException；null 入参直接 NPE。
 * 修复后入口判空返回空结果。
 */
class PinYinHelpTest {

    @Test
    void ch2py_emptyString_returnsEmptyList() {
        List<String> result = PinYinHelp.CH2PY("");
        assertTrue(result.isEmpty(), "空串入参必须返回空列表，不能抛 ArrayIndexOutOfBoundsException");
    }

    @Test
    void ch2py_null_returnsEmptyList() {
        List<String> result = PinYinHelp.CH2PY(null);
        assertTrue(result.isEmpty(), "null 入参必须返回空列表，不能抛 NPE");
    }

    @Test
    void ch2py_normalChinese_stillWorks() {
        List<String> result = PinYinHelp.CH2PY("中");
        assertTrue(result.contains("zhong"), "正常汉字转拼音功能不受影响");
    }
}
