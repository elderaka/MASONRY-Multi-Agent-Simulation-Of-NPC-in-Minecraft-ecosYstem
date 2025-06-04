package com.mas.masonry.agent.states;

import com.mas.masonry.AgentEntity;
import com.mas.masonry.MASONRY;
import com.mas.masonry.AgentChatter;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.MinecraftServer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

public class GreetAgentStateHandler implements IAgentStateHandler {

    // Minimum time to stay in greeting state (10 seconds)
    private static final int MIN_GREETING_TICKS = 200;

    // Maximum time to try to approach before giving up (10 seconds)
    private static final int MAX_APPROACH_TICKS = 200;

    // Distance squared for close interaction (2 blocks squared = 4.0)
    private static final double CLOSE_INTERACTION_DISTANCE_SQR = 4.0;

    @Override
    public void handle(AgentEntity agent) {
        if (agent.getMemory().getTicksInCurrentState() == 0) {
            MASONRY.LOGGER.debug("{} is in GREET_AGENT state.", agent.getBaseName());
        }

        // Implementation of greeting logic:
        // 1. Identify a nearby friendly agent to greet.
        Optional<LivingEntity> targetOptional = agent.getMemory().getTargetEntity();

        if (targetOptional.isEmpty()) {
            // No target set, look for a nearby agent to greet
            LivingEntity nearestAgent = findNearestUngreetedAgent(agent);

            if (nearestAgent != null) {
                // Extra safety check to prevent self-targeting
                if (nearestAgent == agent) {
                    MASONRY.LOGGER.debug("{} attempted to target itself, ignoring", agent.getBaseName());
                    agent.setCurrentState(AgentEntity.AgentState.IDLE);
                    return;
                }
                agent.getMemory().setTargetEntity(nearestAgent);
                MASONRY.LOGGER.debug("{} found target to greet: {}",
                        agent.getBaseName(),
                        nearestAgent instanceof AgentEntity ? ((AgentEntity)nearestAgent).getBaseName() : nearestAgent.getName().getString());
            } else {
                // No ungreeted agents nearby, go back to IDLE
                MASONRY.LOGGER.debug("{} found no ungreeted agents nearby, returning to IDLE.", agent.getBaseName());
                agent.setCurrentState(AgentEntity.AgentState.IDLE);
                return;
            }
        }

        // Now we should have a target
        LivingEntity target = agent.getMemory().getTargetEntity().orElse(null);
        if (target == null || !(target instanceof AgentEntity) || !target.isAlive() || target == agent) {
            // Target is invalid, the same as the agent, or no longer valid, go back to IDLE
            MASONRY.LOGGER.debug("{} has invalid target or is targeting self, returning to IDLE.", agent.getBaseName());
            agent.setCurrentState(AgentEntity.AgentState.IDLE);
            agent.getMemory().clearTargetEntity();
            return;
        }


        // Calculate distance to target
        double distanceSqr = agent.distanceToSqr(target);

        // Check if we've been trying to approach for too long
        if (agent.getMemory().getTicksInCurrentState() > MAX_APPROACH_TICKS &&
                distanceSqr > CLOSE_INTERACTION_DISTANCE_SQR) {
            MASONRY.LOGGER.debug("{} gave up approaching {} after timeout, returning to IDLE.",
                    agent.getBaseName(),
                    target instanceof AgentEntity ? ((AgentEntity)target).getBaseName() : target.getName().getString());

            agent.setCurrentState(AgentEntity.AgentState.IDLE);
            agent.getMemory().clearTargetEntity();
            return;
        }

        // If the target is too far away, move toward them until in close greeting range
        if (distanceSqr > CLOSE_INTERACTION_DISTANCE_SQR) {
            // We're still approaching - move toward target
            agent.getNavigation().moveTo(target, 1.0D);

            // Log movement occasionally
            if (agent.getMemory().getTicksInCurrentState() % 40 == 0) {
                MASONRY.LOGGER.debug("{} moving toward {} to greet, distance: {}",
                        agent.getBaseName(),
                        target instanceof AgentEntity ? ((AgentEntity)target).getBaseName() : target.getName().getString(),
                        Math.sqrt(distanceSqr));
            }
        } else {
            // We're close enough to greet - stop moving
            agent.getNavigation().stop();

            // Look at the target agent
            lookAtEntity(agent, target);

            // Only send the greeting message once after getting close
            if (agent.getMemory().getTicksInCurrentState() % 20 == 0 &&
                    !agent.getMemory().hasRecentlyCommunicated(target)) {

                // Send a chat message for greeting
                String targetName = target instanceof AgentEntity ? ((AgentEntity)target).getBaseName() : target.getName().getString();
                Component greetingMessage = AgentChatter.getAgentCommunicationMessage(agent, AgentEntity.CommunicationType.GREETING, targetName);

                if (agent.level() instanceof ServerLevel && !agent.level().isClientSide) {
                    MinecraftServer server = agent.level().getServer();
                    if (server != null) {
                        server.getPlayerList().broadcastSystemMessage(greetingMessage, false);
                    }
                }

                // Record this greeting to prevent repeats
                agent.getMemory().recordCommunication(target.getUUID(), AgentEntity.CommunicationType.GREETING);

                // Reset the timer when they start greeting properly (got close enough)
                agent.getMemory().resetTicksInState();

                // Make the target agent also greet back, if it's an agent and not already in a social state
                if (target instanceof AgentEntity targetAgent) {
                    AgentEntity.AgentState targetState = targetAgent.getCurrentState();

                    if (targetState != AgentEntity.AgentState.GREET_AGENT &&
                            targetState != AgentEntity.AgentState.CHAT_WITH_AGENT &&
                            targetState != AgentEntity.AgentState.FLEE &&
                            targetState != AgentEntity.AgentState.ATTACK) {

                        // Set this agent as the target's target (for replying)
                        targetAgent.getMemory().setTargetEntity(agent);

                        // Make the target agent also enter the greeting state
                        MASONRY.LOGGER.debug("{} initiating greeting response from {}", agent.getBaseName(), targetAgent.getBaseName());
                        targetAgent.setCurrentState(AgentEntity.AgentState.GREET_AGENT);
                    }
                }
            }

            // After greeting for the minimum time, consider transitioning to another state
            if (agent.getMemory().getTicksInCurrentState() > MIN_GREETING_TICKS) { // 10 seconds of greeting
                // Transition to another state after greeting
                agent.getMemory().increaseSocialMeter(5); // Increase social meter after successful greeting

                // Record this as a completed social interaction for both agents
                agent.getMemory().resetSocialCooldown();

                if (target instanceof AgentEntity targetAgent) {
                    targetAgent.getMemory().resetSocialCooldown();

                    // Calculate if agents should chat based on their social and apathy metrics
                    boolean shouldChat = decideToChatBasedOnPersonality(agent, targetAgent);

                    if (shouldChat &&
                            targetAgent.getCurrentState() != AgentEntity.AgentState.FLEE &&
                            targetAgent.getCurrentState() != AgentEntity.AgentState.ATTACK) {

                        MASONRY.LOGGER.debug("{} and {} decided to chat based on personality metrics",
                                agent.getBaseName(), targetAgent.getBaseName());

                        // Both agents transition to chat state
                        agent.setCurrentState(AgentEntity.AgentState.CHAT_WITH_AGENT);

                        // Reset ticks in state when transitioning to chat
                        agent.getMemory().resetTicksInState();

                        // Make sure target also enters chat state if not already in it
                        if (targetAgent.getCurrentState() != AgentEntity.AgentState.CHAT_WITH_AGENT) {
                            targetAgent.getMemory().setTargetEntity(agent);
                            targetAgent.setCurrentState(AgentEntity.AgentState.CHAT_WITH_AGENT);
                            targetAgent.getMemory().resetTicksInState();
                        }
                    } else {
                        MASONRY.LOGGER.debug("{} and {} decided not to chat, returning to IDLE",
                                agent.getBaseName(), targetAgent.getBaseName());

                        // Both agents return to idle state
                        agent.setCurrentState(AgentEntity.AgentState.IDLE);
                        agent.getMemory().clearTargetEntity();

                        // If target is still in GREET_AGENT state, transition them to IDLE as well
                        if (targetAgent.getCurrentState() == AgentEntity.AgentState.GREET_AGENT) {
                            targetAgent.setCurrentState(AgentEntity.AgentState.IDLE);
                            targetAgent.getMemory().clearTargetEntity();
                        }
                    }
                } else {
                    // If not interacting with an agent, just go back to IDLE
                    MASONRY.LOGGER.debug("{} returning to IDLE after greeting {}.", agent.getBaseName(), target.getName().getString());
                    agent.setCurrentState(AgentEntity.AgentState.IDLE);
                    agent.getMemory().clearTargetEntity();
                }
            }
        }
    }


