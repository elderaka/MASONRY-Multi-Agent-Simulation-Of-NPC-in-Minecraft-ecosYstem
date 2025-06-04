package com.mas.masonry.agent.states;

import com.mas.masonry.AgentEntity;
import com.mas.masonry.MASONRY;
import com.mas.masonry.agent.systems.ScheduleSystem;

public class TakeWorkBreakStateHandler implements IAgentStateHandler {
    
    private static final int MIN_BREAK_TIME = 200; // 10 seconds minimum
    private static final int MAX_BREAK_TIME = 1200; // 1 minute maximum
    
    @Override
    public void handle(AgentEntity agent) {
        if (agent.getMemory().getTicksInCurrentState() == 0) {
            MASONRY.LOGGER.debug("{} is taking a work break", agent.getBaseName());
        }
        
        // Stop any movement during break
        agent.getNavigation().stop();

        if (agent.getMemory().getTicksInCurrentState() < MIN_BREAK_TIME) {
            return; // Force agent to stay in break for minimum duration
        }


        // Check if break should end
        ScheduleSystem.AgentActivity currentActivity = agent.getScheduleSystem().determineActivity(agent.level(), agent);

        // Only check for transitions after minimum break time
        if (currentActivity == ScheduleSystem.AgentActivity.WORK) {
            // Break over, return to work
            MASONRY.LOGGER.debug("{} break is over, returning to work", agent.getBaseName());
            agent.setCurrentState(AgentEntity.AgentState.GO_TO_WORK);
            return;
        } else if (currentActivity == ScheduleSystem.AgentActivity.SOCIALIZE) {
            // Break turned into social time
            agent.setCurrentState(AgentEntity.AgentState.GO_TO_MEETING);
            return;
        } else if (currentActivity != ScheduleSystem.AgentActivity.BREAK &&
                currentActivity != ScheduleSystem.AgentActivity.WORK) {
            // Work day ended during break
            agent.setCurrentState(AgentEntity.AgentState.IDLE);
            return;
        }

        // Force end break if too long
        if (agent.getMemory().getTicksInCurrentState() > MAX_BREAK_TIME) {
            agent.setCurrentState(AgentEntity.AgentState.GO_TO_WORK);
        }


        // Heal a bit during break
        if (agent.getMemory().getTicksInCurrentState() % 100 == 0 && 
            agent.getHealth() < agent.getMaxHealth()) {
            agent.heal(0.5f);
        }
    }
}