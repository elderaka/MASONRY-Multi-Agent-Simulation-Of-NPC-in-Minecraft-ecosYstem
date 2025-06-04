package com.mas.masonry.agent.states;

import com.mas.masonry.AgentEntity;
import com.mas.masonry.AgentEntity.AgentState;
import com.mas.masonry.MASONRY;
import com.mas.masonry.AgentEntity.AgentMemory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.Vec3;

public class FleeStateHandler implements IAgentStateHandler {

    // Minimum number of ticks to stay in flee state
    private static final int MIN_FLEE_TICKS = 300; // 5 seconds

    // Minimum fear level required to exit flee state
    private static final int SAFE_FEAR_LEVEL = 10;

    // Distance squared that is considered "safe" from danger
    private static final double SAFE_DISTANCE_SQR = 256.0; // 16 blocks squared

    // How often to recalculate flee position (in ticks)
    private static final int RECALCULATE_TICKS = 20; // 1 second

    @Override
    public void handle(AgentEntity agent) {
        AgentMemory memory = agent.getMemory();

        // Only log once when entering the state, not every tick
        if (memory.getTicksInCurrentState() == 0) {
            MASONRY.LOGGER.debug("{} is fleeing!", agent.getName().getString());
        }

        // Flee from the nearest danger
        LivingEntity danger = memory.getNearestDanger();

        // Check if we should recalculate the flee position
        if (memory.getTicksInCurrentState() % RECALCULATE_TICKS == 0 ||
                agent.getNavigation().isDone() ||
                !agent.getNavigation().isInProgress()) {

            // If there's still a danger, flee from it
            if (danger != null) {
                Vec3 fleePos = DefaultRandomPos.getPosAway(agent, 16, 7, danger.position());
                if (fleePos != null) {
                    agent.getNavigation().moveTo(fleePos.x, fleePos.y, fleePos.z, 1.2D); // Move faster when fleeing
                } else {
                    // If we can't find a flee position, try random movement
                    Vec3 randomPos = DefaultRandomPos.getPos(agent, 10, 5);
                    if (randomPos != null) {
                        agent.getNavigation().moveTo(randomPos.x, randomPos.y, randomPos.z, 1.1D);
                    }
                }
            } else if (memory.getTicksInCurrentState() < MIN_FLEE_TICKS) {
                // No immediate danger but haven't fled long enough yet - move randomly
                Vec3 randomPos = DefaultRandomPos.getPos(agent, 10, 5);
                if (randomPos != null) {
                    agent.getNavigation().moveTo(randomPos.x, randomPos.y, randomPos.z, 1.0D);
                }
            }
        }

        // Decrease fear more slowly while fleeing
        if (memory.getTicksInCurrentState() % 5 == 0) { // Only decrease every 5 ticks
            memory.setFearLevel(Math.max(0, memory.getFearLevel() - 1));
        }

        // Only exit FLEE state if:
        // 1. We've been fleeing for minimum time AND
        // 2. Either no danger is present OR we're far enough from danger AND
        // 3. Fear level has dropped below the safety threshold
        if (memory.getTicksInCurrentState() > MIN_FLEE_TICKS &&
                (danger == null || agent.distanceToSqr(danger) > SAFE_DISTANCE_SQR) &&
                memory.getFearLevel() < SAFE_FEAR_LEVEL) {

            // Safe to exit flee state
            agent.setCurrentState(AgentState.IDLE);
            // Keep some fear to prevent immediate return to danger
            memory.setFearLevel(Math.max(10, memory.getFearLevel()));
        }
    }
}