    /**
     * Determines if agents should chat based on their personality metrics
     * @param agent The first agent
     * @param targetAgent The second agent
     * @return true if they should chat, false otherwise
     */
    private boolean decideToChatBasedOnPersonality(AgentEntity agent, AgentEntity targetAgent) {
        // Get social and apathy values for both agents
        int agentSocial = agent.getMemory().getSocialMeter();
        int targetSocial = targetAgent.getMemory().getSocialMeter();
        int agentApathy = agent.getMemory().getApathyLevel();
        int targetApathy = targetAgent.getMemory().getApathyLevel();

        // Calculate a base chance based on both agents' social meters
        // Higher social = higher chance to chat
        double socialFactor = (agentSocial + targetSocial) / 200.0; // Average social / 100

        // Calculate a reduction factor based on both agents' apathy levels
        // Higher apathy = lower chance to chat
        double apathyFactor = (agentApathy + targetApathy) / 60.0; // Average apathy / 30

        // INCREASED BASE CHANCE: from 0.2 to 0.6 to make agents much more likely to chat
        double chatChance = 0.6 + (socialFactor * 0.3) - (apathyFactor * 0.3);

        // Ensure chance is within reasonable bounds
        // INCREASED minimum chance from 5% to 40%
        // INCREASED maximum chance from 80% to 95%
        chatChance = Math.max(0.4, Math.min(0.95, chatChance));

        // Roll the dice
        boolean result = agent.getRandom().nextDouble() < chatChance;

        MASONRY.LOGGER.debug("Chat decision for {} and {}: social={},{} apathy={},{} chance={} result={}",
                agent.getBaseName(), targetAgent.getBaseName(),
                agentSocial, targetSocial, agentApathy, targetApathy,
                String.format("%.2f", chatChance), result);

        return result;
    }
    /**
     * Finds the nearest agent that hasn't been greeted recently
     */
    private LivingEntity findNearestUngreetedAgent(AgentEntity agent) {
        double closestDistSqr = Double.MAX_VALUE;
        LivingEntity closestAgent = null;

        for (LivingEntity entity : agent.level().getEntitiesOfClass(
                LivingEntity.class,
                agent.getBoundingBox().inflate(agent.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.FOLLOW_RANGE)),
                e -> e instanceof AgentEntity && e != agent)) {

            // Skip if we've recently communicated with this entity
            if (agent.getMemory().hasRecentlyCommunicated(entity)) {
                continue;
            }

            // Skip if the entity is in FLEE or ATTACK state
            if (entity instanceof AgentEntity targetAgent) {
                AgentEntity.AgentState targetState = targetAgent.getCurrentState();
                if (targetState == AgentEntity.AgentState.FLEE || targetState == AgentEntity.AgentState.ATTACK) {
                    continue;
                }
            }

            double distSqr = agent.distanceToSqr(entity);
            if (distSqr < closestDistSqr) {
                closestDistSqr = distSqr;
                closestAgent = entity;
            }
        }

        return closestAgent;
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