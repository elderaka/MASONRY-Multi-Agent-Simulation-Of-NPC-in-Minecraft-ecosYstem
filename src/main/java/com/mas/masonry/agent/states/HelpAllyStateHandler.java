
package com.mas.masonry.agent.states;

import com.mas.masonry.AgentEntity;
import com.mas.masonry.MASONRY;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

public class HelpAllyStateHandler implements IAgentStateHandler {

    // Minimum ticks to stay in HELP_ALLY state
    private static final int MIN_HELP_TICKS = 100; // 5 seconds

    // Maximum ticks to try helping before giving up
    private static final int MAX_HELP_TICKS = 400; // 20 seconds

    // Distance squared that is considered "close enough" to ally
    private static final double CLOSE_ENOUGH_SQR = 16.0; // 4 blocks squared

    @Override
    public void handle(AgentEntity agent) {
        // Only log once when entering the state, not every tick
        if (agent.getMemory().getTicksInCurrentState() == 0) {
            MASONRY.LOGGER.debug("{} is trying to help an ally!", agent.getBaseName());
        }

        // Implementation of help logic:
        // 1. Get the ally that needs help
        Optional<LivingEntity> targetOptional = agent.getMemory().getTargetEntity();

        if (targetOptional.isEmpty()) {
            // No target set, look for a nearby ally that needs help
            LivingEntity allyToHelp = findNearestAllyNeedingHelp(agent);

            if (allyToHelp != null) {
                agent.getMemory().setTargetEntity(allyToHelp);
            } else {
                // No allies need help, go back to IDLE
                agent.setCurrentState(AgentEntity.AgentState.IDLE);
                return;
            }
        }

        // Now we should have a target ally
        LivingEntity ally = agent.getMemory().getTargetEntity().orElse(null);
        if (ally == null || !ally.isAlive()) {
            // Target is invalid or no longer alive, go back to IDLE
            agent.setCurrentState(AgentEntity.AgentState.IDLE);
            return;
        }

        // 2. Move toward the ally
        double distanceSqr = agent.distanceToSqr(ally);

        if (distanceSqr > CLOSE_ENOUGH_SQR) {
            // Still too far, keep moving toward ally
            agent.getNavigation().moveTo(ally, 1.0D);
        } else {
            // We're close enough to help!

            // Look at the ally
            lookAtEntity(agent, ally);

            // Check if the ally is still in danger
            boolean allyStillInDanger = false;
            if (ally instanceof AgentEntity allyAgent) {
                allyStillInDanger = allyAgent.getMemory().isDangerNearby() ||
                        allyAgent.getCurrentState() == AgentEntity.AgentState.FLEE;
            }

            // If ally is still in danger and we've been helping for at least MIN_HELP_TICKS,
            // look for the danger and attack it
            if (allyStillInDanger && agent.getMemory().getTicksInCurrentState() >= MIN_HELP_TICKS) {
                // Look for nearby dangers
                LivingEntity danger = findNearbyDanger(agent);

                if (danger != null) {
                    // Found the danger! Attack it
                    agent.getMemory().setAttackTarget(danger);
                    agent.setCurrentState(AgentEntity.AgentState.ATTACK);
                    return;
                }
            }

            // If we've been trying to help for too long, give up
            if (agent.getMemory().getTicksInCurrentState() >= MAX_HELP_TICKS) {
                // Increase apathy slightly - this experience made the agent less likely to help next time
                agent.getMemory().increaseApathyLevel(1);
                agent.setCurrentState(AgentEntity.AgentState.IDLE);
                return;
            }
        }
    }

    /**
     * Finds the nearest ally that might need help
     */
    private LivingEntity findNearestAllyNeedingHelp(AgentEntity agent) {
        double closestDistSqr = Double.MAX_VALUE;
        LivingEntity closestAlly = null;

        for (LivingEntity entity : agent.level().getEntitiesOfClass(
                LivingEntity.class,
                agent.getBoundingBox().inflate(agent.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.FOLLOW_RANGE)),
                e -> e instanceof AgentEntity && e != agent)) {

            // Check if this ally needs help
            boolean needsHelp = false;
            if (entity instanceof AgentEntity allyAgent) {
                needsHelp = allyAgent.getMemory().isDangerNearby() ||
                        allyAgent.getCurrentState() == AgentEntity.AgentState.FLEE;
            }

            if (!needsHelp) {
                continue;
            }

            double distSqr = agent.distanceToSqr(entity);
            if (distSqr < closestDistSqr) {
                closestDistSqr = distSqr;
                closestAlly = entity;
            }
        }

        return closestAlly;
    }

    /**
     * Finds a nearby danger that might be threatening the ally
     */
    private LivingEntity findNearbyDanger(AgentEntity agent) {
        for (LivingEntity entity : agent.level().getEntitiesOfClass(
                LivingEntity.class,
                agent.getBoundingBox().inflate(agent.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.FOLLOW_RANGE)),
                e -> agent.isEntityDangerous(e))) {

            // Just return the first dangerous entity found
            return entity;
        }

        return null;
    }

    /**
     * Makes the agent look at another entity
     */
    private void lookAtEntity(AgentEntity agent, LivingEntity target) {
        Vec3 targetPos = new Vec3(
                target.getX(),
                target.getEyeY(),
                target.getZ()
        );

        Vec3 agentPos = new Vec3(
                agent.getX(),
                agent.getEyeY(),
                agent.getZ()
        );

        Vec3 direction = targetPos.subtract(agentPos).normalize();
        double horizontalDistance = Math.sqrt(direction.x * direction.x + direction.z * direction.z);

        float yaw = (float) (Math.atan2(direction.z, direction.x) * 180.0 / Math.PI) - 90.0F;
        float pitch = (float) -(Math.atan2(direction.y, horizontalDistance) * 180.0 / Math.PI);

        agent.setYRot(yaw);
        agent.setXRot(pitch);
        agent.setYHeadRot(yaw);
    }
}