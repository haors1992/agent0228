package com.agent.demo;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 循环依赖演示 - Service A
 * 
 * Service A 需要 Service B
 * 而 Service B 又需要 Service A
 * 这形成了循环依赖
 */
@Slf4j
@Service
public class CircularDependencyServiceA {

    private CircularDependencyServiceB serviceB;

    // Setter 注入（允许循环依赖）
    public void setServiceB(CircularDependencyServiceB serviceB) {
        this.serviceB = serviceB;
        log.info("✅ ServiceA 获得了 ServiceB 的引用");
    }

    public CircularDependencyServiceB getServiceB() {
        return serviceB;
    }

    public String getName() {
        return this.getClass().getSimpleName();
    }

    public Map<String, Object> getInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("服务名", "CircularDependencyServiceA");
        info.put("持有的引用",
                serviceB != null ? serviceB.getClass().getSimpleName() : "未初始化");
        info.put("情况", "Setter 注入的循环依赖，Spring 可以自动解决");
        return info;
    }
}
