package com.mas.masonry.agent.systems;

import com.mas.masonry.AgentEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import java.util.*;

/**
 * Manages trading relationships and transactions between agents
 */
public class TradingSystem {
    private final Map<UUID, TraderProfile> traderProfiles = new HashMap<>();
    private final List<TradeOffer> activeOffers = new ArrayList<>();
    private final List<CompletedTrade> tradeHistory = new ArrayList<>();

    // Constants
    private static final int MAX_ACTIVE_OFFERS = 5;
    private static final int OFFER_EXPIRY_TICKS = 6000; // 5 minutes
    private static final int MAX_TRADE_HISTORY = 20;
    private static final double BASE_TRADE_WILLINGNESS = 0.6;

    public static class TraderProfile {
        public final UUID agentId;
        public int tradingReputation; // 0-100
        public Map<Item, Integer> preferredItems; // Items they like to collect
        public Map<Item, Integer> surplusItems; // Items they have too much of
        public double generosity; // 0.0-1.0, how generous in trades
        public long lastTradeTime;
        public int totalTrades;

        public TraderProfile(UUID agentId) {
            this.agentId = agentId;
            this.tradingReputation = 50;
            this.preferredItems = new HashMap<>();
            this.surplusItems = new HashMap<>();
            this.generosity = 0.5 + new Random().nextDouble() * 0.3; // 0.5-0.8
            this.lastTradeTime = 0;
            this.totalTrades = 0;
        }
    }

    public static class TradeOffer {
        public final UUID offeredBy;
        public final Item offeredItem;
        public final int offeredQuantity;
        public final Item requestedItem;
        public final int requestedQuantity;
        public final long creationTime;
        public final String description;
        public boolean isActive;
        public UUID acceptedBy;

        public TradeOffer(UUID offeredBy, Item offeredItem, int offeredQty,
                          Item requestedItem, int requestedQty, long creationTime, String description) {
            this.offeredBy = offeredBy;
            this.offeredItem = offeredItem;
            this.offeredQuantity = offeredQty;
            this.requestedItem = requestedItem;
            this.requestedQuantity = requestedQty;
            this.creationTime = creationTime;
            this.description = description;
            this.isActive = true;
            this.acceptedBy = null;
        }

        public boolean isExpired(long currentTime) {
            return (currentTime - creationTime) > OFFER_EXPIRY_TICKS;
        }
    }

    public static class CompletedTrade {
        public final UUID trader1;
        public final UUID trader2;
        public final Item item1;
        public final int quantity1;
        public final Item item2;
        public final int quantity2;
        public final long completionTime;
        public final boolean wasSuccessful;

        public CompletedTrade(UUID trader1, UUID trader2, Item item1, int qty1,
                              Item item2, int qty2, long time, boolean successful) {
            this.trader1 = trader1;
            this.trader2 = trader2;
            this.item1 = item1;
            this.quantity1 = qty1;
            this.item2 = item2;
            this.quantity2 = qty2;
            this.completionTime = time;
            this.wasSuccessful = successful;
        }
    }

    /**
     * Create a trade offer
     */
    public boolean createTradeOffer(AgentEntity trader, Item offered, int offeredQty,
                                    Item requested, int requestedQty) {

        if (activeOffers.size() >= MAX_ACTIVE_OFFERS) {
            return false; // Too many active offers
        }

        // Check if trader has the offered items
        if (!hasEnoughItems(trader, offered, offeredQty)) {
            return false;
        }

        TradeOffer offer = new TradeOffer(
                trader.getUUID(),
                offered,
                offeredQty,
                requested,
                requestedQty,
                trader.level().getGameTime(),
                "Tukar " + offeredQty + " " + offered.getDescription().getString() +
                        " dengan " + requestedQty + " " + requested.getDescription().getString()
        );

        activeOffers.add(offer);

        // Create gossip about this trade
        trader.getGossipSystem().createTradingGossip(trader, offered, true, offeredQty,
                requestedQty + " " + requested.getDescription().getString());

        return true;
    }

