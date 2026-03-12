package com.agent.demo;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Bean 容器演示服务
 * 
 * 演示：Spring 容器如何管理所有的 Bean
 * 展示：165 个 Bean 是如何被组织和管理的，不会乱、不会状态不一致
 */
@Slf4j
@Service
public class BeanContainerDemoService {

    private final ApplicationContext applicationContext;

    /**
     * ApplicationContext 就是 Spring 容器本身
     * Spring 会自动注入这个容器
     */
    public BeanContainerDemoService(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
        log.info("✅ BeanContainerDemoService 已创建，获得了 ApplicationContext 容器的引用");
    }

    /**
     * 获取容器中的所有 Bean 统计信息
     */
    public Map<String, Object> getContainerStatistics() {
        Map<String, Object> stats = new HashMap<>();

        // 1. 获取容器类型
        String containerType = applicationContext.getClass().getSimpleName();
        stats.put("容器类型", containerType);
        stats.put("说明", "这是 Spring 的核心容器，管理着所有的 Bean");

        // 2. 获取 Bean 总数
        int beanCount = applicationContext.getBeanDefinitionCount();
        stats.put("容器中 Bean 总数", beanCount);

        // 3. 获取所有 Bean 的名称
        String[] beanNames = applicationContext.getBeanDefinitionNames();
        stats.put("所有 Bean 已注册", "✓（共 " + beanNames.length + " 个）");

        // 4. 统计自定义的 Bean（来自 com.agent 包）
        List<String> customBeans = new ArrayList<>();
        Map<String, Object> customBeanMap = new HashMap<>();

        for (String beanName : beanNames) {
            if (beanName.contains("Agent") || beanName.contains("Controller") ||
                    beanName.contains("Service") || beanName.contains("Manager") ||
                    beanName.contains("Engine") || beanName.contains("Config")) {

                customBeans.add(beanName);

                // 获取这个 Bean 的实际对象
                try {
                    Object beanInstance = applicationContext.getBean(beanName);
                    // 记录 Bean 的类名和 HashCode
                    customBeanMap.put(
                            beanName,
                            new HashMap<String, Object>() {
                                {
                                    put("类", beanInstance.getClass().getSimpleName());
                                    put("HashCode", beanInstance.hashCode()); // Bean 的唯一标识
                                    put("说明", "这个 Bean 对象的唯一标识，相同 HashCode 表示同一个对象");
                                }
                            });
                } catch (Exception e) {
                    // 某些 Bean 无法获取，跳过
                }
            }
        }

        stats.put("自定义 Bean 列表", customBeanMap);
        stats.put("自定义 Bean 数量", customBeans.size());

        return stats;
    }

    /**
     * 演示：容器中的 Bean 不会乱 - 唯一标识
     * 
     * 每个 Bean 都有唯一的名称和 HashCode
     * 即使名称相同的类，也只会有一个实例（Singleton）
     */
    public Map<String, Object> demonstrateUniqueness() {
        Map<String, Object> result = new HashMap<>();

        // 获取同一个 Bean 多次
        Object bean1 = applicationContext.getBean("beanDemoService");
        Object bean2 = applicationContext.getBean("beanDemoService");
        Object bean3 = applicationContext.getBean("beanDemoService");

        result.put("获取同一个 Bean 三次:", new HashMap<String, Object>() {
            {
                put("第一次 HashCode", bean1.hashCode());
                put("第二次 HashCode", bean2.hashCode());
                put("第三次 HashCode", bean3.hashCode());
                put("三次都相同吗?", bean1.hashCode() == bean2.hashCode() && bean2.hashCode() == bean3.hashCode());
            }
        });

        result.put("✅ 核心发现", "Spring 容器中每个 Bean 只有一个实例（Singleton），" +
                "多次获取都返回同一个对象，所以不会乱、状态始终一致！");

        return result;
    }

