package com.mas.masonry.agent.systems;

import com.mas.masonry.AgentEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Item;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages gossip propagation between agents
 * Gossip includes: resource locations, danger warnings, reputation, trading info
 */
public class GossipSystem {
    private final Map<String, GossipEntry> knownGossip = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> agentReputations = new HashMap<>();
    private final List<GossipEntry> recentGossip = new ArrayList<>();
    
    // Constants
    private static final int MAX_GOSSIP_AGE_TICKS = 24000; // 20 minutes
    private static final int MAX_RECENT_GOSSIP = 10;
    private static final int GOSSIP_SHARE_COOLDOWN = 1200; // 1 minute between sharing same gossip
    private static final double GOSSIP_DECAY_RATE = 0.02; // 2% confidence loss per minute
    
    public enum GossipType {
        RESOURCE_LOCATION,    // "Ada diamond di koordinat X!"
        DANGER_WARNING,       // "Hati-hati, ada creeper spawn di area Y!"
        AGENT_REPUTATION,     // "Si Budi orangnya helpful banget!"
        TRADING_INFO,         // "Sari lagi jual iron murah!"
        CONSTRUCTION_TIP,     // "Ada spot bagus buat bikin base!"
        WEATHER_PREDICTION,   // "Kayaknya mau hujan nih..."
        GENERAL_NEWS         // "Tadi gue lihat village baru!"
    }
    
    public static class GossipEntry {
        public final String id;
        public final GossipType type;
        public final String content;
        public final Map<String, Object> data;
        public final UUID originalSpeaker;
        public final long creationTime;
        public double confidence; // 0.0 to 1.0
        public int timesShared;
        public long lastSharedTime;
        
        public GossipEntry(String id, GossipType type, String content, UUID speaker, long creationTime) {
            this.id = id;
            this.type = type;
            this.content = content;
            this.data = new HashMap<>();
            this.originalSpeaker = speaker;
            this.creationTime = creationTime;
            this.confidence = 1.0;
            this.timesShared = 0;
            this.lastSharedTime = 0;
        }
        
        public boolean isStale(long currentTime) {
            return (currentTime - creationTime) > MAX_GOSSIP_AGE_TICKS;
        }
        
        public void addData(String key, Object value) {
            data.put(key, value);
        }
        
        public Object getData(String key) {
            return data.get(key);
        }
        
        public void decreaseConfidence() {
            confidence = Math.max(0.0, confidence - GOSSIP_DECAY_RATE);
        }
        
        public void increaseShared(long currentTime) {
            timesShared++;
            lastSharedTime = currentTime;
        }
        
        public boolean canShareAgain(long currentTime) {
            return (currentTime - lastSharedTime) > GOSSIP_SHARE_COOLDOWN;
        }
    }
    
    /**
     * Create gossip about a resource location
     */
    public void createResourceGossip(AgentEntity speaker, Item item, BlockPos location) {
        String id = "resource_" + item.toString() + "_" + location.toString();
        GossipEntry gossip = new GossipEntry(
            id, 
            GossipType.RESOURCE_LOCATION, 
            "Ada " + item.getDescription().getString() + " di sekitar sini!",
            speaker.getUUID(),
            speaker.level().getGameTime()
        );
        gossip.addData("item", item);
        gossip.addData("location", location);
        gossip.addData("speaker_name", speaker.getBaseName());
        
        addGossip(gossip);
    }
    
    /**
     * Create gossip about danger
     */
    public void createDangerGossip(AgentEntity speaker, String dangerType, BlockPos location) {
        String id = "danger_" + dangerType + "_" + location.toString() + "_" + speaker.level().getGameTime();
        GossipEntry gossip = new GossipEntry(
            id,
            GossipType.DANGER_WARNING,
            "Hati-hati! Ada " + dangerType + " berbahaya di area itu!",
            speaker.getUUID(),
            speaker.level().getGameTime()
        );
        gossip.addData("danger_type", dangerType);
        gossip.addData("location", location);
        gossip.addData("speaker_name", speaker.getBaseName());
        
        addGossip(gossip);
    }
    
    /**
     * Create gossip about agent reputation
     */
    public void createReputationGossip(AgentEntity speaker, AgentEntity subject, boolean isPositive, String reason) {
        String sentiment = isPositive ? "positive" : "negative";
        String id = "reputation_" + subject.getUUID() + "_" + sentiment + "_" + speaker.level().getGameTime();
        
        String content = isPositive 
            ? subject.getBaseName() + " orangnya helpful banget! " + reason
            : subject.getBaseName() + " agak susah diajak kerjasama. " + reason;
            
        GossipEntry gossip = new GossipEntry(
            id,
            GossipType.AGENT_REPUTATION,
            content,
            speaker.getUUID(),
            speaker.level().getGameTime()
        );
        gossip.addData("subject_id", subject.getUUID());
        gossip.addData("subject_name", subject.getBaseName());
        gossip.addData("is_positive", isPositive);
        gossip.addData("reason", reason);
        gossip.addData("speaker_name", speaker.getBaseName());
        
        addGossip(gossip);
        
        // Update local reputation tracking
        updateReputation(subject.getUUID(), isPositive ? 5 : -3);
    }
    