    /**
     * Find suitable trade offers for an agent
     */
    public List<TradeOffer> findSuitableOffers(AgentEntity agent, Item neededItem) {
        UUID agentId = agent.getUUID();

        return activeOffers.stream()
                .filter(offer -> offer.isActive)
                .filter(offer -> !offer.offeredBy.equals(agentId)) // Can't trade with yourself
                .filter(offer -> offer.requestedItem.equals(neededItem))
                .filter(offer -> hasEnoughItems(agent, offer.offeredItem, offer.offeredQuantity))
                .filter(offer -> !offer.isExpired(agent.level().getGameTime()))
                .sorted(Comparator.comparingDouble(offer -> calculateTradeValue(agent, offer)))
                .toList();
    }

    /**
     * Accept a trade offer
     */
    public boolean acceptTradeOffer(AgentEntity accepter, TradeOffer offer) {
        if (!offer.isActive || offer.acceptedBy != null) {
            return false;
        }

        // Find the offering agent
        AgentEntity offerer = findAgentById(accepter, offer.offeredBy);
        if (offerer == null) {
            return false;
        }

        // Verify both agents have required items
        if (!hasEnoughItems(offerer, offer.offeredItem, offer.offeredQuantity) ||
                !hasEnoughItems(accepter, offer.requestedItem, offer.requestedQuantity)) {
            return false;
        }

        // Execute the trade
        boolean success = executeTrade(offerer, accepter, offer);

        if (success) {
            offer.acceptedBy = accepter.getUUID();
            offer.isActive = false;

            // Record the trade
            CompletedTrade trade = new CompletedTrade(
                    offer.offeredBy,
                    accepter.getUUID(),
                    offer.offeredItem,
                    offer.offeredQuantity,
                    offer.requestedItem,
                    offer.requestedQuantity,
                    accepter.level().getGameTime(),
                    true
            );

            addTradeToHistory(trade);

            // Update trading reputations
            updateTradingReputation(offer.offeredBy, 5);
            updateTradingReputation(accepter.getUUID(), 5);

            // Create positive reputation gossip
            accepter.getGossipSystem().createReputationGossip(
                    accepter, offerer, true, "Fair trader, good deals!");
            offerer.getGossipSystem().createReputationGossip(
                    offerer, accepter, true, "Smooth transaction!");
        }

        return success;
    }

    /**
     * Execute the actual item transfer
     */
    private boolean executeTrade(AgentEntity trader1, AgentEntity trader2, TradeOffer offer) {
        // Remove items from trader1 (offerer)
        if (!removeItems(trader1, offer.offeredItem, offer.offeredQuantity)) {
            return false;
        }

        // Remove items from trader2 (accepter)  
        if (!removeItems(trader2, offer.requestedItem, offer.requestedQuantity)) {
            // Restore trader1's items if trader2 doesn't have theirs
            addItems(trader1, offer.offeredItem, offer.offeredQuantity);
            return false;
        }

        // Give items to each trader
        boolean give1Success = addItems(trader2, offer.offeredItem, offer.offeredQuantity);
        boolean give2Success = addItems(trader1, offer.requestedItem, offer.requestedQuantity);

        if (!give1Success || !give2Success) {
            // Rollback if either addition failed
            if (!give1Success) {
                addItems(trader1, offer.offeredItem, offer.offeredQuantity);
            }
            if (!give2Success) {
                addItems(trader2, offer.requestedItem, offer.requestedQuantity);
            }
            return false;
        }

        return true;
    }

    /**
     * Calculate the value/attractiveness of a trade offer
     */
    private double calculateTradeValue(AgentEntity evaluator, TradeOffer offer) {
        TraderProfile profile = getOrCreateProfile(evaluator.getUUID());

        double value = 1.0;

        // Check if offered item is preferred
        if (profile.preferredItems.containsKey(offer.offeredItem)) {
            value += 0.5;
        }

        // Check if requested item is surplus
        if (profile.surplusItems.containsKey(offer.requestedItem)) {
            value += 0.3;
        }

        // Consider quantity ratios
        double ratio = (double) offer.offeredQuantity / offer.requestedQuantity;
        if (ratio > 1.0) {
            value += (ratio - 1.0) * 0.2; // Better deals are more attractive
        }

        // Consider trading reputation of offerer
        int offererReputation = getTradingReputation(offer.offeredBy);
        if (offererReputation > 70) {
            value += 0.3; // Trustworthy traders
        } else if (offererReputation < 30) {
            value -= 0.4; // Avoid risky traders
        }

        return value;
    }

