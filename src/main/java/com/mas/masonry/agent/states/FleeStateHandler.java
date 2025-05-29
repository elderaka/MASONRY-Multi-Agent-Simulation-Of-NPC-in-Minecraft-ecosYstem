package com.mas.masonry.agent.states;

import com.mas.masonry.AgentEntity;
import com.mas.masonry.AgentEntity.AgentState;
import com.mas.masonry.MASONRY;
import com.mas.masonry.AgentEntity.AgentMemory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.Vec3;
// import com.mas.masonry.MASONRY; // Uncomment if MASONRY.LOGGER is used

public class FleeStateHandler implements IAgentStateHandler {

    @Override
    public void handle(AgentEntity agent) {
        AgentMemory memory = agent.getMemory();
        // Flee from the nearest danger
        MASONRY.LOGGER.info("{} is fleeing!", agent.getName().getString());
        LivingEntity danger = memory.getNearestDanger();
        if (danger != null) {
            Vec3 fleePos = DefaultRandomPos.getPosAway(agent, 16, 7, danger.position());
            if (fleePos != null) {
                agent.getNavigation().moveTo(fleePos.x, fleePos.y, fleePos.z, 1.2D); // Move faster when fleeing
            }
        } else {
            // No danger perceived, transition back to IDLE
            agent.setCurrentState(AgentState.IDLE);
            // MASONRY.LOGGER.info("{} is no longer fleeing, returning to IDLE.", agent.getName().getString());
        }
        // Decrease fear over time when fleeing
        memory.setFearLevel(Math.max(0, memory.getFearLevel() - 1));
    }
}
