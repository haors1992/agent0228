package com.agent.reasoning.engine;

import com.agent.tool.model.ToolResult;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Execution Context
 * 
 * Maintains the context and state during the Agent's reasoning process
 */
@Data
@NoArgsConstructor
public class ExecutionContext {
    
    /**
     * Original user query
     */
    private String userQuery;
    
    /**
     * Conversation messages (for context)
     */
    private List<String> messages = new ArrayList<>();
    
    /**
     * List of thought-action-observation cycles
     */
    private List<ThoughtAction> thoughtActions = new ArrayList<>();
    
    /**
     * List of tool execution results
     */
    private List<ToolResult> toolResults = new ArrayList<>();
    
    /**
     * Final answer from the agent
     */
    private String finalAnswer;
    
    /**
     * Current iteration number
     */
    private Integer currentIteration = 0;
    
    /**
     * Whether the reasoning is complete
     */
    private Boolean isComplete = false;
    
    /**
     * Start time of execution
     */
    private Long startTime;
    
    /**
     * End time of execution
     */
    private Long endTime;
    
    public ExecutionContext(String userQuery) {
        this.userQuery = userQuery;
        this.startTime = System.currentTimeMillis();
    }
    
    /**
     * Add a message to the conversation
     */
    public void addMessage(String message) {
        messages.add(message);
    }
    
    /**
     * Add a thought-action pair
     */
    public void addThoughtAction(ThoughtAction thoughtAction) {
        thoughtActions.add(thoughtAction);
    }
    
    /**
     * Add a tool result
     */
    public void addToolResult(ToolResult toolResult) {
        toolResults.add(toolResult);
    }
    
    /**
     * Mark as complete
     */
    public void finish(String answer) {
        this.finalAnswer = answer;
        this.isComplete = true;
        this.endTime = System.currentTimeMillis();
    }
    
    /**
     * Get execution duration in milliseconds
     */
    public Long getExecutionTimeMs() {
        if (endTime != null && startTime != null) {
            return endTime - startTime;
        }
        return null;
    }
    
    /**
     * Get the full context as a string for LLM
     */
    public String getContextAsString() {
        StringBuilder context = new StringBuilder();
        context.append("Original Query: ").append(userQuery).append("\n\n");
        
        for (int i = 0; i < thoughtActions.size(); i++) {
            ThoughtAction ta = thoughtActions.get(i);
            context.append("Iteration ").append(i + 1).append(":\n");
            context.append("Thought: ").append(ta.getThought()).append("\n");
            context.append("Action: ").append(ta.getAction()).append("\n");
            context.append("Action Input: ").append(ta.getActionInput()).append("\n");
            
            if (i < toolResults.size()) {
                ToolResult result = toolResults.get(i);
                context.append("Observation: ").append(result.getResult()).append("\n\n");
            }
        }
        
        return context.toString();
    }
}
