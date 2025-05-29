package com.mas.masonry.agent.states;

import com.mas.masonry.AgentEntity;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.Vec3;

public class WanderStateHandler implements IAgentStateHandler {

    @Override
    public void handle(AgentEntity agent) {
        // Simple wander: pick a random direction and move a bit
        // This is often better handled by a Goal, but can be a simple state behavior too
        if (agent.getNavigation().isDone() || agent.getRandom().nextFloat() < 0.05f) { // If path complete or randomly
            Vec3 randomTarget = DefaultRandomPos.getPos(agent, 10, 7);
            if (randomTarget != null) {
                agent.getNavigation().moveTo(randomTarget.x, randomTarget.y, randomTarget.z, 1.0D);
            }
        }
    }
}
