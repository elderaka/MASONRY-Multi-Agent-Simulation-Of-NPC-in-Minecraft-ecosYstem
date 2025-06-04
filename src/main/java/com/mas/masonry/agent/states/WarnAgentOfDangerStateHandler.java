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

public class WarnAgentOfDangerStateHandler implements IAgentStateHandler {

    @Override
    public void handle(AgentEntity agent) {
        // Only log once when entering the state, not every tick
        if (agent.getMemory().getTicksInCurrentState() == 0) {
            MASONRY.LOGGER.debug("{} is in WARN_AGENT_OF_DANGER state.", agent.getBaseName());
        }

        // Implementation of warning logic:
        // 1. Get the danger that we want to warn about
        LivingEntity danger = agent.getMemory().getNearestDanger();
        if (danger == null) {
            // No danger to warn about anymore, go back to previous state
            agent.setCurrentState(AgentEntity.AgentState.IDLE);
            return;
        }
        
        // 2. Identify a nearby friendly agent to warn
        Optional<LivingEntity> targetOptional = agent.getMemory().getTargetEntity();
        
        if (targetOptional.isEmpty()) {
            // No target set, look for a nearby agent to warn
            LivingEntity nearestAgent = findNearestUnwarnedAgent(agent);
            
            if (nearestAgent != null) {
                agent.getMemory().setTargetEntity(nearestAgent);
            } else {
                // No agents to warn nearby, go back to FLEE or IDLE
                if (agent.getMemory().getFearLevel() > 50) {
                    agent.setCurrentState(AgentEntity.AgentState.FLEE);
                } else {
                    agent.setCurrentState(AgentEntity.AgentState.IDLE);
                }
                return;
            }
        }
        
        // Now we should have a target
        LivingEntity target = agent.getMemory().getTargetEntity().orElse(null);
        if (target == null || !(target instanceof AgentEntity) || !target.isAlive()) {
            // Target is invalid or no longer valid, go back to appropriate state
            if (agent.getMemory().getFearLevel() > 50) {
                agent.setCurrentState(AgentEntity.AgentState.FLEE);
            } else {
                agent.setCurrentState(AgentEntity.AgentState.IDLE);
            }
            return;
        }
        
        // 3. Look at and move toward the target agent if needed
        lookAtEntity(agent, target);
        
        double distanceSqr = agent.distanceToSqr(target);
        
        // If the danger is too close, don't approach target (prioritize safety)
        double dangerDistSqr = agent.distanceToSqr(danger);
        if (dangerDistSqr < 64.0) { // 8 blocks squared
            // Too close to danger, either warn from here or flee
            if (distanceSqr > 100.0) { // 10 blocks squared - too far to warn effectively
                // Too far to warn and danger is close, just flee
                agent.setCurrentState(AgentEntity.AgentState.FLEE);
                return;
            }
            // Otherwise, we're close enough to warn without moving closer
        } else if (distanceSqr > 64.0) { // 8 blocks squared
            // Not too close to danger, move closer to target to warn
            agent.getNavigation().moveTo(target, 1.0D);
            return; // Don't warn until we're closer
        }
        
        // We're close enough to warn
        // Only send the warning message once
        if (agent.getMemory().getTicksInCurrentState() == 10) { // Send warning quickly
            // 4. Send a chat message for warning
            Component warningMessage = AgentChatter.getAgentCommunicationMessage(agent, AgentEntity.CommunicationType.DANGER_WARNING, null);
            
            if (agent.level() instanceof ServerLevel && !agent.level().isClientSide) {
                MinecraftServer server = agent.level().getServer();
                if (server != null) {
                    server.getPlayerList().broadcastSystemMessage(warningMessage, false);
                }
            }
            
            // Record this communication and danger
            agent.getMemory().recordCommunication(target.getUUID(), AgentEntity.CommunicationType.DANGER_WARNING);
            agent.getMemory().recordDanger(danger.getType(), danger.position());
            
            // Alert the target agent about the danger directly
            if (target instanceof AgentEntity targetAgent) {
                targetAgent.getMemory().setDangerNearby(true);
                targetAgent.getMemory().setFearLevel(Math.max(targetAgent.getMemory().getFearLevel(), 40));
            }
        }
        
        // After warning, transition back to FLEE or IDLE
        if (agent.getMemory().getTicksInCurrentState() > 30) { // 1.5 seconds after warning
            // Go back to FLEE if still scared, otherwise IDLE
            if (agent.getMemory().getFearLevel() > 30) {
                agent.setCurrentState(AgentEntity.AgentState.FLEE);
            } else {
                agent.setCurrentState(AgentEntity.AgentState.IDLE);
            }
        }
    }
    
    /**
     * Finds the nearest agent that hasn't been warned about this danger
     */
    private LivingEntity findNearestUnwarnedAgent(AgentEntity agent) {
        double closestDistSqr = Double.MAX_VALUE;
        LivingEntity closestAgent = null;
        
        for (LivingEntity entity : agent.level().getEntitiesOfClass(
                LivingEntity.class, 
                agent.getBoundingBox().inflate(agent.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.FOLLOW_RANGE)),
                e -> e instanceof AgentEntity && e != agent)) {
            
            // Skip if we've recently warned this agent
            if (agent.getMemory().hasRecentlyCommunicated(entity)) {
                continue;
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