package com.mas.masonry;

import com.mas.masonry.AgentEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
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

    public static void checkForDanger(AgentEntity agent, double radius, AgentEntity.AgentMemory memory) {
        List<LivingEntity> nearbyEntities = agent.level().getEntitiesOfClass(
                LivingEntity.class,
                agent.getBoundingBox().inflate(radius),
                entity -> entity != agent && isDangerous(entity, agent)
        );

        if (!nearbyEntities.isEmpty()) {
            memory.setDangerNearby(true);
            // Update fear level based on closest threat
            LivingEntity closestThreat = nearbyEntities.get(0);
            for (LivingEntity threat : nearbyEntities) {
                if (agent.distanceToSqr(threat) < agent.distanceToSqr(closestThreat)) {
                    closestThreat = threat;
                }
            }
            adjustFearLevel(agent, closestThreat, radius, memory);
        }
    }

    /**
     * Utility method to check for allies near the agent
     */
    public static void checkForAllies(AgentEntity agent, double radius, AgentEntity.AgentMemory memory) {
        List<AgentEntity> nearbyAllies = agent.level().getEntitiesOfClass(
                AgentEntity.class,
                agent.getBoundingBox().inflate(radius),
                ally -> ally != agent && ally.getType() == agent.getType()
        );

        if (!nearbyAllies.isEmpty()) {
            memory.setAllyNearby(true);
            // Optionally identify allies that need help
            for (AgentEntity ally : nearbyAllies) {
                if (ally.getMemory().getHealthPercent() < 50) {
                    memory.setTargetEntity(ally);
                    break;
                }
            }
        }
    }

    /**
     * Helper method to determine if an entity is dangerous
     */
    private static boolean isDangerous(LivingEntity entity, AgentEntity agent) {
        // Check if entity is hostile mob
        if (entity instanceof Monster) {
            return true;
        }

        // Check if it's a recent attacker
        if (entity == agent.getLastHurtByMob() &&
                (agent.level().getGameTime() - agent.getLastHurtByMobTimestamp()) < 200) {
            return true;
        }

        // Check if it's an armed player
        if (entity instanceof Player player) {
            return player.getMainHandItem().isDamageableItem() ||
                    player.getOffhandItem().isDamageableItem();
        }

        return false;
    }

    /**
     * Helper method to adjust fear level based on threat proximity
     */
    private static void adjustFearLevel(AgentEntity agent, LivingEntity threat,
                                        double radius, AgentEntity.AgentMemory memory) {
        double distance = agent.distanceTo(threat);
        int fearIncrease = (int)((radius - distance) / radius * 20);
        memory.setFearLevel(Math.min(100, memory.getFearLevel() + fearIncrease));
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

    /**
     * Goal for fleeing from danger
     */
    public static class FleeGoal extends Goal {
        private final AgentEntity agent;
        private final double speedModifier;
        private Vec3 escapePos;

        public FleeGoal(AgentEntity agent, double speedModifier) {
            this.agent = agent;
            this.speedModifier = speedModifier;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (agent.getCurrentState() != AgentEntity.AgentState.FLEE) {
                return false;
            }

            // Find nearest hostile entity
            List<LivingEntity> nearbyEntities = agent.level().getEntitiesOfClass(
                    LivingEntity.class,
                    agent.getBoundingBox().inflate(16.0),
                    entity -> entity != agent && isHostile(entity)
            );

            if (!nearbyEntities.isEmpty()) {
                // Find escape position away from danger
                LivingEntity threat = nearbyEntities.get(0);
                Vec3 awayDir = agent.position().subtract(threat.position()).normalize();
                escapePos = agent.position().add(awayDir.scale(10.0));
                return true;
            }

            return false;
        }

        @Override
        public boolean canContinueToUse() {
            return agent.getCurrentState() == AgentEntity.AgentState.FLEE &&
                    escapePos != null &&
                    agent.getMemory().getFearLevel() > 20;
        }

        @Override
        public void start() {
            if (escapePos != null) {
                agent.getNavigation().moveTo(escapePos.x, escapePos.y, escapePos.z, speedModifier);
            }
        }

        @Override
        public void stop() {
            agent.getNavigation().stop();
            escapePos = null;
        }

        private boolean isHostile(LivingEntity entity) {
            return entity instanceof Monster ||
                    entity == agent.getLastHurtByMob();
        }
    }

    /**
     * Goal for helping nearby allies
     */
    public static class HelpAllyGoal extends Goal {
        private final AgentEntity agent;
        private final double speedModifier;
        private final float searchRange;
        private static final TargetingConditions ALLY_PREDICATE = TargetingConditions.forNonCombat()
                .range(16.0D)
                .ignoreLineOfSight();
        private AgentEntity targetAlly;

        public HelpAllyGoal(AgentEntity agent, double speedModifier, float searchRange) {
            this.agent = agent;
            this.speedModifier = speedModifier;
            this.searchRange = searchRange;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (agent.getCurrentState() != AgentEntity.AgentState.HELP_ALLY) {
                return false;
            }

            // Find nearby ally that needs help (low health)
            List<AgentEntity> nearbyAllies = agent.level().getEntitiesOfClass(
                    AgentEntity.class,
                    agent.getBoundingBox().inflate(searchRange),
                    ally -> ally != agent &&
                            ALLY_PREDICATE.test(agent, ally) &&
                            ally.getMemory().getHealthPercent() < 50
            );

            if (!nearbyAllies.isEmpty()) {
                targetAlly = nearbyAllies.get(0);
                return true;
            }

            return false;
        }

        @Override
        public boolean canContinueToUse() {
            return agent.getCurrentState() == AgentEntity.AgentState.HELP_ALLY &&
                    targetAlly != null &&
                    targetAlly.isAlive() &&
                    agent.distanceTo(targetAlly) <= searchRange;
        }

        @Override
        public void start() {
            if (targetAlly != null) {
                agent.getNavigation().moveTo(targetAlly, speedModifier);
            }
        }

        @Override
        public void stop() {
            agent.getNavigation().stop();
            targetAlly = null;
        }

        @Override
        public void tick() {
            if (targetAlly != null) {
                // Move to ally position
                if (!agent.getNavigation().isInProgress()) {
                    agent.getNavigation().moveTo(targetAlly, speedModifier);
                }

                // If we're close enough, could implement healing or other help mechanics here
                if (agent.distanceTo(targetAlly) < 2.0) {
                    // Example: Share health or resources
                    if (agent.getHealth() > 10.0F && targetAlly.getHealth() < targetAlly.getMaxHealth()) {
                        targetAlly.heal(1.0F);
                    }
                }
            }
        }
    }
}