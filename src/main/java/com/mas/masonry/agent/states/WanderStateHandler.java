package com.mas.masonry.agent.states;

import com.mas.masonry.AgentEntity;
import com.mas.masonry.MASONRY;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.Vec3;

public class WanderStateHandler implements IAgentStateHandler {

    // Reduced maximum wander time from 400 to 200 ticks (10 seconds)
    private static final int MAX_WANDER_TICKS = 200;

    // Reduced minimum wander time from 100 to 60 ticks (3 seconds)
    private static final int MIN_WANDER_TICKS = 60;

    // Time before trying to pick a new wander target
    private static final int RETARGET_TICKS = 60;

    // Time this agent will wander before considering a state change
    private int targetWanderTicks = 0;

    @Override
    public void handle(AgentEntity agent) {
        // Set a random target wander time on first tick
        if (agent.getMemory().getTicksInCurrentState() == 0) {
            targetWanderTicks = agent.getRandom().nextInt(MAX_WANDER_TICKS - MIN_WANDER_TICKS + 1) + MIN_WANDER_TICKS;
            MASONRY.LOGGER.debug("{} entering WANDER state for {} ticks", agent.getBaseName(), targetWanderTicks);
        }

        // Check if we need to pick a new random position
        if (agent.getMemory().getTicksInCurrentState() % RETARGET_TICKS == 0 ||
                !agent.getNavigation().isInProgress()) {

            // Find a random position to wander to
            Vec3 targetPos = DefaultRandomPos.getPos(agent, 8, 4);

            if (targetPos != null) {
                // Move to the random position
                agent.getNavigation().moveTo(targetPos.x, targetPos.y, targetPos.z, 1.0);
            }
        }

        // Don't wander forever - consider a state change after the target time
        if (agent.getMemory().getTicksInCurrentState() > targetWanderTicks) {
            // Increased chance to return to IDLE from 50% to 80%
            if (agent.getRandom().nextFloat() < 0.8) {
                MASONRY.LOGGER.debug("{} finished wandering, returning to IDLE", agent.getBaseName());
                agent.setCurrentState(AgentEntity.AgentState.IDLE);
            } else {
                // Otherwise, just reset the wander timer and keep wandering a bit longer
                agent.getMemory().resetTicksInState();
            }
        }
    }
}