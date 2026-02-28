package com.agent.reasoning.engine;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Thought Action
 * 
 * Represents a thought and action pair from the Agent's reasoning
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ThoughtAction {
    
    /**
     * The thought/reasoning of the agent
     */
    @JsonProperty("thought")
    private String thought;
    
    /**
     * The action to take (tool name or "finish")
     */
    @JsonProperty("action")
    private String action;
    
    /**
     * Input parameters for the action
     */
    @JsonProperty("action_input")
    private String actionInput;
    
    /**
     * Whether the agent has decided to finish
     */
    @JsonProperty("is_finished")
    private Boolean isFinished;
    
    /**
     * Final answer (if finished)
     */
    @JsonProperty("final_answer")
    private String finalAnswer;
    
    /**
     * Timestamp when this decision was made
     */
    @JsonProperty("timestamp")
    private Long timestamp;
}
