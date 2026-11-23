package top.kdla.framework.common.aspect.mdc;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.MDC;
import org.springframework.asm.ClassReader;
import org.springframework.asm.ClassVisitor;
import org.springframework.asm.Label;
import org.springframework.asm.MethodVisitor;
import org.springframework.asm.Opcodes;
import org.springframework.asm.Type;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.expression.BeanFactoryResolver;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import top.kdla.framework.common.help.MdcHelp;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * @MdcDot(bizCode = "#req.articleId")
 * @author YiHui
 * @date 2023/5/26
 */
@Slf4j
@Aspect
public class MdcAspect implements ApplicationContextAware {
    private static final String BIZ_CODE_KEY = "bizCode";

    private ExpressionParser parser = new SpelExpressionParser();
    private ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    @Pointcut("@annotation(MdcDot) || @within(MdcDot)")
    public void getLogAnnotation() {
        if (log.isDebugEnabled()) {
            log.debug("--- MdcAspect start ---");
        }
    }

    @Around("getLogAnnotation()")
    public Object handle(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        // 覆盖前保存 bizCode 旧值，finally 精确恢复而非 MDC.clear() 整体清空，嵌套 @MdcDot 时外层上下文不丢
        String oldBizCode = MDC.get(BIZ_CODE_KEY);
        boolean hasTag = addMdcCode(joinPoint);
        try {
            return joinPoint.proceed();
        } finally {
            if (log.isInfoEnabled()) {
                log.info("方法执行耗时: {}#{} = {}", joinPoint.getSignature().getDeclaringTypeName(), joinPoint.getSignature().getName() , System.currentTimeMillis() - start);
            }
            if (hasTag) {
                MdcHelp.restore(BIZ_CODE_KEY, oldBizCode);
            }
        }
    }

    private boolean addMdcCode(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        MdcDot dot = method.getAnnotation(MdcDot.class);
        if (dot == null) {
            dot = (MdcDot) joinPoint.getSignature().getDeclaringType().getAnnotation(MdcDot.class);
        }

        if (dot != null) {
            MdcHelp.add(BIZ_CODE_KEY, loadBizCode(dot.bizCode(), joinPoint));
            return true;
        }
        return false;
    }

    private String loadBizCode(String key, ProceedingJoinPoint joinPoint) {
        if (StringUtils.isBlank(key)) {
            return "";
        }
        StandardEvaluationContext context = new StandardEvaluationContext();

        context.setBeanResolver(new BeanFactoryResolver(applicationContext));
        String[] params = discoverParameterNames(((MethodSignature) joinPoint.getSignature()).getMethod());
        if (params == null) {
            return "";
        }
        Object[] args = joinPoint.getArgs();
        for (int i = 0; i < args.length; i++) {
            context.setVariable(params[i], args[i]);
        }
        try {
            // SpEL 解析失败降级为空串，禁止让日志切面异常炸掉业务方法
            String val = parser.parseExpression(key).getValue(context, String.class);
            return val == null ? "" : val;
        } catch (Exception e) {
            if (log.isWarnEnabled()) {
                log.warn("MdcAspect loadBizCode SpEL 解析失败, key:{}, 降级为空串", key, e);
            }
            return "";
        }
    }

    private String[] discoverParameterNames(Method method) {
        String[] params = parameterNameDiscoverer.getParameterNames(method);
        return params != null ? params : lvtParameterNames(method);
    }

    /**
     * Spring 6.1 删除了 LocalVariableTableParameterNameDiscoverer，宿主未开 -parameters 编译时
     * 反射拿不到参数名；回退读字节码 LocalVariableTable（spring-core 内嵌 ASM），保住 SpEL #参数名 绑定
     */
    private static String[] lvtParameterNames(Method method) {
        Class<?> clazz = method.getDeclaringClass();
        String resource = clazz.getName().replace('.', '/') + ".class";
        ClassLoader loader = clazz.getClassLoader();
        try (InputStream in = loader == null ? ClassLoader.getSystemResourceAsStream(resource) : loader.getResourceAsStream(resource)) {
            if (in == null) {
                return null;
            }
            String[] names = new String[method.getParameterCount()];
            int[] slots = new int[names.length];
            int slot = Modifier.isStatic(method.getModifiers()) ? 0 : 1;
            for (int i = 0; i < names.length; i++) {
                slots[i] = slot;
                slot += Type.getType(method.getParameterTypes()[i]).getSize();
            }
            String descriptor = Type.getMethodDescriptor(method);
            new ClassReader(in).accept(new ClassVisitor(Opcodes.ASM9) {
                @Override
                public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                    if (!name.equals(method.getName()) || !desc.equals(descriptor)) {
                        return null;
                    }
                    return new MethodVisitor(Opcodes.ASM9) {
                        @Override
                        public void visitLocalVariable(String varName, String desc, String signature, Label start, Label end, int index) {
                            for (int i = 0; i < slots.length; i++) {
                                if (slots[i] == index && names[i] == null) {
                                    names[i] = varName;
                                }
                            }
                        }
                    };
                }
            }, 0);
            for (String name : names) {
                if (name == null) {
                    return null;
                }
            }
            return names;
        } catch (Exception e) {
            return null;
        }
    }

    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
