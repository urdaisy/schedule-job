package com.schedule.job.security.aop;

import com.schedule.job.security.annotation.*;
import com.schedule.job.security.util.AdviceType;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;

/**
 * 切面扫描器
 */
public class AspectScanner implements ApplicationContextAware {
    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    /**
     * 扫描项目下的所有被@Aspenct注解标记的切面类，并创建切面实例AspectDefinition
     * @param basePackage
     * @return
     */
    public List<AspectDefinition> scanAspects(String basePackage) {
        // 1. 扫描包下的所有类
        Set<Class<?>> classes = scanClasses(basePackage);

        // 2. 检查是否有 @Aspect 注解
        List<AspectDefinition> aspects = new ArrayList<>();
        for (Class<?> clazz : classes) {
            if (clazz.isAnnotationPresent(Aspect.class)) {
                aspects.add(parseAspect(clazz));
            }
        }
        return aspects;
    }

    // 解析单个切面类
    private AspectDefinition parseAspect(Class<?> aspectClass) {
        Object aspectInstance = createAspectInstance(aspectClass);
        // 解析切点方法（@Pointcut）
        Map<String, String> pointcuts = parsePointcuts(aspectClass);
        // 解析通知方法（@Before、@After、@Around）
        List<AdviceDefinition> advices = parseAdvices(aspectClass, pointcuts);
        // 获取优先级
        int order = 0;
        if (aspectClass.isAnnotationPresent(Order.class)) {
            order = aspectClass.getAnnotation(Order.class).value();
        }
        // 构建 AspectDefinition（注意：这里需要从通知中提取切点表达式，或者使用默认的）
        String pointcutExpression = advices.isEmpty() ? "" : advices.get(0).getPointcutExpression();
        AspectDefinition aspectDefinition = new AspectDefinition();
        aspectDefinition.setAspectInstance(aspectInstance);
        aspectDefinition.setPointCutExpression(pointcutExpression);
        aspectDefinition.setAdvices(advices);
        aspectDefinition.setSortOrder(order);
        return aspectDefinition;
    }

    // 扫描包下的所有类
    private Set<Class<?>> scanClasses(String basePackage) {
        Set<Class<?>> classes = new HashSet<>();
        try {
            String path = basePackage.replace('.', '/');
            URL resource = Thread.currentThread().getContextClassLoader().getResource(path);
            if (resource != null) {
                File directory = new File(resource.getFile());
                if (directory.exists()) {
                    scanDirectory(directory, basePackage, classes);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("扫描包失败: " + basePackage, e);
        }
        return classes;
    }

    // 递归扫描目录
    private void scanDirectory(File directory, String packageName, Set<Class<?>> classes) {
        File[] files = directory.listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            if (file.isDirectory()) {
                scanDirectory(file, packageName + "." + file.getName(), classes);
            } else if (file.getName().endsWith(".class")) {
                String className = packageName + '.' + file.getName().substring(0, file.getName().length() - 6);
                try {
                    Class<?> clazz = Class.forName(className);
                    classes.add(clazz);
                } catch (ClassNotFoundException e) {
                    // 忽略无法加载的类
                }
            }
        }
    }

    // 创建切面实例
    private Object createAspectInstance(Class<?> aspectClass) {
        try {
            // 优先从 Spring 容器中获取
            if (applicationContext != null) {
                try {
                    return applicationContext.getBean(aspectClass);
                } catch (Exception e) {
                    // 如果容器中没有，使用反射创建
                }
            }
            // 使用反射创建实例
            return aspectClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException("创建切面实例失败: " + aspectClass.getName(), e);
        }
    }

    // 解析切点方法（@Pointcut）
    private Map<String, String> parsePointcuts(Class<?> aspectClass) {
        Map<String, String> pointcuts = new HashMap<>();
        Method[] methods = aspectClass.getDeclaredMethods();
        
        for (Method method : methods) {
            Pointcut pointcut = method.getAnnotation(Pointcut.class);
            if (pointcut != null) {
                // 方法名作为 key，切点表达式作为 value
                pointcuts.put(method.getName(), pointcut.execute());
            }
        }
        return pointcuts;
    }

    // 解析通知方法（@Before、@Around、@After）
    private List<AdviceDefinition> parseAdvices(Class<?> aspectClass, Map<String, String> pointcuts) {
        List<AdviceDefinition> advices = new ArrayList<>();
        Method[] methods = aspectClass.getDeclaredMethods();
        
        for (Method method : methods) {
            // 解析 @Before
            Before before = method.getAnnotation(Before.class);
            if (before != null) {
                String expression = resolvePointcutExpression(before.value(), pointcuts);
                int order = getMethodOrder(method);
                advices.add(new AdviceDefinition(AdviceType.BEFORE, method, expression, order));
            }
            
            // 解析 @Around
            Around around = method.getAnnotation(Around.class);
            if (around != null) {
                String expression = resolvePointcutExpression(around.value(), pointcuts);
                int order = getMethodOrder(method);
                advices.add(new AdviceDefinition(AdviceType.AROUND, method, expression, order));
            }
            
            // 解析 @After
            After after = method.getAnnotation(After.class);
            if (after != null) {
                String expression = resolvePointcutExpression(after.value(), pointcuts);
                int order = getMethodOrder(method);
                advices.add(new AdviceDefinition(AdviceType.AFTER, method, expression, order));
            }
        }
        
        // 按优先级排序
        advices.sort(Comparator.comparingInt(AdviceDefinition::getOrder));
        return advices;
    }

    // 解析切点表达式引用（如 "permissionPointcut()"）
    private String resolvePointcutExpression(String expression, Map<String, String> pointcuts) {
        if (expression == null || expression.isEmpty()) {
            return "";
        }
        // 如果表达式是方法名（如 "permissionPointcut()"），从 pointcuts Map 中查找
        if (expression.endsWith("()")) {
            String methodName = expression.substring(0, expression.length() - 2);
            return pointcuts.getOrDefault(methodName, expression);
        }
        return expression;
    }

    // 获取方法优先级
    private int getMethodOrder(Method method) {
        if (method.isAnnotationPresent(Order.class)) {
            return method.getAnnotation(Order.class).value();
        }
        return 0;
    }
}
