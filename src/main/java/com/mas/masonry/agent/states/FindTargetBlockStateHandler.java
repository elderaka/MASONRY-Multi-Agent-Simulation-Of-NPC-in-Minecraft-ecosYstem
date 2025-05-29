package com.mas.masonry.agent.states;

import com.mas.masonry.AgentEntity;
import com.mas.masonry.AgentEntity.AgentState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;

public class FindTargetBlockStateHandler implements IAgentStateHandler {

    @Override
    public void handle(AgentEntity agent) {
        // MASONRY.LOGGER.info("{} is finding target block: {}.", agent.getName().getString(), agent.getTargetBlockTypeToFind().toString());
        BlockPos currentTargetPos = agent.getTargetBlockPos();
        Block targetType = agent.getTargetBlockTypeToFind();

        if (targetType == null) {
            // MASONRY.LOGGER.warn("{} has no targetBlockType set in FIND_TARGET_BLOCK state. Transitioning to IDLE.", agent.getName().getString());
            agent.setCurrentState(AgentState.IDLE);
            return;
        }

        if (currentTargetPos == null || !isBlockOfType(agent, currentTargetPos, targetType)) {
            Optional<BlockPos> foundPos = findNearbyBlock(agent, targetType, agent.getFindBlockScanRadius());
            if (foundPos.isPresent()) {
                agent.setTargetBlockPos(foundPos.get());
                // MASONRY.LOGGER.info("{} found target block {} at {}.", agent.getName().getString(), targetType.toString(), foundPos.get().toString());
                agent.setCurrentState(AgentState.MOVE_TO_TARGET_BLOCK);
                agent.resetFindBlockAttempts();
            } else {
                agent.incrementFindBlockAttempts();
                if (agent.getFindBlockAttempts() >= agent.getMaxFindBlockAttempts()) {
                    // MASONRY.LOGGER.info("{} could not find target block {} after {} attempts. Returning to IDLE.", agent.getName().getString(), targetType.toString(), agent.getFindBlockAttempts());
                    agent.setCurrentState(AgentState.IDLE);
                    agent.resetFindBlockAttempts();
                }
                // If still not found and attempts not maxed, will try again next tick
            }
        } else {
            // Already has a valid target block position
            // MASONRY.LOGGER.info("{} already has target block {} at {}. Moving to target.", agent.getName().getString(), targetType.toString(), currentTargetPos.toString());
            agent.setCurrentState(AgentState.MOVE_TO_TARGET_BLOCK);
            agent.resetFindBlockAttempts();
        }
    }

    private static Optional<BlockPos> findNearbyBlock(AgentEntity agent, Block blockType, int radius) {
        BlockPos agentPos = agent.blockPosition();
        for (int y = -radius; y <= radius; y++) {
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos checkPos = agentPos.offset(x, y, z);
                    if (isBlockOfType(agent, checkPos, blockType)) {
                        // Check for air above to ensure it's accessible (simple check)
                        if (agent.level().isEmptyBlock(checkPos.above())) {
                           return Optional.of(checkPos);
                        }
                    }
                }
            }
        }
        return Optional.empty();
    }

    private static boolean isBlockOfType(AgentEntity agent, BlockPos pos, Block type) {
        BlockState state = agent.level().getBlockState(pos);
        return state.is(type);
    }
}
