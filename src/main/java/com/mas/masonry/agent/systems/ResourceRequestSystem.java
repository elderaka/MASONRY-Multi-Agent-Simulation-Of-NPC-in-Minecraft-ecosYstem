package com.mas.masonry.agent.systems;

import com.mas.masonry.AgentEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.BlockPos;
import java.util.*;

/**
 * Manages resource requests and sharing between agents
 */
public class ResourceRequestSystem {
    private final Map<Item, Integer> neededItems = new HashMap<>();
    private final Map<UUID, PendingRequest> pendingRequests = new HashMap<>();
    private final List<CompletedRequest> requestHistory = new ArrayList<>();
    private final Map<UUID, Integer> helpfulness = new HashMap<>(); // Track how helpful other agents are

    // Constants
    private static final int MAX_REQUEST_DISTANCE = 32;
    private static final int REQUEST_TIMEOUT_TICKS = 1200; // 1 minute
    private static final int MAX_PENDING_REQUESTS = 5;
    private static final int MAX_HISTORY_SIZE = 20;

    public static class PendingRequest {
        public final UUID requesterId;
        public final UUID targetAgentId;
        public final Item item;
        public final int quantity;
        public final long requestTime;
        public final BlockPos requestLocation;
        public boolean accepted = false;
        public boolean completed = false;

        public PendingRequest(UUID requesterId, UUID targetAgentId, Item item, int quantity, long requestTime, BlockPos location) {
            this.requesterId = requesterId;
            this.targetAgentId = targetAgentId;
            this.item = item;
            this.quantity = quantity;
            this.requestTime = requestTime;
            this.requestLocation = location;
        }

        public boolean isExpired(long currentTime) {
            return (currentTime - requestTime) > REQUEST_TIMEOUT_TICKS;
        }
    }

    public static class CompletedRequest {
        public final UUID helperId;
        public final Item item;
        public final int quantity;
        public final long completionTime;
        public final boolean wasHelpful;

        public CompletedRequest(UUID helperId, Item item, int quantity, long completionTime, boolean wasHelpful) {
            this.helperId = helperId;
            this.item = item;
            this.quantity = quantity;
            this.completionTime = completionTime;
            this.wasHelpful = wasHelpful;
        }
    }

    /**
     * Request an item from nearby agents
     */
    public boolean requestItemFromNearbyAgents(AgentEntity requester, Item item, int quantity) {
        if (pendingRequests.size() >= MAX_PENDING_REQUESTS) {
            return false; // Too many pending requests
        }

        // Add to needed items list
        neededItems.put(item, neededItems.getOrDefault(item, 0) + quantity);

        // Find nearby agents who might have the item
        List<AgentEntity> nearbyAgents = findNearbyAgentsWithItem(requester, item, quantity);

        if (nearbyAgents.isEmpty()) {
            return false;
        }

        // Sort by helpfulness (most helpful first)
        nearbyAgents.sort((a, b) -> Integer.compare(
                getHelpfulness(b.getUUID()),
                getHelpfulness(a.getUUID())
        ));

        // Send request to the most promising agent
        AgentEntity targetAgent = nearbyAgents.get(0);
        return sendRequestToAgent(requester, targetAgent, item, quantity);
    }

    /**
     * Send a specific request to a target agent
     */
    public boolean sendRequestToAgent(AgentEntity requester, AgentEntity targetAgent, Item item, int quantity) {
        UUID requestId = UUID.randomUUID();
        PendingRequest request = new PendingRequest(
                requester.getUUID(),
                targetAgent.getUUID(),
                item,
                quantity,
                requester.level().getGameTime(),
                requester.blockPosition()
        );

        pendingRequests.put(requestId, request);

        // Set the target agent to consider this request
        targetAgent.getResourceRequestSystem().receiveRequest(requestId, request);

        // Set requester's state to wait
        requester.getMemory().setTargetEntity(targetAgent);

        return true;
    }

    /**
     * Receive a request from another agent
     */
    public void receiveRequest(UUID requestId, PendingRequest request) {
        // Store the request for consideration
        pendingRequests.put(requestId, request);
    }

