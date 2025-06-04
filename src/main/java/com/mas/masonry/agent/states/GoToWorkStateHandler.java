package com.mas.masonry.agent.states;

import com.mas.masonry.AgentEntity;
import com.mas.masonry.MASONRY;
import net.minecraft.core.BlockPos;

public class GoToWorkStateHandler implements IAgentStateHandler {
    
    private static final double CLOSE_ENOUGH_TO_WORK = 3.0;
    private static final int MAX_TRAVEL_TO_WORK_TICKS = 1200; // 1 minute max
    
    @Override
    public void handle(AgentEntity agent) {
        if (agent.getMemory().getTicksInCurrentState() == 0) {
            MASONRY.LOGGER.debug("{} is going to work", agent.getBaseName());
        }
        
        BlockPos jobSite = agent.getPoiManager().getJobSite();
        
        if (jobSite == null || !agent.getProfessionSystem().hasProfession()) {
            // No job site or profession, look for work
            agent.setCurrentState(AgentEntity.AgentState.LOOK_FOR_JOB);
            return;
        }
        
        // Check if it's still work time
        if (!agent.getScheduleSystem().isWorkTime(agent.level())) {
            // Work time over, go idle
            agent.setCurrentState(AgentEntity.AgentState.IDLE);
            return;
        }
        
        double distanceToWork = agent.blockPosition().distSqr(jobSite);
        
        if (distanceToWork > CLOSE_ENOUGH_TO_WORK * CLOSE_ENOUGH_TO_WORK) {
            // Navigate to work
            agent.getNavigation().moveTo(jobSite.getX(), jobSite.getY(), jobSite.getZ(), 1.0);
            
            // Give up if taking too long
            if (agent.getMemory().getTicksInCurrentState() > MAX_TRAVEL_TO_WORK_TICKS) {
                MASONRY.LOGGER.debug("{} couldn't reach work, going idle", agent.getBaseName());
                agent.setCurrentState(AgentEntity.AgentState.IDLE);
            }
        } else {
            // Reached work site!
            MASONRY.LOGGER.debug("{} reached work site", agent.getBaseName());
            agent.setCurrentState(AgentEntity.AgentState.WORK_AT_JOB_SITE);
        }
    }
}