    /**
     * Create gossip about trading
     */
    public void createTradingGossip(AgentEntity speaker, Item item, boolean isSelling, int quantity, String price) {
        String action = isSelling ? "jual" : "beli";
        String id = "trade_" + speaker.getUUID() + "_" + item.toString() + "_" + action;
        
        GossipEntry gossip = new GossipEntry(
            id,
            GossipType.TRADING_INFO,
            speaker.getBaseName() + " lagi " + action + " " + item.getDescription().getString() + "!",
            speaker.getUUID(),
            speaker.level().getGameTime()
        );
        gossip.addData("trader_id", speaker.getUUID());
        gossip.addData("trader_name", speaker.getBaseName());
        gossip.addData("item", item);
        gossip.addData("is_selling", isSelling);
        gossip.addData("quantity", quantity);
        gossip.addData("price", price);
        
        addGossip(gossip);
    }
    
    /**
     * Create general news gossip
     */
    public void createGeneralGossip(AgentEntity speaker, String newsContent) {
        String id = "news_" + speaker.getUUID() + "_" + speaker.level().getGameTime();
        GossipEntry gossip = new GossipEntry(
            id,
            GossipType.GENERAL_NEWS,
            newsContent,
            speaker.getUUID(),
            speaker.level().getGameTime()
        );
        gossip.addData("speaker_name", speaker.getBaseName());
        
        addGossip(gossip);
    }
    
    /**
     * Add gossip to the system
     */
    private void addGossip(GossipEntry gossip) {
        knownGossip.put(gossip.id, gossip);
        recentGossip.add(0, gossip); // Add to front
        
        // Keep recent gossip list manageable
        if (recentGossip.size() > MAX_RECENT_GOSSIP) {
            recentGossip.remove(recentGossip.size() - 1);
        }
    }
    
    /**
     * Share gossip with another agent
     */
    public boolean shareGossipWith(AgentEntity speaker, AgentEntity listener) {
        // Find interesting gossip to share
        GossipEntry gossipToShare = selectGossipToShare(speaker, listener);
        
        if (gossipToShare == null) {
            return false;
        }
        
        // Transfer the gossip
        listener.getGossipSystem().receiveGossip(gossipToShare, speaker);
        
        // Mark as shared
        gossipToShare.increaseShared(speaker.level().getGameTime());
        
        return true;
    }
    
    /**
     * Receive gossip from another agent
     */
    public void receiveGossip(GossipEntry originalGossip, AgentEntity source) {
        // Create a copy with reduced confidence (telephone game effect)
        GossipEntry receivedGossip = new GossipEntry(
            originalGossip.id,
            originalGossip.type,
            originalGossip.content,
            originalGossip.originalSpeaker,
            originalGossip.creationTime
        );
        
        // Copy data
        receivedGossip.data.putAll(originalGossip.data);
        
        // Reduce confidence based on how many times it's been shared
        receivedGossip.confidence = Math.max(0.3, originalGossip.confidence * 0.85);
        receivedGossip.timesShared = originalGossip.timesShared;
        
        // Only add if we don't already know this gossip or if this version is more confident
        GossipEntry existing = knownGossip.get(receivedGossip.id);
        if (existing == null || existing.confidence < receivedGossip.confidence) {
            addGossip(receivedGossip);
        }
    }
    
    /**
     * Select the most interesting gossip to share
     */
    private GossipEntry selectGossipToShare(AgentEntity speaker, AgentEntity listener) {
        long currentTime = speaker.level().getGameTime();
        
        // Filter gossip that can be shared
        List<GossipEntry> shareableGossip = recentGossip.stream()
            .filter(gossip -> !gossip.isStale(currentTime))
            .filter(gossip -> gossip.confidence > 0.3)
            .filter(gossip -> gossip.canShareAgain(currentTime))
            .filter(gossip -> !gossip.originalSpeaker.equals(listener.getUUID())) // Don't tell people their own gossip
            .toList();
        
        if (shareableGossip.isEmpty()) {
            return null;
        }
        
        // Prioritize by type and relevance
        return shareableGossip.stream()
            .max(Comparator
                .comparingDouble(this::calculateGossipRelevance)
                .thenComparing(gossip -> gossip.confidence)
                .thenComparing(gossip -> -gossip.timesShared)) // Less shared = more interesting
            .orElse(null);
    }
    