    /**
     * Check if this agent can fulfill a request
     */
    public boolean canFulfillRequest(AgentEntity agent, Item item, int quantity) {
        // Check inventory
        int availableQuantity = getAvailableQuantity(agent, item);

        if (availableQuantity < quantity) {
            return false;
        }

        // Check willingness based on:
        // 1. Social meter (more social = more willing to help)
        // 2. Profession (related profession items are shared more readily)
        // 3. Village membership (village members help each other more)

        int socialMeter = agent.getMemory().getSocialMeter();
        int baseWillingness = socialMeter;

        // Profession bonus - willing to share profession-related items
        if (agent.getProfessionSystem().hasProfession()) {
            for (Item profItem : agent.getProfessionSystem().getCurrentProfession().getProfessionItems()) {
                if (profItem == item) {
                    baseWillingness += 30;
                    break;
                }
            }
        }

        // Random factor
        int randomFactor = agent.getRandom().nextInt(50);

        return (baseWillingness + randomFactor) > 60;
    }

    /**
     * Accept a pending request
     */
    public boolean acceptRequest(AgentEntity agent, UUID requestId) {
        PendingRequest request = pendingRequests.get(requestId);
        if (request == null || request.accepted || request.completed) {
            return false;
        }

        if (!canFulfillRequest(agent, request.item, request.quantity)) {
            return false;
        }

        request.accepted = true;

        // Set agent's target to go deliver the item
        agent.getMemory().setTargetLocation(net.minecraft.world.phys.Vec3.atCenterOf(request.requestLocation));

        return true;
    }

    /**
     * Complete a request by giving items
     */
    public boolean completeRequest(AgentEntity giver, AgentEntity receiver, UUID requestId) {
        PendingRequest request = pendingRequests.get(requestId);
        if (request == null || !request.accepted || request.completed) {
            return false;
        }

        // Transfer items
        boolean success = transferItems(giver, receiver, request.item, request.quantity);

        if (success) {
            request.completed = true;

            // Update helpfulness tracking
            increaseHelpfulness(giver.getUUID(), request.quantity);

            // Add to history
            addToHistory(new CompletedRequest(
                    giver.getUUID(),
                    request.item,
                    request.quantity,
                    giver.level().getGameTime(),
                    true
            ));

            // Remove from needed items
            int needed = neededItems.getOrDefault(request.item, 0);
            if (needed <= request.quantity) {
                neededItems.remove(request.item);
            } else {
                neededItems.put(request.item, needed - request.quantity);
            }

            // Clean up
            pendingRequests.remove(requestId);

            return true;
        }

        return false;
    }

    /**
     * Decline a request
     */
    public void declineRequest(AgentEntity agent, UUID requestId) {
        PendingRequest request = pendingRequests.get(requestId);
        if (request != null) {
            // Track that this agent declined to help
            decreaseHelpfulness(agent.getUUID(), 1);

            pendingRequests.remove(requestId);
        }
    }

    /**
     * Find nearby agents who have the requested item
     */
    private List<AgentEntity> findNearbyAgentsWithItem(AgentEntity requester, Item item, int quantity) {
        List<AgentEntity> candidates = new ArrayList<>();

        List<LivingEntity> nearbyEntities = requester.level().getEntitiesOfClass(
                LivingEntity.class,
                requester.getBoundingBox().inflate(MAX_REQUEST_DISTANCE),
                entity -> entity instanceof AgentEntity && entity != requester
        );

        for (LivingEntity entity : nearbyEntities) {
            AgentEntity agent = (AgentEntity) entity;

            if (getAvailableQuantity(agent, item) >= quantity) {
                candidates.add(agent);
            }
        }

        return candidates;
    }

    /**
     * Get available quantity of an item in agent's inventory
     */
    private int getAvailableQuantity(AgentEntity agent, Item item) {
        int total = 0;

        for (int i = 0; i < agent.getInventory().getContainerSize(); i++) {
            ItemStack stack = agent.getInventory().getItem(i);
            if (stack.getItem() == item) {
                total += stack.getCount();
            }
        }

        return total;
    }

