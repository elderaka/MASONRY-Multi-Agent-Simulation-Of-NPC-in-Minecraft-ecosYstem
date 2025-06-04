package com.mas.masonry.agent.states;

import com.mas.masonry.AgentEntity;
import com.mas.masonry.MASONRY;

public class IdleStateHandler implements IAgentStateHandler {

    // Increased minimum idle time from 100 to 300 ticks (15 seconds)
    private static final int MIN_IDLE_TICKS = 300;

    // Increased maximum idle time from 300 to 900 ticks (45 seconds)
    private static final int MAX_IDLE_TICKS = 900;

    // Time this agent will idle before considering a state change
    private int targetIdleTicks = 0;

    @Override
    public void handle(AgentEntity agent) {
        // Set a random target idle time on first tick
        if (agent.getMemory().getTicksInCurrentState() == 0) {
            targetIdleTicks = agent.getRandom().nextInt(MAX_IDLE_TICKS - MIN_IDLE_TICKS + 1) + MIN_IDLE_TICKS;
            MASONRY.LOGGER.debug("{} entering IDLE state for {} ticks", agent.getBaseName(), targetIdleTicks);
        }

        // Nothing special to do while idle - agent will automatically transition
        // to other states based on FSM logic in determineNextState()

        // We might want to occasionally look around or perform other idle animations
        if (agent.getMemory().getTicksInCurrentState() % 20 == 0) {
            // 5% chance to look around while idle
            if (agent.getRandom().nextFloat() < 0.05) {
                // Look in a random direction
                float yaw = agent.getRandom().nextFloat() * 360.0F;
                agent.setYRot(yaw);
                agent.setYHeadRot(yaw);
            }
        }

        // Don't stay idle forever - force a state change evaluation after the target time
        if (agent.getMemory().getTicksInCurrentState() > targetIdleTicks) {
            // Instead of directly setting WANDER, we'll allow the FSM to decide
            // But we reset the ticks in state to force a re-evaluation
            agent.getMemory().resetTicksInState();
        }
    }
}