package top.kdla.framework.common.help;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 回归测试：convertToCamelCase 的快速检查曾写反为 !"_".contains(name)，
 * 导致含下划线的 "HELLO_WORLD" 走了"不含下划线"分支被原样返回。
 * 修复后应按 javadoc 约定转换为 HelloWorld。
 */
class KdlaStringHelpCamelCaseTest {

    @Test
    void convertToCamelCase_withUnderscore_converts() {
        assertEquals("HelloWorld", KdlaStringHelp.convertToCamelCase("HELLO_WORLD"),
                "HELLO_WORLD 必须转换为 HelloWorld（javadoc 约定）");
    }

    @Test
    void convertToCamelCase_withoutUnderscore_onlyCapitalizesFirstChar() {
        assertEquals("Hello", KdlaStringHelp.convertToCamelCase("hello"),
                "不含下划线时仅首字母大写，修复不得破坏该分支");
    }
}
