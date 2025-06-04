package com.mas.masonry.agent.states;

import com.mas.masonry.AgentChatter;
import com.mas.masonry.AgentEntity;
import com.mas.masonry.MASONRY;
import com.mas.masonry.agent.systems.ResourceRequestSystem;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import java.util.List;
import java.util.UUID;

public class GiveItemToAgentStateHandler implements IAgentStateHandler {

    private static final double DELIVERY_RANGE = 3.0;

    @Override
    public void handle(AgentEntity agent) {
        if (agent.getMemory().getTicksInCurrentState() == 0) {
            MASONRY.LOGGER.debug("{} is giving items to another agent", agent.getBaseName());
        }

        // Check for accepted requests that need delivery
        List<ResourceRequestSystem.PendingRequest> acceptedRequests =
                agent.getResourceRequestSystem().getAcceptedRequestsForAgent(agent.getUUID());

        if (acceptedRequests.isEmpty()) {
            // No deliveries to make
            agent.setCurrentState(AgentEntity.AgentState.IDLE);
            return;
        }

        // Get the first accepted request
        ResourceRequestSystem.PendingRequest request = acceptedRequests.get(0);

        // Find the requesting agent
        AgentEntity requester = findAgentById(agent, request.requesterId);
        if (requester == null) {
            // Requester not found, abandon this request
            agent.getResourceRequestSystem().declineRequest(agent,
                    getRequestId(agent, request));
            return;
        }

        // Move to the requester
        double distance = agent.distanceTo(requester);
        if (distance > DELIVERY_RANGE) {
            agent.getNavigation().moveTo(requester, 1.2);

            // Send encouraging message while traveling
            if (agent.getMemory().getTicksInCurrentState() % 400 == 0) {
                Component travelMessage = AgentChatter.getFormattedChatMessage(
                        AgentEntity.AgentState.DELIVER_RESOURCE_TO_AGENT,
                        Component.literal(agent.getBaseName()),
                        request.item,
                        requester.getBaseName()
                );
                agent.sendChatMessage(travelMessage);
            }

            // Give up if taking too long
            if (agent.getMemory().getTicksInCurrentState() > 1200) { // 1 minute
                agent.setCurrentState(AgentEntity.AgentState.IDLE);
            }
            return;
        }

        // Close enough to deliver
        agent.getNavigation().stop();

        // Look at each other
        lookAtTarget(agent, requester);
        lookAtTarget(requester, agent);

        // Send delivery message
        Component deliveryMessage = AgentChatter.getResourceChatMessage(
                agent, "give_item_to_agent", request.item, requester.getBaseName());
        agent.sendChatMessage(deliveryMessage);

        // Complete the delivery
        UUID requestId = getRequestId(agent, request);
        boolean success = agent.getResourceRequestSystem().completeRequest(agent, requester, requestId);

        if (success) {
            MASONRY.LOGGER.debug("{} successfully delivered {} to {}",
                    agent.getBaseName(), request.item.getDescription().getString(), requester.getBaseName());

            // Increase social meter for both agents
            agent.getMemory().increaseSocialMeter(5);
            requester.getMemory().increaseSocialMeter(5);

            // Record positive communication
            agent.getMemory().recordCommunication(requester);
            requester.getMemory().recordCommunication(agent);

        } else {
            MASONRY.LOGGER.debug("{} failed to deliver items to {}",
                    agent.getBaseName(), requester.getBaseName());
        }

        agent.setCurrentState(AgentEntity.AgentState.IDLE);
    }

    private AgentEntity findAgentById(AgentEntity searcher, UUID targetId) {
        return searcher.level().getEntitiesOfClass(AgentEntity.class,
                        searcher.getBoundingBox().inflate(64.0))
                .stream()
                .filter(agent -> agent.getUUID().equals(targetId))
                .findFirst()
                .orElse(null);
    }

    private UUID getRequestId(AgentEntity agent, ResourceRequestSystem.PendingRequest request) {
        // This is a simplification - in a real implementation, you'd want to track request IDs properly
        return UUID.nameUUIDFromBytes((request.requesterId.toString() + request.item.toString()).getBytes());
    }

    private void lookAtTarget(AgentEntity agent, AgentEntity target) {
        double dx = target.getX() - agent.getX();
        double dz = target.getZ() - agent.getZ();
        float yaw = (float) (Math.atan2(dz, dx) * 180.0 / Math.PI) - 90.0F;
        agent.setYRot(yaw);
        agent.setYHeadRot(yaw);
    }
}