
package com.mas.masonry.agent.states;

import com.mas.masonry.AgentEntity;
import com.mas.masonry.MASONRY;
import net.minecraft.core.BlockPos;

public class ReturnHomeStateHandler implements IAgentStateHandler {
    
    private static final double CLOSE_ENOUGH_TO_HOME = 2.0;
    private static final int MAX_RETURN_HOME_TICKS = 1200; // 1 minute max to return home
    
    @Override
    public void handle(AgentEntity agent) {
        // Log when entering state
        if (agent.getMemory().getTicksInCurrentState() == 0) {
            MASONRY.LOGGER.debug("{} is returning home", agent.getBaseName());
        }
        
        BlockPos home = agent.getHomeLocationSystem().getHomeLocation();
        
        if (home == null || !agent.getHomeLocationSystem().hasHome()) {
            // No home to return to, look for one
            agent.setCurrentState(AgentEntity.AgentState.LOOK_FOR_HOME);
            return;
        }
        
        // Navigate to home
        double distanceToHome = agent.blockPosition().distSqr(home);
        
        if (distanceToHome > CLOSE_ENOUGH_TO_HOME * CLOSE_ENOUGH_TO_HOME) {
            // Still moving to home
            agent.getNavigation().moveTo(home.getX(), home.getY(), home.getZ(), 1.0);
            
            // Give up if taking too long
            if (agent.getMemory().getTicksInCurrentState() > MAX_RETURN_HOME_TICKS) {
                MASONRY.LOGGER.debug("{} gave up returning home, switching to wander", agent.getBaseName());
                agent.setCurrentState(AgentEntity.AgentState.WANDER);
            }
        } else {
            // Reached home!
            MASONRY.LOGGER.debug("{} reached home", agent.getBaseName());
            
            // Determine what to do at home based on time and needs
            long dayTime = agent.level().getDayTime() % 24000;
            boolean isNight = dayTime >= 18000 || dayTime <= 6000;
            boolean lowHealth = agent.getMemory().getHealthPercent() < 50;
            
            if (isNight || lowHealth) {
                agent.setCurrentState(AgentEntity.AgentState.GO_TO_BED);
            } else {
                // During day, just rest briefly then go back to normal activities
                agent.setCurrentState(AgentEntity.AgentState.IDLE);
            }
        }
    }
}