package com.mas.masonry.agent.states;

import com.mas.masonry.AgentEntity;
import com.mas.masonry.AgentEntity.AgentState;
import com.mas.masonry.AgentEntity.AgentMemory;
import java.util.Optional;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
// import com.mas.masonry.MASONRY; // Uncomment if MASONRY.LOGGER is used

public class AttackStateHandler implements IAgentStateHandler {

    @Override
    public void handle(AgentEntity agent) {
        AgentMemory memory = agent.getMemory();
        Optional<LivingEntity> target = memory.getAttackTarget();

        if (target.isPresent() && target.get().isAlive() && agent.hasLineOfSight(target.get())) {
            // MASONRY.LOGGER.info("{} is attacking {}.", agent.getName().getString(), target.getName().getString());
            agent.getLookControl().setLookAt(target.get(), 30.0F, 30.0F); // Look at the target
            agent.getNavigation().moveTo(target.get(), 1.0D); // Move towards the target

            if (agent.distanceToSqr(target.get()) < agent.getAttackRangeSqr()) { // If close enough, attack
                agent.swing(InteractionHand.MAIN_HAND);
                agent.doHurtTarget(target.get());
            }
        } else {
            // No target, or target is dead/out of sight, transition back to IDLE
            memory.setAttackTarget(null);
            agent.setCurrentState(AgentState.IDLE);
            // MASONRY.LOGGER.info("{} is no longer attacking, returning to IDLE.", agent.getName().getString());
        }
    }
}
