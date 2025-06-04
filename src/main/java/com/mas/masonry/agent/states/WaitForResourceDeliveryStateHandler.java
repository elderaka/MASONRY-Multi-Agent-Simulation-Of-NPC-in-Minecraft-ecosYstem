package com.mas.masonry.agent.states;

import com.mas.masonry.AgentEntity;
import com.mas.masonry.MASONRY;

public class WaitForResourceDeliveryStateHandler implements IAgentStateHandler {
    
    private static final int MAX_WAIT_TIME = 2400;
    
    @Override
    public void handle(AgentEntity agent) {
        if (agent.getMemory().getTicksInCurrentState() == 0) {
            MASONRY.LOGGER.debug("{} is waiting for resource delivery", agent.getBaseName());
        }
        
        // Stop moving while waiting
        agent.getNavigation().stop();

        if (!agent.getResourceRequestSystem().hasNeededItems()) {
            // Got what we needed!
            MASONRY.LOGGER.debug("{} received needed resources", agent.getBaseName());
            
            // Return to construction or work
            if (agent.getCurrentBlueprintIndex() < MASONRY.SIMPLE_HUT_BLUEPRINT.size()) {
                agent.setCurrentState(AgentEntity.AgentState.PLACE_CONSTRUCTION_BLOCK);
            } else {
                agent.setCurrentState(AgentEntity.AgentState.IDLE);
            }
            return;
        }
        
        // Check for timeout
        if (agent.getMemory().getTicksInCurrentState() > MAX_WAIT_TIME) {
            MASONRY.LOGGER.debug("{} gave up waiting for resources", agent.getBaseName());
            
            // Give up and try to find resources ourselves
            agent.setCurrentState(AgentEntity.AgentState.SEEK_RESOURCE);
            return;
        }
        
        // Look around occasionally (shows the agent is waiting/looking for help)
        if (agent.getMemory().getTicksInCurrentState() % 100 == 0) {
            float randomYaw = agent.getRandom().nextFloat() * 360f;
            agent.setYRot(randomYaw);
            agent.setYHeadRot(randomYaw);
        }
    }
}