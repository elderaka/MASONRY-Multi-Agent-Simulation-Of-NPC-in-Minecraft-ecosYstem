package com.mas.masonry.agent.states;

import com.mas.masonry.AgentEntity;
import com.mas.masonry.AgentEntity.AgentState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.server.level.ServerLevel;
import java.util.List;

public class HarvestBlockStateHandler implements IAgentStateHandler {

    @Override
    public void handle(AgentEntity agent) {
        BlockPos targetBlockPos = agent.getTargetBlockPos();
        AgentEntity.AgentMemory memory = agent.getMemory();

        if (targetBlockPos == null) {
            // MASONRY.LOGGER.warn("{} has no target block to harvest. Returning to IDLE.", agent.getName().getString());
            agent.setCurrentState(AgentState.IDLE);
            memory.resetTicksInState();
            return;
        }

        BlockState blockState = agent.level().getBlockState(targetBlockPos);
        if (blockState.isAir()) {
            // MASONRY.LOGGER.warn("{} target block at {} is air. Returning to IDLE.", agent.getName().getString(), targetBlockPos);
            agent.setTargetBlockPos(null);
            agent.setCurrentState(AgentState.IDLE);
            memory.resetTicksInState();
            return;
        }

        // MASONRY.LOGGER.info("{} is attempting to harvest block: {} at {}. Ticks in state: {}", agent.getName().getString(), blockState.getBlock().getName().getString(), targetBlockPos.toString(), memory.getTicksInCurrentState());

        // Check if enough time has passed to harvest the block
        if (memory.getTicksInCurrentState() < AgentEntity.TICKS_TO_HARVEST_BLOCK) {
            // MASONRY.LOGGER.debug("{} continuing to harvest block at {}. Ticks: {}/{}", agent.getName().getString(), targetBlockPos, memory.getTicksInCurrentState(), AgentEntity.TICKS_TO_HARVEST_BLOCK);
            // Agent needs more time to harvest, do nothing else this tick for harvesting logic
            // The agent will remain in HARVEST_BLOCK state, and ticksInCurrentState will be incremented by AgentEntity.tick()
            return; 
        }

        // MASONRY.LOGGER.info("{} has spent enough time, proceeding with harvest of block: {} at {}.", agent.getName().getString(), blockState.getBlock().getName().getString(), targetBlockPos.toString());

        // Simulate block breaking and getting drops
        if (agent.level() instanceof ServerLevel) {
            ServerLevel serverLevel = (ServerLevel) agent.level();
            List<ItemStack> drops = Block.getDrops(blockState, serverLevel, targetBlockPos, null, agent, agent.getMainHandItem());
            
            if (agent.level().destroyBlock(targetBlockPos, false, agent)) { // destroyBlock 'true' would drop items, 'false' means we handle it
                // MASONRY.LOGGER.info("{} successfully harvested block at {}.", agent.getName().getString(), targetBlockPos.toString());
                for (ItemStack drop : drops) {
                    boolean added = agent.getInventory().addItem(drop.copy()).isEmpty(); // addItem returns what couldn't be added
                    if (!added) {
                        // MASONRY.LOGGER.warn("{} inventory full, could not add {}.", agent.getName().getString(), drop.getDescriptionId());
                        // Optionally, drop the item in the world if inventory is full
                        agent.spawnAtLocation(drop, 0.5F);
                    }
                }
                agent.setTargetBlockPos(null); // Clear target
                agent.setCurrentState(AgentState.IDLE); // Or next logical state
            } else {
                // MASONRY.LOGGER.warn("{} failed to destroy block at {}. Block might be too hard or protected. Returning to IDLE.", agent.getName().getString(), targetBlockPos.toString());
                agent.setTargetBlockPos(null);
                agent.setCurrentState(AgentState.IDLE);
            }
        } else {
             // MASONRY.LOGGER.warn("{} cannot harvest block on client side. Returning to IDLE.", agent.getName().getString());
            agent.setTargetBlockPos(null);
            agent.setCurrentState(AgentState.IDLE);
        }
        memory.resetTicksInState();
    }
}
