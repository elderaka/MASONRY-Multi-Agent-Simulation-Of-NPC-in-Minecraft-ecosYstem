package com.mas.masonry.agent.states;

import com.mas.masonry.AgentEntity;
import com.mas.masonry.MASONRY;
import net.minecraft.core.BlockPos;

public class GoToBedStateHandler implements IAgentStateHandler {
    
    private static final double CLOSE_ENOUGH_TO_BED = 1.5;
    private static final int MIN_REST_TICKS = 600; // 30 seconds minimum rest
    private static final int MAX_REST_TICKS = 4800; // 4 minutes maximum rest
    
    private int restTicks = 0;
    
    @Override
    public void handle(AgentEntity agent) {
        if (agent.getMemory().getTicksInCurrentState() == 0) {
            MASONRY.LOGGER.debug("{} is going to bed", agent.getBaseName());
            restTicks = 0;
        }
        
        BlockPos bedPos = agent.getHomeLocationSystem().getHomeLocation();
        
        if (bedPos == null) {
            // No bed, look for home
            agent.setCurrentState(AgentEntity.AgentState.LOOK_FOR_HOME);
            return;
        }
        
        double distanceToBed = agent.blockPosition().distSqr(bedPos);
        
        if (distanceToBed > CLOSE_ENOUGH_TO_BED * CLOSE_ENOUGH_TO_BED) {
            // Move to bed
            agent.getNavigation().moveTo(bedPos.getX(), bedPos.getY(), bedPos.getZ(), 1.0);
        } else {
            // At bed, rest
            agent.getNavigation().stop();
            restTicks++;
            
            // Heal gradually while resting
            if (restTicks % 100 == 0 && agent.getHealth() < agent.getMaxHealth()) {
                agent.heal(1.0f);
            }
            
            // Update health perception
            agent.getMemory().updateHealthAwareness();
            
            // Check if should get up
            boolean shouldGetUp = false;
            
            // Minimum rest time met
            if (restTicks >= MIN_REST_TICKS) {
                long dayTime = agent.level().getDayTime() % 24000;
                boolean isDaytime = dayTime >= 6000 && dayTime < 18000;
                boolean healthRestored = agent.getMemory().getHealthPercent() >= 80;
                boolean maxRestReached = restTicks >= MAX_REST_TICKS;
                boolean dangerNearby = agent.getMemory().isDangerNearby();
                
                shouldGetUp = isDaytime || healthRestored || maxRestReached || dangerNearby;
            }
            
            if (shouldGetUp) {
                MASONRY.LOGGER.debug("{} is getting up from bed after {} ticks", agent.getBaseName(), restTicks);
                agent.setCurrentState(AgentEntity.AgentState.IDLE);
            }
        }
    }
}