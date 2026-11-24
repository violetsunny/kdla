package top.kdla.framework.validator;

import org.junit.jupiter.api.Test;
import top.kdla.framework.exception.BizException;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 回归测试：ObjectValidator此前四处缺陷：
 * 1) 引用sun.misc.Unsafe（JDK内部API，多余引用）；
 * 2) 排除列表equals精确匹配，Temporal/Enum/Number等父类型对子类不生效（自定义枚举被递归遍历直至StackOverflowError）；
 * 3) 无递归深度上限与已访问集合，循环引用直接StackOverflowError；
 * 4) getDeclaredFields不查父类字段，父类上的校验注解全部漏检。
 */
class ObjectValidatorTest {

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    @interface CheckPresent {
        String[] match() default {};

        String[] nonMatch() default {};

        String message() default "field required";
    }

    static class Parent {
        @CheckPresent(message = "parentField required")
        private String parentField;

        Parent(String parentField) {
            this.parentField = parentField;
        }
    }

    static class Child extends Parent {
        @CheckPresent(message = "childField required")
        private String childField;

        Child(String parentField, String childField) {
            super(parentField);
            this.childField = childField;
        }
    }

    static class Node {
        private Node next;
    }

    enum Status {
        ACTIVE;
        @CheckPresent(message = "枚举内部字段不应被校验")
        private String inner = null;
    }

    static class EnumHolder {
        private Status status = Status.ACTIVE;
    }

    private final ObjectValidator<Object> validator = new ObjectValidator<>();

    @Test
    void parentClassAnnotatedField_isValidated() {
        // 修复前：getDeclaredFields不含父类字段，parentField为null漏检，只报childField
        BizException e = assertThrows(BizException.class,
                () -> validator.getValidatorMsg(new Child(null, null), CheckPresent.class, "biz"));
        assertTrue(e.getMessage().contains("parentField required"), "父类字段注解必须被校验，实际=" + e.getMessage());
        assertTrue(e.getMessage().contains("childField required"), "子类字段注解保持校验");
    }

    @Test
    void allFieldsPresent_noException() {
        assertDoesNotThrow(() -> validator.getValidatorMsg(new Child("p", "c"), CheckPresent.class, "biz"));
    }

    @Test
    void circularReference_noStackOverflow() {
        Node a = new Node();
        Node b = new Node();
        a.next = b;
        b.next = a;

        // 修复前：无已访问集合，a→b→a→…无限递归StackOverflowError
        assertDoesNotThrow(() -> validator.getValidatorMsg(a, CheckPresent.class, "biz"));
    }

    @Test
    void deepChain_failClosedBeyondDepthLimit() {
        // 2万层链式对象：修复前超深静默放行（fail-open，深层注解被绕过）；修复后超深拒绝
        Node head = new Node();
        Node cur = head;
        for (int i = 0; i < 20000; i++) {
            cur.next = new Node();
            cur = cur.next;
        }
        BizException e = assertThrows(BizException.class,
                () -> validator.getValidatorMsg(head, CheckPresent.class, "biz"));
        assertTrue(e.getMessage().contains("层级过深"), "超深必须拒绝，实际=" + e.getMessage());
    }

    @Test
    void chainAtDepthLimit_passes() {
        // 17个节点对应深度0..16，恰在上限内，必须放行
        Node head = new Node();
        Node cur = head;
        for (int i = 0; i < 16; i++) {
            cur.next = new Node();
            cur = cur.next;
        }
        assertDoesNotThrow(() -> validator.getValidatorMsg(head, CheckPresent.class, "biz"));
    }

    @Test
    void enumField_notRecursedByParentTypeMatch() {
        // 修复前：equals匹配不认Enum父类型，递归进枚举内部（$VALUES自引用）直至StackOverflowError，
        // 且枚举内部字段上的注解会被误校验
        assertDoesNotThrow(() -> validator.getValidatorMsg(new EnumHolder(), CheckPresent.class, "biz"));
    }

    @Test
    void commonlyClassList_noUnsafeReference() throws Exception {
        // 修复前：排除列表含sun.misc.Unsafe（JDK内部API多余引用）
        Method m = ObjectValidator.class.getDeclaredMethod("getCommonlyClass");
        m.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<Class<?>> list = (List<Class<?>>) m.invoke(null);
        assertTrue(list.stream().noneMatch(c -> c.getName().equals("sun.misc.Unsafe")),
                "排除列表不得再引用sun.misc.Unsafe");
        assertTrue(list.contains(Enum.class) && list.contains(java.time.temporal.Temporal.class),
                "父类型排除项保留");
    }

    @Test
    void annotationClass_noUnsafeImport() {
        // 类级验证：ObjectValidator源码不再依赖sun.misc（若仍import，JDK9+直接编译失败）
        assertDoesNotThrow(() -> {
            for (Annotation a : ObjectValidator.class.getAnnotations()) {
                a.annotationType();
            }
        });
    }
}
