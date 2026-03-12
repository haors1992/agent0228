package com.agent.demo;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 循环依赖演示 - Service B
 * 
 * Service B 需要 Service A
 * 而 Service A 又需要 Service B
 * 这形成了循环依赖
 */
@Slf4j
@Service
public class CircularDependencyServiceB {

    private CircularDependencyServiceA serviceA;

    // Setter 注入（允许循环依赖）
    public void setServiceA(CircularDependencyServiceA serviceA) {
        this.serviceA = serviceA;
        log.info("✅ ServiceB 获得了 ServiceA 的引用");
    }

    public CircularDependencyServiceA getServiceA() {
        return serviceA;
    }

    public String getName() {
        return this.getClass().getSimpleName();
    }

    public Map<String, Object> getInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("服务名", "CircularDependencyServiceB");
        info.put("持有的引用",
                serviceA != null ? serviceA.getClass().getSimpleName() : "未初始化");
        info.put("情况", "Setter 注入的循环依赖，Spring 可以自动解决");
        return info;
    }
}