    /**
     * Transfer items from one agent to another
     */
    private boolean transferItems(AgentEntity giver, AgentEntity receiver, Item item, int quantity) {
        // Find item in giver's inventory
        int remaining = quantity;

        for (int i = 0; i < giver.getInventory().getContainerSize() && remaining > 0; i++) {
            ItemStack giverStack = giver.getInventory().getItem(i);

            if (giverStack.getItem() == item) {
                int toTake = Math.min(remaining, giverStack.getCount());

                // Try to add to receiver's inventory
                if (addItemToInventory(receiver, new ItemStack(item, toTake))) {
                    // Remove from giver
                    giverStack.shrink(toTake);
                    if (giverStack.isEmpty()) {
                        giver.getInventory().setItem(i, ItemStack.EMPTY);
                    }
                    remaining -= toTake;
                } else {
                    // Receiver's inventory full
                    break;
                }
            }
        }

        return remaining == 0; // Success if all items transferred
    }

    /**
     * Add item to agent's inventory
     */
    private boolean addItemToInventory(AgentEntity agent, ItemStack itemStack) {
        for (int i = 0; i < agent.getInventory().getContainerSize(); i++) {
            ItemStack slotStack = agent.getInventory().getItem(i);

            if (slotStack.isEmpty()) {
                agent.getInventory().setItem(i, itemStack);
                return true;
            } else if (ItemStack.isSameItem(slotStack, itemStack)) {
                int maxStackSize = slotStack.getMaxStackSize();
                int spaceAvailable = maxStackSize - slotStack.getCount();

                if (spaceAvailable >= itemStack.getCount()) {
                    slotStack.grow(itemStack.getCount());
                    return true;
                } else if (spaceAvailable > 0) {
                    slotStack.grow(spaceAvailable);
                    itemStack.shrink(spaceAvailable);
                    // Continue looking for more space
                }
            }
        }

        return false; // No space found
    }

    /**
     * Clean up expired requests
     */
    public void tick(AgentEntity agent) {
        long currentTime = agent.level().getGameTime();

        // Remove expired requests
        pendingRequests.entrySet().removeIf(entry -> {
            PendingRequest request = entry.getValue();
            if (request.isExpired(currentTime)) {
                // If this was our request, mark helper as unhelpful
                if (request.requesterId.equals(agent.getUUID()) && !request.completed) {
                    decreaseHelpfulness(request.targetAgentId, 2);
                }
                return true;
            }
            return false;
        });
    }

    /**
     * Get all pending requests where this agent is the target
     */
    public List<PendingRequest> getPendingRequestsForAgent(UUID agentId) {
        return pendingRequests.values().stream()
                .filter(request -> request.targetAgentId.equals(agentId) && !request.accepted && !request.completed)
                .toList();
    }

    /**
     * Get all accepted requests where this agent is the target
     */
    public List<PendingRequest> getAcceptedRequestsForAgent(UUID agentId) {
        return pendingRequests.values().stream()
                .filter(request -> request.targetAgentId.equals(agentId) && request.accepted && !request.completed)
                .toList();
    }

    /**
     * Check if agent needs any items
     */
    public boolean hasNeededItems() {
        return !neededItems.isEmpty();
    }

    /**
     * Get needed items
     */
    public Map<Item, Integer> getNeededItems() {
        return new HashMap<>(neededItems);
    }

    /**
     * Get helpfulness score for an agent
     */
    private int getHelpfulness(UUID agentId) {
        return helpfulness.getOrDefault(agentId, 50); // Start neutral
    }

    /**
     * Increase helpfulness score
     */
    private void increaseHelpfulness(UUID agentId, int amount) {
        int current = getHelpfulness(agentId);
        helpfulness.put(agentId, Math.min(100, current + amount));
    }

    /**
     * Decrease helpfulness score
     */
    private void decreaseHelpfulness(UUID agentId, int amount) {
        int current = getHelpfulness(agentId);
        helpfulness.put(agentId, Math.max(0, current - amount));
    }

    /**
     * Add completed request to history
     */
    private void addToHistory(CompletedRequest request) {
        requestHistory.add(request);

        // Keep history size manageable
        if (requestHistory.size() > MAX_HISTORY_SIZE) {
            requestHistory.remove(0);
        }
    }

    /**
     * Check if agent has recently helped with this type of item
     */
    public boolean hasRecentlyReceivedHelp(Item item) {
        long currentTime = System.currentTimeMillis();
        long recentThreshold = currentTime - (REQUEST_TIMEOUT_TICKS * 50); // Recent = last hour

        return requestHistory.stream()
                .anyMatch(request -> request.item == item &&
                        request.completionTime > recentThreshold &&
                        request.wasHelpful);
    }
}