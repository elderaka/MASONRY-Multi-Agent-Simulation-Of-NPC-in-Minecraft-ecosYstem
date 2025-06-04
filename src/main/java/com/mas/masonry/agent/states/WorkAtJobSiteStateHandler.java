package com.mas.masonry.agent.states;

import com.mas.masonry.AgentEntity;
import com.mas.masonry.MASONRY;
import com.mas.masonry.agent.systems.ScheduleSystem;
import net.minecraft.core.BlockPos;

public class WorkAtJobSiteStateHandler implements IAgentStateHandler {
    
    private static final int WORK_TASK_INTERVAL = 40; // Perform task every 2 seconds
    private static final int MAX_WORK_SESSION = 2400; // 2 minutes max continuous work
    
    @Override
    public void handle(AgentEntity agent) {
        if (agent.getMemory().getTicksInCurrentState() == 0) {
            MASONRY.LOGGER.debug("{} started working", agent.getBaseName());
        }
        
        // Check if should still be working
        ScheduleSystem.AgentActivity currentActivity = agent.getScheduleSystem().determineActivity(agent.level(), agent);
        
        if (currentActivity == ScheduleSystem.AgentActivity.BREAK) {
            agent.setCurrentState(AgentEntity.AgentState.TAKE_WORK_BREAK);
            return;
        } else if (currentActivity != ScheduleSystem.AgentActivity.WORK) {
            // Work time over
            MASONRY.LOGGER.debug("{} finished work for the day", agent.getBaseName());
            agent.setCurrentState(AgentEntity.AgentState.IDLE);
            return;
        }
        
        BlockPos jobSite = agent.getPoiManager().getJobSite();
        if (jobSite == null) {
            agent.setCurrentState(AgentEntity.AgentState.LOOK_FOR_JOB);
            return;
        }
        
        // Check if still at work site
        double distanceToWork = agent.blockPosition().distSqr(jobSite);
        if (distanceToWork > 9.0) { // 3 blocks away
            // Moved away from work, go back
            agent.setCurrentState(AgentEntity.AgentState.GO_TO_WORK);
            return;
        }
        
        // Perform work tasks periodically
        if (agent.getMemory().getTicksInCurrentState() % WORK_TASK_INTERVAL == 0) {
            agent.getProfessionSystem().performProfessionTask(agent, jobSite);
            
            // Look at work site while working
            lookAtPosition(agent, jobSite);
        }
        
        // Take breaks after working for too long
        if (agent.getMemory().getTicksInCurrentState() > MAX_WORK_SESSION) {
            agent.setCurrentState(AgentEntity.AgentState.TAKE_WORK_BREAK);
        }
    }
    
    private void lookAtPosition(AgentEntity agent, BlockPos pos) {
        double dx = pos.getX() - agent.getX();
        double dz = pos.getZ() - agent.getZ();
        
        float yaw = (float) (Math.atan2(dz, dx) * 180.0 / Math.PI) - 90.0F;
        agent.setYRot(yaw);
        agent.setYHeadRot(yaw);
    }
}