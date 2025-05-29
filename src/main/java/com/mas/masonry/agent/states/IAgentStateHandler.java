package com.mas.masonry.agent.states;

import com.mas.masonry.AgentEntity;

/**
 * Interface for classes that handle the logic for a specific agent state.
 */
public interface IAgentStateHandler {
    /**
     * Executes the behavior associated with this state for the given agent.
     * @param agent The agent whose state is being handled.
     */
    void handle(AgentEntity agent);
}
