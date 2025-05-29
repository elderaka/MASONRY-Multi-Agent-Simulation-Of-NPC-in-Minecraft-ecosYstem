package com.mas.masonry.agent.states;

import com.mas.masonry.AgentEntity;
import com.mas.masonry.AgentEntity.AgentState;
import com.mas.masonry.MASONRY;
import com.mas.masonry.MASONRY.BlueprintBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.InteractionHand;

public class PlaceConstructionBlockStateHandler implements IAgentStateHandler {
    private static final double MAX_PLACEMENT_DISTANCE_SQR = 3.5 * 3.5; // Max distance to place a block

    @Override
    public void handle(AgentEntity agent) {
        AgentEntity.AgentMemory memory = agent.getMemory();
        memory.incrementTicksSinceLastBlockPlace(); // Increment cooldown timer each tick in this state

        if (agent.getConstructionOrigin() == null) {
            // MASONRY.LOGGER.info("{} initializing construction origin to current position.", agent.getName().getString());
            agent.setConstructionOrigin(agent.blockPosition());
        }

        if (MASONRY.SIMPLE_WALL_BLUEPRINT.isEmpty()) {
            // MASONRY.LOGGER.warn("{} cannot place construction block: Blueprint is empty. Transitioning to IDLE.", agent.getName().getString());
            agent.setCurrentState(AgentState.IDLE);
            memory.resetTicksInState();
            return;
        }

        int blueprintIndex = agent.getCurrentBlueprintIndex();
        if (blueprintIndex >= MASONRY.SIMPLE_WALL_BLUEPRINT.size()) {
            // MASONRY.LOGGER.info("{} has completed the blueprint! Transitioning to IDLE.", agent.getName().getString());
            agent.setCurrentState(AgentState.IDLE); // Or a new CELEBRATE state :)
            // agent.setConstructionOrigin(null); // Optionally reset for next construction
            // agent.setCurrentBlueprintIndex(0);
            memory.resetTicksInState();
            return;
        }

        BlueprintBlock currentBlueprintBlock = MASONRY.SIMPLE_WALL_BLUEPRINT.get(blueprintIndex);
        BlockPos targetPlacementPos = agent.getConstructionOrigin().offset(currentBlueprintBlock.relativePos);

        // Check if block is already there
        BlockState existingBlockState = agent.level().getBlockState(targetPlacementPos);
        if (existingBlockState.is(currentBlueprintBlock.blockType)) {
            // MASONRY.LOGGER.info("{} found block {} already at {}. Moving to next blueprint item.", agent.getName().getString(), currentBlueprintBlock.blockType.getName().getString(), targetPlacementPos);
            agent.incrementCurrentBlueprintIndex();
            memory.resetTicksInState();
            return;
        }
        // If it's not air and not the target block, it's obstructed. For now, we can't handle this well.
        if (!existingBlockState.isAir()) {
            // MASONRY.LOGGER.warn("{} found obstruction {} at {}. Cannot place {}. Transitioning to IDLE.", 
            //    agent.getName().getString(), existingBlockState.getBlock().getName().getString(), targetPlacementPos, currentBlueprintBlock.blockType.getName().getString());
            agent.setCurrentState(AgentState.IDLE); // Or a new CANNOT_BUILD state
            memory.resetTicksInState();
            return;
        }

        // Check distance to placement position
        if (agent.position().distanceToSqr(Vec3.atCenterOf(targetPlacementPos)) > MAX_PLACEMENT_DISTANCE_SQR) {
            // MASONRY.LOGGER.info("{} is too far from {}. Moving to target block for placement.", agent.getName().getString(), targetPlacementPos);
            agent.setTargetPos(Vec3.atCenterOf(targetPlacementPos));
            agent.setCurrentState(AgentState.MOVE_TO_TARGET_BLOCK);
            memory.resetTicksInState();
            return;
        }

        // Check inventory for the required block
        ItemStack requiredItemStack = new ItemStack(currentBlueprintBlock.blockType.asItem());
        if (!inventoryHasItem(agent.getInventory(), requiredItemStack)) {
            // MASONRY.LOGGER.info("{} does not have {}. Transitioning to FIND_TARGET_BLOCK.", agent.getName().getString(), currentBlueprintBlock.blockType.getName().getString());
            agent.setTargetBlockTypeToFind(currentBlueprintBlock.blockType);
            agent.setCurrentState(AgentState.FIND_TARGET_BLOCK);
            memory.resetTicksInState();
            return;
        }

        // Check placement cooldown
        if (memory.getTicksSinceLastBlockPlace() < AgentEntity.MIN_TICKS_BETWEEN_PLACEMENT) {
            // MASONRY.LOGGER.debug("{} waiting for placement cooldown. Ticks: {}/{}", agent.getName().getString(), memory.getTicksSinceLastBlockPlace(), AgentEntity.MIN_TICKS_BETWEEN_PLACEMENT);
            return; // Wait for cooldown, do nothing else this tick regarding placement
        }

        // Attempt to place the block
        // MASONRY.LOGGER.info("{} attempting to place {} at {}.", agent.getName().getString(), currentBlueprintBlock.blockType.getName().getString(), targetPlacementPos);
        // Simulate using the item. This is a simplified way to place a block.
        // A more robust way would involve using GameEvents, or specific item interaction logic.
        // For now, directly set the block state and remove from inventory.
        boolean placed = agent.level().setBlock(targetPlacementPos, currentBlueprintBlock.blockType.defaultBlockState(), 3);

        if (placed) {
            // MASONRY.LOGGER.info("{} successfully placed {} at {}.", agent.getName().getString(), currentBlueprintBlock.blockType.getName().getString(), targetPlacementPos);
            int slot = findSlotWithItem(agent.getInventory(), requiredItemStack);
            if (slot != -1) {
                agent.getInventory().removeItem(slot, 1); // Remove one item
            }
            agent.incrementCurrentBlueprintIndex();
            memory.resetTicksSinceLastBlockPlace(); // Reset cooldown after successful placement
        } else {
            // MASONRY.LOGGER.warn("{} failed to place {} at {}. Transitioning to IDLE.", agent.getName().getString(), currentBlueprintBlock.blockType.getName().getString(), targetPlacementPos);
            agent.setCurrentState(AgentState.IDLE); // Or retry logic
        }
        memory.resetTicksInState();
    }

    private static boolean inventoryHasItem(net.minecraft.world.SimpleContainer inventory, ItemStack itemStack) {
        for (int i = 0; i < inventory.getContainerSize(); ++i) {
            ItemStack slotStack = inventory.getItem(i);
            if (!slotStack.isEmpty() && ItemStack.isSameItem(slotStack, itemStack)) {
                return true;
            }
        }
        return false;
    }

    private static int findSlotWithItem(net.minecraft.world.SimpleContainer inventory, ItemStack itemStack) {
        for (int i = 0; i < inventory.getContainerSize(); ++i) {
            ItemStack slotStack = inventory.getItem(i);
            if (!slotStack.isEmpty() && ItemStack.isSameItem(slotStack, itemStack)) {
                return i;
            }
        }
        return -1;
    }
}
