package com.agent.demo;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 循环依赖演示服务
 * 
 * 展示不同类型的循环依赖和 Spring 如何处理它们
 */
@Slf4j
@Component
public class CircularDependencyDemoService {

    private final CircularDependencyServiceA serviceA;
    private final CircularDependencyServiceB serviceB;

    /**
     * 注入两个互相依赖的 Service
     * Spring 自动解决了它们之间的循环依赖！
     */
    public CircularDependencyDemoService(
            CircularDependencyServiceA serviceA,
            CircularDependencyServiceB serviceB) {
        this.serviceA = serviceA;
        this.serviceB = serviceB;
        log.info("✅ CircularDependencyDemoService 成功注入了两个有循环依赖的 Service");
    }

    public Map<String, Object> explainCircularDependency() {
        Map<String, Object> result = new HashMap<>();
        result.put("🔄 什么是循环依赖?", new HashMap<String, Object>() {
            {
                put("定义", "Bean A 需要 Bean B，而 Bean B 又需要 Bean A");
                put("图示", "ServiceA → ServiceB → ServiceA（形成了一个圆圈）");
                put("问题", "如果没有 Spring，会导致无限循环，程序崩溃");
            }
        });
        result.put("这个项目中的例子", new HashMap<String, Object>() {
            {
                put("ServiceA", "CircularDependencyServiceA");
                put("ServiceB", "CircularDependencyServiceB");
                put("关系", "ServiceA 需要 ServiceB，ServiceB 也需要 ServiceA");
                put("结果", "✅ 循环依赖存在，但 Spring 已自动解决它");
            }
        });
        return result;
    }

    public Map<String, Object> demonstrateWithoutSpring() {
        Map<String, Object> result = new HashMap<>();
        result.put("💔 没有 Spring 时会发生什么？", new HashMap<String, Object>() {
            {
                put("尝试创建 ServiceA", "new ServiceA(new ServiceB(new ServiceA(...)))");
                put("结果", "❌ 无限递归，栈溢出（StackOverflowError）");
                put("原因", "你的代码无法知道什么时候停止创建");
            }
        });
        result.put("具体代码会这样", new HashMap<String, Object>() {
            {
                put("第 1 步", "开始创建 ServiceA");
                put("第 2 步", "ServiceA 需要 ServiceB，开始创建");
                put("第 3 步", "ServiceB 需要 ServiceA，又开始创建");
                put("第 4 步", "ServiceA 需要 ServiceB，又开始创建");
                put("第 5 步", "... 无限循环 ...");
                put("第 ∞ 步", "栈空间用完，程序崩溃 ☠️");
            }
        });
        return result;
    }

    public Map<String, Object> demonstrateSpringMagic() {
        Map<String, Object> result = new HashMap<>();
        result.put("✨ Spring 用三级缓存解决循环依赖", new HashMap<String, Object>() {
            {
                put("一级缓存", "存放完全初始化的 Bean（成品）");
                put("二级缓存", "存放已创建但未初始化的 Bean（半成品）");
                put("三级缓存", "存放生产 Bean 的工厂（能生产成品）");
            }
        });
        result.put("具体过程", new HashMap<String, Object>() {
            {
                put("1 扫描", "发现 ServiceA 和 ServiceB");
                put("2 创建 A", "创建 ServiceA，加入二级缓存");
                put("3 发现需要 B", "开始创建 ServiceB");
                put("4 创建 B", "创建 ServiceB，加入二级缓存");
                put("5 发现需要 A", "从二级缓存取出 ServiceA（半成品）");
                put("6 注入 A 给B", "ServiceB 现在有了 ServiceA");
                put("7 初始化 B", "ServiceB 初始化完成，移到一级缓存");
                put("8 初始化 A", "ServiceA 初始化完成，移到一级缓存");
                put("9 完成", "✅ 两个有循环依赖的 Bean 都创建成功");
            }
        });
        return result;
    }

    public Map<String, Object> demonstrateProof() {
        Map<String, Object> result = new HashMap<>();
        result.put("🔍 循环依赖确实存在", new HashMap<String, Object>() {
            {
                put("ServiceA 的信息", serviceA.getInfo());
                put("ServiceB 的信息", serviceB.getInfo());
            }
        });
        result.put("验证循环关系", new HashMap<String, Object>() {
            {
                put("ServiceA.serviceB",
                        serviceA.getServiceB() != null ? serviceA.getServiceB().getClass().getSimpleName() : "null");
                put("ServiceB.serviceA",
                        serviceB.getServiceA() != null ? serviceB.getServiceA().getClass().getSimpleName() : "null");
                put("结论", "✅ 循环依赖确实存在，且能正常工作!");
            }
        });
        return result;
    }

    public Map<String, Object> demonstrateUnsolvable() {
        Map<String, Object> result = new HashMap<>();
        result.put("❌ Spring 无法解决的情况", new HashMap<String, Object>() {
            {
                put("1 构造器循环依赖", "无法延迟执行构造器 → ❌ BeanCurrentlyInCreationException");
                put("2 代理对象循环依赖", "AOP 代理创建顺序问题 → ❌ 可能无法解决");
                put("3 单例与原型混合", "作用域不同导致的问题 → ❌ 可能不一致");
            }
        });
        result.put("✅ Spring 能解决的情况", new HashMap<String, Object>() {
            {
                put("Setter 注入循环依赖", "可以先创建再注入 → ✅ 自动解决");
                put("字段注入循环依赖", "@Autowired 字段注入 → ✅ 自动解决");
                put("本演示的例子", "ServiceA ↔ ServiceB → ✅ 已自动解决");
            }
        });
        return result;
    }

    public Map<String, Object> demonstrateBestPractices() {
        Map<String, Object> result = new HashMap<>();
        result.put("💡 解决循环依赖的最佳实践", new HashMap<String, Object>() {
            {
                put("方案 1: Setter 注入", "✅ Spring 自动解决，但要注意 null");
                put("方案 2: ObjectProvider", "✅ 灵活延迟注入，推荐使用");
                put("方案 3: 重新设计架构", "✅✅✅ 最好的方案，项目上去必须做");
                put("方案 4: @Lazy 延迟加载", "⚠️  可能隐藏问题，最后考虑");
            }
        });
        result.put("🎯 优先级", new HashMap<String, Object>() {
            {
                put("第 1 优先", "方案 3 - 重新设计（根本解决）");
                put("第 2 优先", "方案 2 - ObjectProvider（灵活）");
                put("第 3 优先", "方案 1 - Setter 注入（快速）");
                put("最后才用", "方案 4 - @Lazy（隐患）");
            }
        });
        return result;
    }
}
