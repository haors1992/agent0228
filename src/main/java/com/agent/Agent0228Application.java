package com.agent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Agent0228 Application
 * 
 * === 启动机制学习 ===
 * 
 * @SpringBootApplication 注解说明：
 *                        1. @Configuration - 标记这是一个配置类，可以用 @Bean 定义 Bean
 *                        2. @EnableAutoConfiguration - 启用自动配置，自动检测 classpath
 *                        中的依赖
 *                        3. @ComponentScan - 扫描 package com.agent 及其子包下的组件
 * 
 *                        扫描会找到：
 *                        - @Controller 类 (如 ChatController)
 *                        - @Service 类 (如 SessionManager)
 *                        - @Component 类 (任何被标记的组件)
 */
@SpringBootApplication
public class Agent0228Application {

    public static void main(String[] args) {
        System.out.println("========== 【步骤 1】开始启动 Spring Boot 应用 ==========");
        System.out.println("主类: com.agent.Agent0228Application");
        System.out.println();

        // 【关键步骤】创建 SpringApplication 对象并运行
        ConfigurableApplicationContext context = SpringApplication.run(
                Agent0228Application.class, args);

        System.out.println();
        System.out.println("========== 【步骤 2】Spring 容器已初始化 ==========");
        System.out.println("ApplicationContext 类型: " + context.getClass().getSimpleName());
        System.out.println("容器中的 Bean 总数: " + context.getBeanDefinitionCount());
        System.out.println();

        // 打印所有已加载的 Bean
        String[] beanNames = context.getBeanDefinitionNames();
        System.out.println("========== 【步骤 3】容器中已扫描到的关键 Bean ==========");
        int count = 0;
        for (String beanName : beanNames) {
            // 只打印我们自己定义的 Bean（来自 com.agent 包）
            if (beanName.contains("Agent") || beanName.contains("Controller") ||
                    beanName.contains("Service") || beanName.contains("Engine") ||
                    beanName.contains("Manager")) {
                System.out.println("  ✓ Bean 已注册: " + beanName);
                count++;
            }
        }
        System.out.println("  (共扫描到 " + count + " 个自定义 Bean)");
        System.out.println();

        System.out.println("========== 【步骤 4】应用启动完成 ==========");
        System.out.println("API Endpoint: http://localhost:8080");
        System.out.println("================================");
        System.out.println("Agent0228 已成功启动！");
        System.out.println("================================");
    }

}
