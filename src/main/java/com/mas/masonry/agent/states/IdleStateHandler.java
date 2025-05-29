package com.mas.masonry.agent.states;

import com.mas.masonry.AgentEntity;

public class IdleStateHandler implements IAgentStateHandler {

    @Override
    public void handle(AgentEntity agent) {
        // In idle state, the agent mostly just observes
        // Gradually reduce fear level while idle
        if (agent.getMemory().getFearLevel() > 0 && agent.getRandom().nextFloat() < 0.1f) {
            agent.getMemory().setFearLevel(agent.getMemory().getFearLevel() - 1);
        }
        // MASONRY.LOGGER.debug("{} is idling. Fear level: {}", agent.getName().getString(), agent.getMemory().getFearLevel());
    }
}
