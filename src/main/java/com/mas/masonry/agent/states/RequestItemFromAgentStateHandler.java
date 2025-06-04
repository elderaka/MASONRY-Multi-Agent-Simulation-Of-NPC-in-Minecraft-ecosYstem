package com.mas.masonry.agent.states;

import com.mas.masonry.AgentChatter;
import com.mas.masonry.AgentEntity;
import com.mas.masonry.MASONRY;
import com.mas.masonry.agent.systems.ResourceRequestSystem;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;

public class RequestItemFromAgentStateHandler implements IAgentStateHandler {

    private static final int MAX_REQUEST_ATTEMPTS = 3;
    private static final int REQUEST_COOLDOWN = 600; // 30 seconds between requests

    @Override
    public void handle(AgentEntity agent) {
        AgentEntity targetAgent;

        if (!agent.getMemory().getTargetEntity().isPresent()) {
            targetAgent = findNearestHelpfulAgent(agent);
            if (targetAgent == null) {
                agent.setCurrentState(AgentEntity.AgentState.IDLE);
                return;
            }
            agent.getMemory().setTargetEntity(targetAgent);
        } else {
            targetAgent = (AgentEntity) agent.getMemory().getTargetEntity().get();
        }

        if (agent.getMemory().getTicksInCurrentState() == 0) {
            MASONRY.LOGGER.debug("{} is requesting items from other agents", agent.getBaseName());
        }

        // Check if we're close enough to make the request
        double distance = agent.distanceTo(targetAgent);



        if (distance > 8.0) {
            // Move closer
            agent.getNavigation().moveTo(targetAgent, 1.0);

            // Give up if target is too far or unreachable
            if (agent.getMemory().getTicksInCurrentState() > 600) { // 30 seconds
                agent.setCurrentState(AgentEntity.AgentState.SEEK_RESOURCE);
            }
            return;
        }

        // Close enough, make the request
        if (agent.getMemory().getTicksInCurrentState() % REQUEST_COOLDOWN == 0) {
            boolean requestMade = makeResourceRequest(agent, targetAgent);

            if (requestMade) {
                MASONRY.LOGGER.debug("{} made a resource request to {}",
                    agent.getBaseName(), targetAgent.getBaseName());
                agent.setCurrentState(AgentEntity.AgentState.WAIT_FOR_RESOURCE_DELIVERY);
            } else {
                // Request failed, try someone else or give up
                agent.setCurrentState(AgentEntity.AgentState.SEEK_RESOURCE);
            }
        }
    }

    private AgentEntity findNearestHelpfulAgent(AgentEntity requester) {
        return requester.level().getEntitiesOfClass(AgentEntity.class,
            requester.getBoundingBox().inflate(32.0))
            .stream()
            .filter(agent -> agent != requester)
            .filter(agent -> !requester.getMemory().hasRecentlyCommunicated(agent))
            .min((a1, a2) -> Double.compare(requester.distanceTo(a1), requester.distanceTo(a2)))
            .orElse(null);
    }

    private boolean makeResourceRequest(AgentEntity requester, AgentEntity target) {
        ResourceRequestSystem requestSystem = requester.getResourceRequestSystem();

        // Determine what item to request based on current need
        Item neededItem = determineNeededItem(requester);
        if (neededItem == null) {
            return false;
        }

        // Send chat message for the request
        Component chatMessage = AgentChatter.getResourceChatMessage(
            requester, "request_item_from_agent", neededItem, target.getBaseName());
        requester.sendChatMessage(chatMessage);

        // Make the request
        boolean success = requestSystem.sendRequestToAgent(requester, target, neededItem, 1);

        if (success) {
            // Record communication
            requester.getMemory().recordCommunication(target);

            // Make agents look at each other
            lookAtTarget(requester, target);
            lookAtTarget(target, requester);
        }

        return success;
    }

    private Item determineNeededItem(AgentEntity agent) {
        // Check what items are needed for current construction
        if (agent.getCurrentBlueprintIndex() < MASONRY.SIMPLE_HUT_BLUEPRINT.size()) {
            return MASONRY.SIMPLE_HUT_BLUEPRINT.get(agent.getCurrentBlueprintIndex()).blockType.asItem();
        }

        // Check profession-related needs
        if (agent.getProfessionSystem().hasProfession()) {
            Item[] professionItems = agent.getProfessionSystem().getCurrentProfession().getProfessionItems();
            for (Item item : professionItems) {
                if (getItemCount(agent, item) == 0) {
                    return item;
                }
            }
        }

        return null;
    }

    private int getItemCount(AgentEntity agent, Item item) {
        int count = 0;
        for (int i = 0; i < agent.getInventory().getContainerSize(); i++) {
            if (agent.getInventory().getItem(i).getItem() == item) {
                count += agent.getInventory().getItem(i).getCount();
            }
        }
        return count;
    }

    private void lookAtTarget(AgentEntity agent, AgentEntity target) {
        double dx = target.getX() - agent.getX();
        double dz = target.getZ() - agent.getZ();
        float yaw = (float) (Math.atan2(dz, dx) * 180.0 / Math.PI) - 90.0F;
        agent.setYRot(yaw);
        agent.setYHeadRot(yaw);
    }
}