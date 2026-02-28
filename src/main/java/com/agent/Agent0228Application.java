package com.agent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Agent0228 Application
 * 
 * Main entry point for the AI Agent Quick Start application
 */
@SpringBootApplication
public class Agent0228Application {

    public static void main(String[] args) {
        SpringApplication.run(Agent0228Application.class, args);
        System.out.println("========================================");
        System.out.println("Agent0228 Started successfully!");
        System.out.println("API Endpoint: http://localhost:8080");
        System.out.println("========================================");
    }

}
