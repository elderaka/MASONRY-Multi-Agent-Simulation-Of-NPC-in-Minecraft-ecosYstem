package com.mas.masonry.agent.states;

import com.mas.masonry.AgentEntity;
import com.mas.masonry.AgentEntity.AgentState;
import com.mas.masonry.AgentEntity.AgentMemory;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;


public class MoveToTargetBlockStateHandler implements IAgentStateHandler {

    @Override
    public void handle(AgentEntity agent) {
        AgentMemory memory = agent.getMemory();
        Vec3 targetPosVec = agent.getTargetPos(); // Precise Vec3 for construction
        BlockPos targetBlock = agent.getTargetBlockPos(); // BlockPos for harvesting/general

        boolean moved = false;

        if (targetPosVec != null) {
            // MASONRY.LOGGER.info("{} moving to precise targetPos: {}", agent.getName().getString(), targetPosVec);
            agent.getNavigation().moveTo(targetPosVec.x, targetPosVec.y, targetPosVec.z, 1.0D);
            moved = true;
            if (agent.position().distanceToSqr(targetPosVec) < 2.25) { // 1.5 blocks squared
                // MASONRY.LOGGER.info("{} reached precise targetPos. Transitioning to PLACE_CONSTRUCTION_BLOCK.", agent.getName().getString());
                agent.setCurrentState(AgentState.PLACE_CONSTRUCTION_BLOCK);
                agent.setTargetPos(null); // Clear after reaching
                memory.resetTicksInState();
                return;
            }
        } else if (targetBlock != null) {
            // MASONRY.LOGGER.info("{} moving to targetBlockPos: {}", agent.getName().getString(), targetBlock);
            agent.getNavigation().moveTo(targetBlock.getX() + 0.5D, targetBlock.getY(), targetBlock.getZ() + 0.5D, 1.0D);
            moved = true;
            // Check if agent is close enough to the center of the block or adjacent
            if (agent.blockPosition().distSqr(targetBlock) < 2.25) { // Closer than 1.5 blocks to the block's center
                // MASONRY.LOGGER.info("{} reached targetBlockPos. Transitioning to HARVEST_BLOCK.", agent.getName().getString());
                agent.setCurrentState(AgentState.HARVEST_BLOCK); // Or other appropriate state like PLACE_CONSTRUCTION_BLOCK if that's the goal
                // agent.setTargetBlockPos(null); // Clearing targetBlockPos might be premature if harvesting takes time
                memory.resetTicksInState();
                return;
            }
        }

        if (!moved) {
            // MASONRY.LOGGER.warn("{} is in MOVE_TO_TARGET_BLOCK state with no targetPos or targetBlockPos. Returning to IDLE.", agent.getName().getString());
            agent.setCurrentState(AgentState.IDLE);
            memory.resetTicksInState();
            return;
        }

        if (memory.getTicksInCurrentState() > agent.getMaxTicksToReachBlock()) {
            // MASONRY.LOGGER.info("{} timed out moving to target. Returning to IDLE.", agent.getName().getString());
            agent.setCurrentState(AgentState.IDLE);
            agent.setTargetBlockPos(null); // Clear target
            agent.setTargetPos(null);      // Clear target
            memory.resetTicksInState();
        }
    }
}
