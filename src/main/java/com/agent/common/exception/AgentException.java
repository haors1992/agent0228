package com.agent.common.exception;

/**
 * Agent Exception
 * 
 * Base exception for agent operations
 */
public class AgentException extends RuntimeException {
    
    public AgentException(String message) {
        super(message);
    }
    
    public AgentException(String message, Throwable cause) {
        super(message, cause);
    }
}
