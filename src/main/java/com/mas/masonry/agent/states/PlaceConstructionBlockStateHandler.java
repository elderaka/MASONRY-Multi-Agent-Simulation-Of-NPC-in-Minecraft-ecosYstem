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
    // Increase maximum placement distance from 3.5 to 5.5 blocks (30.25 squared)
    private static final double MAX_PLACEMENT_DISTANCE_SQR = 5.5 * 5.5;

    @Override
    public void handle(AgentEntity agent) {
        AgentEntity.AgentMemory memory = agent.getMemory();
        memory.incrementTicksSinceLastBlockPlace(); // Increment cooldown timer each tick in this state

        if (agent.getConstructionOrigin() == null) {
            // MASONRY.LOGGER.info("{} initializing construction origin to current position.", agent.getName().getString());
            agent.setConstructionOrigin(agent.blockPosition());
        }

        if (MASONRY.SIMPLE_HUT_BLUEPRINT.isEmpty()) {
            // MASONRY.LOGGER.warn("{} cannot place construction block: Blueprint is empty. Transitioning to IDLE.", agent.getName().getString());
            agent.setCurrentState(AgentState.IDLE);
            memory.resetTicksInState();
            return;
        }

        int blueprintIndex = agent.getCurrentBlueprintIndex();
        if (blueprintIndex >= MASONRY.SIMPLE_HUT_BLUEPRINT.size()) {
            // MASONRY.LOGGER.info("{} has completed the blueprint! Transitioning to IDLE.", agent.getName().getString());
            agent.setCurrentState(AgentState.IDLE); // Or a new CELEBRATE state :)
            // agent.setConstructionOrigin(null); // Optionally reset for next construction
            // agent.setCurrentBlueprintIndex(0);
            memory.resetTicksInState();
            return;
        }

        BlueprintBlock currentBlueprintBlock = MASONRY.SIMPLE_HUT_BLUEPRINT.get(blueprintIndex);
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

            // Skip this block instead of giving up on construction entirely
            agent.incrementCurrentBlueprintIndex();
            memory.resetTicksInState();
            return;
        }

        // Calculate distance to placement position
        double distanceSqr = agent.position().distanceToSqr(Vec3.atCenterOf(targetPlacementPos));

        // Check distance to placement position with increased range
        if (distanceSqr > MAX_PLACEMENT_DISTANCE_SQR) {
            // MASONRY.LOGGER.info("{} is too far from {}. Moving to target block for placement.", agent.getName().getString(), targetPlacementPos);

            // Calculate a position near the target that's suitable for placement
            // This tries to find a spot that's not directly on the target but nearby
            Vec3 targetPos = getPlacementApproachPosition(agent, targetPlacementPos);
            agent.setTargetPos(targetPos);
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

        // Turn to face the target block
        lookAtBlock(agent, targetPlacementPos);

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
            // Attempt to place failed - try the next block instead of giving up
            agent.incrementCurrentBlueprintIndex();
            // MASONRY.LOGGER.warn("{} failed to place {} at {}. Trying next block.", agent.getName().getString(), currentBlueprintBlock.blockType.getName().getString(), targetPlacementPos);
        }
        memory.resetTicksInState();
    }

    /**
     * Makes the agent look at the target block position
     */
    private void lookAtBlock(AgentEntity agent, BlockPos targetPos) {
        Vec3 targetVec = Vec3.atCenterOf(targetPos);
        Vec3 agentPos = agent.position().add(0, agent.getEyeHeight(), 0);
        Vec3 direction = targetVec.subtract(agentPos).normalize();

        // Calculate yaw and pitch from the direction vector
        double horizontalDistance = Math.sqrt(direction.x * direction.x + direction.z * direction.z);
        float yaw = (float) (Math.atan2(direction.z, direction.x) * 180.0 / Math.PI) - 90.0F;
        float pitch = (float) -(Math.atan2(direction.y, horizontalDistance) * 180.0 / Math.PI);

        // Set entity rotation
        agent.setYRot(yaw);
        agent.setXRot(pitch);
        agent.setYHeadRot(yaw);
    }

    /**
     * Calculates a good position for the agent to stand for block placement
     */
    private Vec3 getPlacementApproachPosition(AgentEntity agent, BlockPos targetPos) {
        // Get a position that's a few blocks away from the target in the direction of the agent
        Vec3 targetVec = Vec3.atCenterOf(targetPos);
        Vec3 agentPos = agent.position();

        // Direction from target to agent (reversed so we approach from agent's side)
        Vec3 direction = agentPos.subtract(targetVec);

        // If agent is directly above/below the target, use a default horizontal direction
        if (Math.abs(direction.x) < 0.1 && Math.abs(direction.z) < 0.1) {
            direction = new Vec3(1, direction.y, 0);
        }

        // Normalize and scale to a good placement distance (about 3 blocks away)
        direction = direction.normalize().scale(3.0);

        // Create a position that's at the target position plus the offset
        Vec3 approachPos = targetVec.add(direction);

        // Ensure Y coordinate is reasonable (not floating or underground)
        // This is simplified and might need refinement based on terrain
        approachPos = new Vec3(approachPos.x, targetPos.getY(), approachPos.z);

        return approachPos;
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