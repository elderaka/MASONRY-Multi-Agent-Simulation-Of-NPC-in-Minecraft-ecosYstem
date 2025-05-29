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
        Optional<LivingEntity> targetOptional = memory.getTargetEntity();

        if (targetOptional.isPresent()) {
            LivingEntity target = targetOptional.get();
            agent.getNavigation().moveTo(target, 1.0D);

            if (agent.distanceToSqr(target) < 25.0D) { // If close enough (5 blocks)
                agent.setCurrentState(AgentState.IDLE);
            }
        } else {
            agent.setCurrentState(AgentState.IDLE);
        }
    }
}