    /**
     * Suggest automatic trades based on inventory and needs
     */
    public TradeOffer suggestTrade(AgentEntity agent) {
        TraderProfile profile = getOrCreateProfile(agent.getUUID());

        // Analyze current inventory
        analyzeInventory(agent, profile);

        // Find items to offer (surplus items)
        Item surplusItem = findBestSurplusItem(profile);
        if (surplusItem == null) {
            return null;
        }

        // Find items to request (profession needs or construction needs)
        Item neededItem = findNeededItem(agent);
        if (neededItem == null) {
            return null;
        }

        int offerQuantity = Math.min(profile.surplusItems.get(surplusItem), 8);
        int requestQuantity = calculateFairQuantity(surplusItem, neededItem, offerQuantity);

        return new TradeOffer(
                agent.getUUID(),
                surplusItem,
                offerQuantity,
                neededItem,
                requestQuantity,
                agent.level().getGameTime(),
                "Auto-generated trade offer"
        );
    }

    /**
     * Analyze agent's inventory to identify surplus and needs
     */
    private void analyzeInventory(AgentEntity agent, TraderProfile profile) {
        profile.surplusItems.clear();

        Map<Item, Integer> itemCounts = new HashMap<>();

        // Count all items in inventory
        for (int i = 0; i < agent.getInventory().getContainerSize(); i++) {
            ItemStack stack = agent.getInventory().getItem(i);
            if (!stack.isEmpty()) {
                Item item = stack.getItem();
                itemCounts.put(item, itemCounts.getOrDefault(item, 0) + stack.getCount());
            }
        }

        // Identify surplus (more than 16 of any item)
        itemCounts.forEach((item, count) -> {
            if (count > 16) {
                profile.surplusItems.put(item, count - 8); // Keep 8, offer the rest
            }
        });
    }

