package com.mas.masonry.agent.states;

import com.mas.masonry.AgentChatter;
import com.mas.masonry.AgentEntity;
import com.mas.masonry.MASONRY;
import com.mas.masonry.agent.systems.ResourceRequestSystem;
import net.minecraft.network.chat.Component;
import java.util.List;

public class ConsiderResourceRequestStateHandler implements IAgentStateHandler {
    
    @Override
    public void handle(AgentEntity agent) {
        // Send thinking message
        if (agent.getMemory().getTicksInCurrentState() == 0) {
            Component thinkingMessage = AgentChatter.getFormattedChatMessage(
                AgentEntity.AgentState.CONSIDER_RESOURCE_REQUEST, 
                Component.literal(agent.getBaseName())
            );
            agent.sendChatMessage(thinkingMessage);
        }
        
        // Check for incoming resource requests
        List<ResourceRequestSystem.PendingRequest> pendingRequests = 
            agent.getResourceRequestSystem().getPendingRequestsForAgent(agent.getUUID());
        
        if (pendingRequests.isEmpty()) {
            // No requests to consider
            agent.setCurrentState(AgentEntity.AgentState.IDLE);
            return;
        }
        
        // Consider the first request (take some time to "think")
        if (agent.getMemory().getTicksInCurrentState() < 40) { // 2 seconds thinking time
            return;
        }
        
        ResourceRequestSystem.PendingRequest request = pendingRequests.get(0);
        
        MASONRY.LOGGER.debug("{} is considering a request for {} from requester", 
            agent.getBaseName(), request.item.getDescription().getString());
        
        // Check if we can and want to fulfill this request
        boolean canFulfill = agent.getResourceRequestSystem().canFulfillRequest(agent, request.item, request.quantity);
        
        if (canFulfill) {
            // Accept the request
            boolean accepted = agent.getResourceRequestSystem().acceptRequest(agent, getRequestId(request));
            
            if (accepted) {
                MASONRY.LOGGER.debug("{} accepted resource request for {}", 
                    agent.getBaseName(), request.item.getDescription().getString());
                
                // Send acceptance message
                Component acceptMessage = AgentChatter.getResourceChatMessage(
                    agent, "resource_request_accepted", request.item, null);
                agent.sendChatMessage(acceptMessage);
                
                agent.setCurrentState(AgentEntity.AgentState.GIVE_ITEM_TO_AGENT);
            } else {
                agent.setCurrentState(AgentEntity.AgentState.IDLE);
            }
        } else {
            // Decline the request
            agent.getResourceRequestSystem().declineRequest(agent, getRequestId(request));
            MASONRY.LOGGER.debug("{} declined resource request for {}", 
                agent.getBaseName(), request.item.getDescription().getString());
            
            // Send decline message
            Component declineMessage = AgentChatter.getResourceChatMessage(
                agent, "resource_request_declined", request.item, null);
            agent.sendChatMessage(declineMessage);
            
            agent.setCurrentState(AgentEntity.AgentState.IDLE);
        }
    }
    
    private java.util.UUID getRequestId(ResourceRequestSystem.PendingRequest request) {
        // Generate consistent ID from request properties
        return java.util.UUID.nameUUIDFromBytes(
            (request.requesterId.toString() + request.item.toString() + request.requestTime).getBytes()
        );
    }
}