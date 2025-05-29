package com.mas.masonry.agent.states;

import com.mas.masonry.AgentEntity;
import com.mas.masonry.AgentEntity.AgentState;
import com.mas.masonry.AgentEntity.AgentMemory;
import net.minecraft.world.entity.LivingEntity;
import java.util.Optional;

public class HelpAllyStateHandler implements IAgentStateHandler {

    @Override
    public void handle(AgentEntity agent) {
        AgentMemory memory = agent.getMemory();
        Optional<LivingEntity> targetOptional = memory.getTargetEntity(); // Using generic target entity for now

        if (targetOptional.isPresent()) {
            LivingEntity target = targetOptional.get();
            // MASONRY.LOGGER.info("{} is moving to help {}.", agent.getName().getString(), target.getName().getString());
            agent.getNavigation().moveTo(target, 1.0D);

            if (agent.distanceToSqr(target) < 25.0D) { // If close enough (5 blocks)
                // In a more complex system, might transition to ATTACK if enemy is also present,
                // or a GUARD_ALLY state. For now, just become IDLE.
                // MASONRY.LOGGER.info("{} is near the target, becoming IDLE.", agent.getName().getString());
                agent.setCurrentState(AgentState.IDLE);
            }
        } else {
            // No target to help, transition back to IDLE
            // MASONRY.LOGGER.info("{} has no target to help, becoming IDLE.", agent.getName().getString());
            agent.setCurrentState(AgentState.IDLE);
        }
    }
}