    /**
     * Calculate how relevant/interesting gossip is
     */
    private double calculateGossipRelevance(GossipEntry gossip) {
        double relevance = 1.0;
        
        switch (gossip.type) {
            case DANGER_WARNING:
                relevance = 2.0; // Safety is priority
                break;
            case RESOURCE_LOCATION:
                relevance = 1.8; // Very useful
                break;
            case TRADING_INFO:
                relevance = 1.5; // Economic opportunity
                break;
            case AGENT_REPUTATION:
                relevance = 1.3; // Social info
                break;
            case CONSTRUCTION_TIP:
                relevance = 1.2; // Helpful tips
                break;
            case GENERAL_NEWS:
                relevance = 1.0; // General interest
                break;
            case WEATHER_PREDICTION:
                relevance = 0.8; // Less urgent
                break;
        }
        
        // Recent gossip is more relevant
        long age = System.currentTimeMillis() - gossip.creationTime;
        double ageFactor = Math.max(0.3, 1.0 - (age / (double)MAX_GOSSIP_AGE_TICKS));
        
        return relevance * ageFactor * gossip.confidence;
    }
    
    /**
     * Get gossip of a specific type
     */
    public List<GossipEntry> getGossipByType(GossipType type) {
        return knownGossip.values().stream()
            .filter(gossip -> gossip.type == type)
            .filter(gossip -> !gossip.isStale(System.currentTimeMillis()))
            .filter(gossip -> gossip.confidence > 0.2)
            .sorted(Comparator.comparingDouble(gossip -> -gossip.confidence))
            .toList();
    }
    
    /**
     * Get resource locations from gossip
     */
    public List<BlockPos> getKnownResourceLocations(Item item) {
        return getGossipByType(GossipType.RESOURCE_LOCATION).stream()
            .filter(gossip -> gossip.getData("item").equals(item))
            .map(gossip -> (BlockPos) gossip.getData("location"))
            .toList();
    }
    
    /**
     * Get known danger locations
     */
    public List<BlockPos> getKnownDangerLocations() {
        return getGossipByType(GossipType.DANGER_WARNING).stream()
            .map(gossip -> (BlockPos) gossip.getData("location"))
            .toList();
    }
    
    /**
     * Get trading opportunities
     */
    public List<GossipEntry> getTradingOpportunities(Item item, boolean lookingToBuy) {
        return getGossipByType(GossipType.TRADING_INFO).stream()
            .filter(gossip -> gossip.getData("item").equals(item))
            .filter(gossip -> {
                Boolean isSelling = (Boolean) gossip.getData("is_selling");
                return lookingToBuy ? isSelling : !isSelling;
            })
            .toList();
    }
    
    /**
     * Update reputation for an agent
     */
    public void updateReputation(UUID agentId, int change) {
        int current = agentReputations.getOrDefault(agentId, 50); // Start neutral at 50
        int newReputation = Math.max(0, Math.min(100, current + change));
        agentReputations.put(agentId, newReputation);
    }
    
    /**
     * Get reputation for an agent
     */
    public int getReputation(UUID agentId) {
        return agentReputations.getOrDefault(agentId, 50);
    }
    
    /**
     * Check if an agent is well-regarded
     */
    public boolean isWellRegarded(UUID agentId) {
        return getReputation(agentId) > 70;
    }
    
    /**
     * Check if an agent has bad reputation
     */
    public boolean hasBadReputation(UUID agentId) {
        return getReputation(agentId) < 30;
    }
    
    /**
     * Clean up old gossip
     */
    public void tick(AgentEntity agent) {
        long currentTime = agent.level().getGameTime();
        
        // Remove stale gossip
        knownGossip.entrySet().removeIf(entry -> entry.getValue().isStale(currentTime));
        recentGossip.removeIf(gossip -> gossip.isStale(currentTime));
        
        // Decay confidence of old gossip
        knownGossip.values().forEach(GossipEntry::decreaseConfidence);
        
        // Remove gossip with very low confidence
        knownGossip.entrySet().removeIf(entry -> entry.getValue().confidence < 0.1);
    }
    
    /**
     * Get interesting gossip for conversation
     */
    public GossipEntry getRandomInterestingGossip() {
        if (recentGossip.isEmpty()) {
            return null;
        }
        
        List<GossipEntry> interesting = recentGossip.stream()
            .filter(gossip -> gossip.confidence > 0.5)
            .filter(gossip -> !gossip.isStale(System.currentTimeMillis()))
            .toList();
        
        if (interesting.isEmpty()) {
            return null;
        }
        
        return interesting.get(new Random().nextInt(interesting.size()));
    }
    
    /**
     * Check if agent has any interesting gossip to share
     */
    public boolean hasInterestingGossip() {
        return !recentGossip.isEmpty() && 
               recentGossip.stream().anyMatch(gossip -> 
                   gossip.confidence > 0.5 && !gossip.isStale(System.currentTimeMillis()));
    }
}