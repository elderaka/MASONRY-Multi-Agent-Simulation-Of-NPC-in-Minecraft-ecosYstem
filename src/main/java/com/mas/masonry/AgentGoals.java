package com.mas.masonry;

import com.mas.masonry.AgentEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

public class AgentGoals {

    /**
     * Utility method to check for resources near the agent
     */
    public static void checkForResources(AgentEntity agent, double radius, AgentEntity.AgentMemory memory) {
        // Check for dropped food items
        List<ItemEntity> items = agent.level().getEntitiesOfClass(
            ItemEntity.class,
            agent.getBoundingBox().inflate(radius),
            item -> isFoodItem(item.getItem())
        );
        
        if (!items.isEmpty()) {
            memory.setResourceNearby(true);
            return;
        }
        
        // Check for food-providing blocks
        int searchBlockRadius = (int) radius;
        BlockPos agentPos = agent.blockPosition();
        
        for (int x = -searchBlockRadius; x <= searchBlockRadius; x++) {
            for (int y = -3; y <= 3; y++) {
                for (int z = -searchBlockRadius; z <= searchBlockRadius; z++) {
                    BlockPos checkPos = agentPos.offset(x, y, z);
                    BlockState state = agent.level().getBlockState(checkPos);
                    
                    if (isFoodBlock(state)) {
                        memory.setResourceNearby(true);
                        return;
                    }
                }
            }
        }
    }
    
    /**
     * Utility method to check if an item is food
     */
    private static boolean isFoodItem(ItemStack stack) {
        return stack.is(Items.WHEAT) ||
               stack.is(Items.CARROT) ||
               stack.is(Items.POTATO) ||
               stack.is(Items.APPLE);
    }
    
    /**
     * Utility method to check if a block is a food source
     */
    private static boolean isFoodBlock(BlockState state) {
        return state.is(Blocks.WHEAT) || 
               state.is(Blocks.CARROTS) || 
               state.is(Blocks.POTATOES) || 
               state.is(Blocks.SWEET_BERRY_BUSH) ||
               state.is(Blocks.HAY_BLOCK);
    }

    /**
     * Goal for seeking resources (food, valuable items, etc.)
     */
    public static class SeekResourceGoal extends Goal {
        private final AgentEntity agent;
        private final double speedModifier;
        private final float searchRadius;
        private int searchDelay = 0;
        private Vec3 targetPos;

        public SeekResourceGoal(AgentEntity agent, double speedModifier, float searchRadius) {
            this.agent = agent;
            this.speedModifier = speedModifier;
            this.searchRadius = searchRadius;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (searchDelay > 0) {
                searchDelay--;
                return false;
            }

            // Only use if in SEEK_RESOURCE state
            if (agent.getCurrentState() != AgentEntity.AgentState.SEEK_RESOURCE) {
                return false;
            }

            // Look for food items or food blocks
            Optional<Vec3> resource = findNearestResource();
            if (resource.isPresent()) {
                targetPos = resource.get();
                return true;
            }

            // No resources found, try again after a delay
            searchDelay = 20;
            return false;
        }

        @Override
        public boolean canContinueToUse() {
            return agent.getCurrentState() == AgentEntity.AgentState.SEEK_RESOURCE && targetPos != null;
        }

        @Override
        public void start() {
            if (targetPos != null) {
                agent.getNavigation().moveTo(targetPos.x, targetPos.y, targetPos.z, speedModifier);
                agent.getMemory().setTargetLocation(targetPos);
            }
        }

        @Override
        public void stop() {
            agent.getNavigation().stop();
            targetPos = null;
            agent.getMemory().clearTargetLocation();
        }

        @Override
        public void tick() {
            // If we're close enough to the resource, simulate "collecting" it
            if (targetPos != null && agent.position().distanceTo(targetPos) < 1.5) {
                // Simulate finding food
                agent.getMemory().setHungerLevel(Math.max(0, agent.getMemory().getHungerLevel() - 25));
                System.out.println(agent.getName().getString() + " collected resource, hunger now: " + agent.getMemory().getHungerLevel());
                
                // Resource collected, clear target
                stop();
            } else if (targetPos != null && !agent.getNavigation().isInProgress()) {
                // If navigation stopped but we haven't reached target, try moving again
                agent.getNavigation().moveTo(targetPos.x, targetPos.y, targetPos.z, speedModifier);
            }
        }

        private Optional<Vec3> findNearestResource() {
            // First, check for dropped food items
            List<ItemEntity> items = agent.level().getEntitiesOfClass(
                ItemEntity.class,
                agent.getBoundingBox().inflate(searchRadius),
                item -> isFoodItem(item.getItem())
            );

            if (!items.isEmpty()) {
                ItemEntity nearest = items.getFirst();
                for (ItemEntity item : items) {
                    if (agent.distanceToSqr(item) < agent.distanceToSqr(nearest)) {
                        nearest = item;
                    }
                }
                agent.getMemory().setResourceNearby(true);
                return Optional.of(nearest.position());
            }

            // Next, check for food-providing blocks (crops, etc.)
            int searchBlockRadius = (int) searchRadius;
            BlockPos agentPos = agent.blockPosition();
            
            for (int x = -searchBlockRadius; x <= searchBlockRadius; x++) {
                for (int y = -3; y <= 3; y++) {
                    for (int z = -searchBlockRadius; z <= searchBlockRadius; z++) {
                        BlockPos checkPos = agentPos.offset(x, y, z);
                        BlockState state = agent.level().getBlockState(checkPos);
                        
                        if (isFoodBlock(state)) {
                            agent.getMemory().setResourceNearby(true);
                            return Optional.of(Vec3.atCenterOf(checkPos));
                        }
                    }
                }
            }
            
            return Optional.empty();
        }
    }
    
    // Rest of AgentGoals class remains the same
    // ...
}