    /**
     * 演示：Bean 的隔离性 - 不同的 Bean 是不同的对象
     */
    public Map<String, Object> demonstrateIsolation() {
        Map<String, Object> result = new HashMap<>();

        String[] beanNames = applicationContext.getBeanDefinitionNames();

        Map<String, String> beanHashCodes = new HashMap<>();

        // 列出一些主要的 Bean 及其 HashCode
        for (String beanName : beanNames) {
            if (beanName.contains("Controller") || beanName.contains("Service") ||
                    beanName.contains("Manager") || beanName.contains("Engine")) {
                try {
                    Object bean = applicationContext.getBean(beanName);
                    beanHashCodes.put(
                            beanName,
                            bean.getClass().getSimpleName() + " [HashCode: " + bean.hashCode() + "]");
                } catch (Exception e) {
                    // 忽略
                }
            }
        }

        result.put("不同 Bean 的 HashCode 都不同", beanHashCodes);
        result.put("✅ 核心发现", "每个 Bean 对象都有唯一的 HashCode，" +
                "不同的 Bean 是完全不同的对象，互不干扰，保证了隔离性和独立性！");

        return result;
    }

    /**
     * 演示：Bean 的中央管理 - Spring 容器掌握一切
     */
    public Map<String, Object> demonstrateCentralManagement() {
        Map<String, Object> result = new HashMap<>();

        // 获取容器类型
        String containerClass = applicationContext.getClass().getName();
        String containerName = applicationContext.getApplicationName();

        result.put("Spring 容器信息", new HashMap<String, Object>() {
            {
                put("容器类", containerClass);
                put("容器名称", containerName != null ? containerName : "默认容器");
                put("说明", "这一个中央容器，所有 Bean 都由它创建、初始化、管理和销毁");
            }
        });

        // Bean 的管理流程
        result.put("容器的管理流程", new HashMap<String, Object>() {
            {
                put("1. 扫描", "在应用启动时，Spring 自动扫描所有 @Component 等注解");
                put("2. 创建", "Spring 创建所有 Bean 的实例（只创建一次，Singleton）");
                put("3. 注入", "Spring 注入所有依赖和配置值（@Autowired、@Value 等）");
                put("4. 初始化", "Spring 调用 @PostConstruct 方法进行初始化");
                put("5. 存储", "Spring 将 Bean 存储在内部 Map 中，用 beanName 作为 key");
                put("6. 分发", "应用代码需要时，从容器中获取 Bean");
                put("7. 销毁", "应用关闭时，Spring 依次调用 @PreDestroy 方法销毁 Bean");
            }
        });

        result.put("✅ 核心发现", "Spring 容器完全控制了 Bean 的整个生命周期，" +
                "你不用担心创建、初始化、销毁，Spring 全部搞定！");

        return result;
    }

    /**
     * 演示：为什么不会状态不一致
     * 
     * 因为都是 Singleton（单例），所有地方用的都是同一个对象
     */
    public Map<String, Object> demonstrateConsistency() {
        Map<String, Object> result = new HashMap<>();

        result.put("为什么不会状态不一致？", new HashMap<String, Object>() {
            {
                put("原因 1: Singleton 单例模式",
                        "Spring 中的所有 Bean 默认都是单例，" +
                                "容器中只维护一个实例，多个地方使用时都是同一个对象");

                put("原因 2: 中央集中管理",
                        "所有 Bean 都由 ApplicationContext（Spring 容器）统一创建和管理，" +
                                "不会有多个 Bean 实例在不同地方各自为政");

                put("原因 3: 线程安全的容器",
                        "Spring 容器的 Bean Map 是线程安全的，" +
                                "多个线程同时访问同一个 Bean 时不会有并发问题");

                put("原因 4: 状态隔离",
                        "每个 Bean 都是完全独立的对象，" +
                                "Bean A 的状态改变不会影响 Bean B");
            }
        });

        // 具体例子
        result.put("具体例子", new HashMap<String, Object>() {
            {
                put("场景", "ChatController、MonitoringController、KnowledgeBaseController 都要使用 SessionManager");

                put("情况1 - 没有 Spring",
                        "各个 Controller 各自 new 一个 SessionManager，" +
                                "结果：3 个不同的 SessionManager 对象，状态完全不一样 ❌");

                put("情况2 - 使用 Spring",
                        "Spring 创建 1 个 SessionManager Bean，" +
                                "所有 Controller 都注入同一个 Bean，" +
                                "结果：3 个 Controller 使用同一个 SessionManager，状态完全一致 ✅");
            }
        });

        return result;
    }
}