    /**
     * Find the best surplus item to offer
     */
    private Item findBestSurplusItem(TraderProfile profile) {
        return profile.surplusItems.entrySet().stream()
                .filter(entry -> entry.getValue() > 0)
                .max(Comparator.comparingInt(Map.Entry::getValue))
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    /**
     * Find what item the agent needs most
     */
    private Item findNeededItem(AgentEntity agent) {
        // Check construction needs first
        if (agent.getCurrentBlueprintIndex() < agent.getCurrentBlueprint().size()) {
            return agent.getCurrentBlueprint().get(agent.getCurrentBlueprintIndex()).blockType.asItem();
        }

        // Check profession needs
        if (agent.getProfessionSystem().hasProfession()) {
            Item[] professionItems = agent.getProfessionSystem().getCurrentProfession().getProfessionItems();
            for (Item item : professionItems) {
                if (getItemCount(agent, item) < 4) {
                    return item;
                }
            }
        }

        // Default needs (basic materials)
        List<Item> basicNeeds = Arrays.asList(
                net.minecraft.world.item.Items.COBBLESTONE,
                net.minecraft.world.item.Items.OAK_LOG,
                net.minecraft.world.item.Items.IRON_INGOT,
                net.minecraft.world.item.Items.WHEAT
        );

        for (Item item : basicNeeds) {
            if (getItemCount(agent, item) < 8) {
                return item;
            }
        }

        return null;
    }

    /**
     * Calculate fair quantity for trade
     */
    private int calculateFairQuantity(Item offered, Item requested, int offeredQuantity) {
        // Simple value-based calculation (can be expanded)
        Map<Item, Integer> itemValues = createItemValueMap();

        int offeredValue = itemValues.getOrDefault(offered, 1);
        int requestedValue = itemValues.getOrDefault(requested, 1);

        int fairQuantity = Math.max(1, (offeredQuantity * offeredValue) / requestedValue);
        return Math.min(fairQuantity, 16); // Cap at 16
    }

    /**
     * Create a simple item value mapping
     */
    private Map<Item, Integer> createItemValueMap() {
        Map<Item, Integer> values = new HashMap<>();

        // Basic materials
        values.put(net.minecraft.world.item.Items.COBBLESTONE, 1);
        values.put(net.minecraft.world.item.Items.OAK_LOG, 2);
        values.put(net.minecraft.world.item.Items.OAK_PLANKS, 1);
        values.put(net.minecraft.world.item.Items.STICK, 1);

        // Valuable materials
        values.put(net.minecraft.world.item.Items.IRON_INGOT, 8);
        values.put(net.minecraft.world.item.Items.GOLD_INGOT, 12);
        values.put(net.minecraft.world.item.Items.DIAMOND, 20);

        // Food
        values.put(net.minecraft.world.item.Items.WHEAT, 2);
        values.put(net.minecraft.world.item.Items.BREAD, 4);
        values.put(net.minecraft.world.item.Items.APPLE, 3);

        return values;
    }

    /**
     * Helper methods
     */
    private boolean hasEnoughItems(AgentEntity agent, Item item, int quantity) {
        return getItemCount(agent, item) >= quantity;
    }

    private int getItemCount(AgentEntity agent, Item item) {
        int count = 0;
        for (int i = 0; i < agent.getInventory().getContainerSize(); i++) {
            ItemStack stack = agent.getInventory().getItem(i);
            if (stack.getItem() == item) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private boolean removeItems(AgentEntity agent, Item item, int quantity) {
        int remaining = quantity;

        for (int i = 0; i < agent.getInventory().getContainerSize() && remaining > 0; i++) {
            ItemStack stack = agent.getInventory().getItem(i);
            if (stack.getItem() == item) {
                int toRemove = Math.min(remaining, stack.getCount());
                stack.shrink(toRemove);
                if (stack.isEmpty()) {
                    agent.getInventory().setItem(i, ItemStack.EMPTY);
                }
                remaining -= toRemove;
            }
        }

        return remaining == 0;
    }

    private boolean addItems(AgentEntity agent, Item item, int quantity) {
        ItemStack itemsToAdd = new ItemStack(item, quantity);

        for (int i = 0; i < agent.getInventory().getContainerSize(); i++) {
            ItemStack slotStack = agent.getInventory().getItem(i);

            if (slotStack.isEmpty()) {
                agent.getInventory().setItem(i, itemsToAdd);
                return true;
            } else if (ItemStack.isSameItem(slotStack, itemsToAdd)) {
                int maxStackSize = slotStack.getMaxStackSize();
                int spaceAvailable = maxStackSize - slotStack.getCount();

                if (spaceAvailable >= itemsToAdd.getCount()) {
                    slotStack.grow(itemsToAdd.getCount());
                    return true;
                } else if (spaceAvailable > 0) {
                    slotStack.grow(spaceAvailable);
                    itemsToAdd.shrink(spaceAvailable);
                }
            }
        }

        return itemsToAdd.isEmpty();
    }

    private AgentEntity findAgentById(AgentEntity searcher, UUID targetId) {
        return searcher.level().getEntitiesOfClass(AgentEntity.class,
                        searcher.getBoundingBox().inflate(64.0))
                .stream()
                .filter(agent -> agent.getUUID().equals(targetId))
                .findFirst()
                .orElse(null);
    }

    private TraderProfile getOrCreateProfile(UUID agentId) {
        return traderProfiles.computeIfAbsent(agentId, TraderProfile::new);
    }

    private void updateTradingReputation(UUID agentId, int change) {
        TraderProfile profile = getOrCreateProfile(agentId);
        profile.tradingReputation = Math.max(0, Math.min(100, profile.tradingReputation + change));
    }

    private int getTradingReputation(UUID agentId) {
        return getOrCreateProfile(agentId).tradingReputation;
    }

    private void addTradeToHistory(CompletedTrade trade) {
        tradeHistory.add(trade);
        if (tradeHistory.size() > MAX_TRADE_HISTORY) {
            tradeHistory.remove(0);
        }
    }

    /**
     * Clean up expired offers
     */
    public void tick(AgentEntity agent) {
        long currentTime = agent.level().getGameTime();
        activeOffers.removeIf(offer -> offer.isExpired(currentTime));
    }

    /**
     * Get all active offers
     */
    public List<TradeOffer> getActiveOffers() {
        return new ArrayList<>(activeOffers);
    }

    /**
     * Check if agent is actively trading
     */
    public boolean hasActiveOffers(UUID agentId) {
        return activeOffers.stream().anyMatch(offer -> offer.offeredBy.equals(agentId) && offer.isActive);
    }
}