package com.mas.masonry.agent.systems;

import com.mas.masonry.AgentEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Item;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages emergent behaviors that arise from agent interactions
 */
public class EmergentBehaviorSystem {
    private final Map<String, CommunityPattern> detectedPatterns = new ConcurrentHashMap<>();
    private final List<EmergentEvent> emergentEvents = new ArrayList<>();
    private final Map<String, CommunityTradition> traditions = new HashMap<>();
    private final CommunityMetrics metrics = new CommunityMetrics();
    
    // Constants
    private static final int PATTERN_DETECTION_THRESHOLD = 3;
    private static final int TRADITION_FORMATION_EVENTS = 5;
    private static final int MAX_EMERGENT_EVENTS = 50;
    private static final long PATTERN_TIMEOUT = 24000; // 20 minutes
    
    public static class CommunityPattern {
        public final String id;
        public final String type;
        public final String description;
        public final Set<UUID> involvedAgents;
        public final BlockPos epicenter;
        public final long firstOccurrence;
        public int occurrenceCount;
        public long lastOccurrence;
        public double strength; // 0.0 to 1.0
        public Map<String, Object> patternData;
        
        public CommunityPattern(String id, String type, String description, 
                              Set<UUID> agents, BlockPos location, long timestamp) {
            this.id = id;
            this.type = type;
            this.description = description;
            this.involvedAgents = new HashSet<>(agents);
            this.epicenter = location;
            this.firstOccurrence = timestamp;
            this.occurrenceCount = 1;
            this.lastOccurrence = timestamp;
            this.strength = 0.1;
            this.patternData = new HashMap<>();
        }
        
        public void reinforce(long timestamp) {
            occurrenceCount++;
            lastOccurrence = timestamp;
            strength = Math.min(1.0, strength + 0.1);
        }
        
        public void decay() {
            strength = Math.max(0.0, strength - 0.02);
        }
        
        public boolean isActive() {
            return strength > 0.3;
        }
    }
    
    public static class EmergentEvent {
        public final String type;
        public final String description;
        public final Set<UUID> participants;
        public final BlockPos location;
        public final long timestamp;
        public final Map<String, Object> eventData;
        
        public EmergentEvent(String type, String description, Set<UUID> participants,
                           BlockPos location, long timestamp) {
            this.type = type;
            this.description = description;
            this.participants = new HashSet<>(participants);
            this.location = location;
            this.timestamp = timestamp;
            this.eventData = new HashMap<>();
        }
    }
    
    public static class CommunityTradition {
        public final String name;
        public final String description;
        public final String trigger; // What causes this tradition
        public final List<String> activities;
        public final Set<UUID> practitioners;
        public int performanceCount;
        public long lastPerformed;
        public double significance; // How important this tradition is
        
        public CommunityTradition(String name, String description, String trigger) {
            this.name = name;
            this.description = description;
            this.trigger = trigger;
            this.activities = new ArrayList<>();
            this.practitioners = new HashSet<>();
            this.performanceCount = 0;
            this.lastPerformed = 0;
            this.significance = 0.1;
        }
        
        public void perform(Set<UUID> participants, long timestamp) {
            performanceCount++;
            lastPerformed = timestamp;
            practitioners.addAll(participants);
            significance = Math.min(1.0, significance + 0.05);
        }
    }
    
    public static class CommunityMetrics {
        public double cohesion = 0.5;           // How united the community is
        public double productivity = 0.5;       // How efficiently they work together  
        public double innovation = 0.5;         // How creative/adaptive they are
        public double stability = 0.5;          // How consistent their patterns are
        public double diversity = 0.5;          // How varied their behaviors are
        public double leadership = 0.5;         // How well leadership emerges
        public double cooperation = 0.5;        // How well they cooperate
        public double resilience = 0.5;         // How they handle challenges
        
        public void updateMetric(String metric, double change) {
            switch (metric.toLowerCase()) {
                case "cohesion": cohesion = clamp(cohesion + change); break;
                case "productivity": productivity = clamp(productivity + change); break;
                case "innovation": innovation = clamp(innovation + change); break;
                case "stability": stability = clamp(stability + change); break;
                case "diversity": diversity = clamp(diversity + change); break;
                case "leadership": leadership = clamp(leadership + change); break;
                case "cooperation": cooperation = clamp(cooperation + change); break;
                case "resilience": resilience = clamp(resilience + change); break;
            }
        }
        
        private double clamp(double value) {
            return Math.max(0.0, Math.min(1.0, value));
        }
        
        public double getOverallHealthScore() {
            return (cohesion + productivity + innovation + stability + 
                   diversity + leadership + cooperation + resilience) / 8.0;
        }
    }
    
    /**
     * Detect emergent patterns from agent behaviors
     */
    public void analyzeEmergentBehaviors(List<AgentEntity> agents, long currentTime) {
        // Analyze spatial clustering
        analyzeSpacialClustering(agents, currentTime);
        
        // Analyze collaborative patterns
        analyzeCollaborationPatterns(agents, currentTime);
        
        // Analyze economic patterns
        analyzeEconomicPatterns(agents, currentTime);
        
        // Analyze social hierarchies
        analyzeSocialHierarchies(agents, currentTime);
        
        // Analyze innovation patterns
        analyzeInnovationPatterns(agents, currentTime);
        
        // Update community metrics
        updateCommunityMetrics(agents);
        
        // Check for tradition formation
        checkForTraditionFormation(currentTime);
        
        // Decay old patterns
        decayPatterns(currentTime);
    }
    
    private void analyzeSpacialClustering(List<AgentEntity> agents, long currentTime) {
        Map<BlockPos, List<AgentEntity>> clusters = new HashMap<>();
        
        // Group agents by proximity (16-block radius)
        for (AgentEntity agent : agents) {
            BlockPos center = agent.blockPosition();
            BlockPos clusterKey = new BlockPos(
                (center.getX() / 16) * 16,
                center.getY(),
                (center.getZ() / 16) * 16
            );
            
            clusters.computeIfAbsent(clusterKey, k -> new ArrayList<>()).add(agent);
        }
        
        // Identify significant clusters
        for (Map.Entry<BlockPos, List<AgentEntity>> entry : clusters.entrySet()) {
            if (entry.getValue().size() >= 3) { // 3+ agents = cluster
                String patternId = "spatial_cluster_" + entry.getKey().toString();
                
                CommunityPattern pattern = detectedPatterns.get(patternId);
                if (pattern == null) {
                    Set<UUID> agentIds = new HashSet<>();
                    entry.getValue().forEach(agent -> agentIds.add(agent.getUUID()));
                    
                    pattern = new CommunityPattern(
                        patternId,
                        "SPATIAL_CLUSTERING",
                        "Agents consistently gathering at " + entry.getKey(),
                        agentIds,
                        entry.getKey(),
                        currentTime
                    );
                    
                    detectedPatterns.put(patternId, pattern);
                    recordEmergentEvent("CLUSTER_FORMED", 
                        "Spatial cluster formed at " + entry.getKey(), 
                        pattern.involvedAgents, entry.getKey(), currentTime);
                    
                } else {
                    pattern.reinforce(currentTime);
                }
                
                // Check if this becomes a community hub
                if (pattern.occurrenceCount >= 5 && pattern.strength > 0.7) {
                    establishCommunityHub(entry.getKey(), entry.getValue(), currentTime);
                }
            }
        }
    }
    
    private void analyzeCollaborationPatterns(List<AgentEntity> agents, long currentTime) {
        // Look for recurring collaborative groups
        Map<Set<UUID>, Integer> collaborationFrequency = new HashMap<>();
        
        for (AgentEntity agent : agents) {
            List<CoordinationSystem.GroupProject> projects = 
                agent.getCoordinationSystem().getActiveProjects();
            
            for (CoordinationSystem.GroupProject project : projects) {
                if (project.participants.size() >= 2) {
                    collaborationFrequency.merge(project.participants, 1, Integer::sum);
                }
            }
        }
        
        // Identify strong collaboration patterns
        for (Map.Entry<Set<UUID>, Integer> entry : collaborationFrequency.entrySet()) {
            if (entry.getValue() >= PATTERN_DETECTION_THRESHOLD) {
                String patternId = "collaboration_" + entry.getKey().hashCode();
                
                CommunityPattern pattern = detectedPatterns.get(patternId);
                if (pattern == null) {
                    pattern = new CommunityPattern(
                        patternId,
                        "COLLABORATION_TEAM",
                        "Recurring collaboration team of " + entry.getKey().size() + " agents",
                        entry.getKey(),
                        findCenterPoint(agents, entry.getKey()),
                        currentTime
                    );
                    
                    detectedPatterns.put(patternId, pattern);
                    recordEmergentEvent("TEAM_FORMED",
                        "Consistent collaboration team established",
                        pattern.involvedAgents, pattern.epicenter, currentTime);
                }
                
                // Check if this becomes a permanent work group
                if (pattern.occurrenceCount >= 8) {
                    establishWorkGroup(entry.getKey(), currentTime);
                }
            }
        }
        
        // Update cooperation metric
        double cooperationLevel = (double) collaborationFrequency.size() / Math.max(1, agents.size() / 2);
        metrics.updateMetric("cooperation", (cooperationLevel - 0.5) * 0.1);
    }
    
    private void analyzeEconomicPatterns(List<AgentEntity> agents, long currentTime) {
        Map<String, Integer> tradeRoutes = new HashMap<>();
        Map<Item, List<AgentEntity>> resourceSpecialists = new HashMap<>();
        
        for (AgentEntity agent : agents) {
            // Analyze trading patterns
            List<TradingSystem.TradeOffer> offers = agent.getTradingSystem().getActiveOffers();
            for (TradingSystem.TradeOffer offer : offers) {
                String route = offer.offeredItem + "_to_" + offer.requestedItem;
                tradeRoutes.merge(route, 1, Integer::sum);
            }
            
            // Identify resource specialists
            analyzeAgentSpecialization(agent, resourceSpecialists);
        }
        
        // Detect trade route patterns
        for (Map.Entry<String, Integer> entry : tradeRoutes.entrySet()) {
            if (entry.getValue() >= PATTERN_DETECTION_THRESHOLD) {
                String patternId = "trade_route_" + entry.getKey();
                
                if (!detectedPatterns.containsKey(patternId)) {
                    CommunityPattern pattern = new CommunityPattern(
                        patternId,
                        "TRADE_ROUTE",
                        "Established trade route: " + entry.getKey(),
                        new HashSet<>(), // Agents TBD
                        new BlockPos(0, 0, 0), // Center TBD
                        currentTime
                    );
                    
                    detectedPatterns.put(patternId, pattern);
                    recordEmergentEvent("TRADE_ROUTE_ESTABLISHED",
                        "Regular trade route formed: " + entry.getKey(),
                        new HashSet<>(), new BlockPos(0, 0, 0), currentTime);
                }
            }
        }
        
        // Detect resource specialization
        for (Map.Entry<Item, List<AgentEntity>> entry : resourceSpecialists.entrySet()) {
            if (entry.getValue().size() >= 2) {
                String patternId = "specialization_" + entry.getKey().toString();
                
                if (!detectedPatterns.containsKey(patternId)) {
                    Set<UUID> specialists = new HashSet<>();
                    entry.getValue().forEach(agent -> specialists.add(agent.getUUID()));
                    
                    CommunityPattern pattern = new CommunityPattern(
                        patternId,
                        "RESOURCE_SPECIALIZATION",
                        "Specialization in " + entry.getKey().getDescription().getString(),
                        specialists,
                        findCenterPoint(agents, specialists),
                        currentTime
                    );
                    
                    detectedPatterns.put(patternId, pattern);
                    recordEmergentEvent("SPECIALIZATION_EMERGED",
                        "Resource specialization formed: " + entry.getKey().getDescription().getString(),
                        specialists, pattern.epicenter, currentTime);
                }
            }
        }
        
        // Update productivity metric
        double economicActivity = (double) tradeRoutes.size() / Math.max(1, agents.size());
        metrics.updateMetric("productivity", (economicActivity - 0.5) * 0.05);
    }
    
    private void analyzeSocialHierarchies(List<AgentEntity> agents, long currentTime) {
        Map<UUID, Integer> leadershipCounts = new HashMap<>();
        Map<UUID, Double> reputationScores = new HashMap<>();
        
        for (AgentEntity agent : agents) {
            // Count leadership roles
            long leadershipCount = agent.getCoordinationSystem().getActiveProjects().stream()
                .filter(project -> project.projectLeader.equals(agent.getUUID()))
                .count();
            
            if (leadershipCount > 0) {
                leadershipCounts.put(agent.getUUID(), (int) leadershipCount);
            }
            
            // Calculate overall reputation
            int reputation = agent.getGossipSystem().getReputation(agent.getUUID());
            reputationScores.put(agent.getUUID(), reputation / 100.0);
        }
        
        // Identify natural leaders
        leadershipCounts.entrySet().stream()
            .filter(entry -> entry.getValue() >= 2) // Led 2+ projects
            .forEach(entry -> {
                String patternId = "natural_leader_" + entry.getKey();
                
                if (!detectedPatterns.containsKey(patternId)) {
                    CommunityPattern pattern = new CommunityPattern(
                        patternId,
                        "NATURAL_LEADERSHIP",
                        "Natural leader emerged",
                        Set.of(entry.getKey()),
                        findAgentPosition(agents, entry.getKey()),
                        currentTime
                    );
                    
                    detectedPatterns.put(patternId, pattern);
                    recordEmergentEvent("LEADER_EMERGED",
                        "Natural leader recognized in community",
                        Set.of(entry.getKey()), pattern.epicenter, currentTime);
                }
            });
        
        // Identify reputation-based hierarchy
        List<UUID> topReputation = reputationScores.entrySet().stream()
            .filter(entry -> entry.getValue() > 0.7)
            .map(Map.Entry::getKey)
            .toList();
        
        if (topReputation.size() >= 3) {
            String patternId = "reputation_hierarchy";
            
            if (!detectedPatterns.containsKey(patternId)) {
                CommunityPattern pattern = new CommunityPattern(
                    patternId,
                    "REPUTATION_HIERARCHY",
                    "Social hierarchy based on reputation",
                    new HashSet<>(topReputation),
                    findCenterPoint(agents, new HashSet<>(topReputation)),
                    currentTime
                );
                
                detectedPatterns.put(patternId, pattern);
                recordEmergentEvent("HIERARCHY_FORMED",
                    "Social hierarchy established based on reputation",
                    new HashSet<>(topReputation), pattern.epicenter, currentTime);
            }
        }
        
        // Update leadership metric
        double leadershipDensity = (double) leadershipCounts.size() / Math.max(1, agents.size());
        metrics.updateMetric("leadership", (leadershipDensity - 0.3) * 0.1);
    }
    
    private void analyzeInnovationPatterns(List<AgentEntity> agents, long currentTime) {
        Set<String> uniqueProjectTypes = new HashSet<>();
        Set<String> uniqueProfessions = new HashSet<>();
        Map<String, Integer> adaptationEvents = new HashMap<>();
        
        for (AgentEntity agent : agents) {
            // Collect project type diversity
            agent.getCoordinationSystem().getActiveProjects().forEach(project -> 
                uniqueProjectTypes.add(project.type.name()));
            
            // Collect profession diversity
            if (agent.getProfessionSystem().hasProfession()) {
                uniqueProfessions.add(agent.getProfessionSystem().getCurrentProfession().getName());
            }
            
            // Look for adaptation events in recent gossip
            agent.getGossipSystem().getGossipByType(GossipSystem.GossipType.GENERAL_NEWS)
                .forEach(gossip -> {
                    if (gossip.content.contains("new") || gossip.content.contains("innovative")) {
                        adaptationEvents.merge("innovation", 1, Integer::sum);
                    }
                });
        }
        
        // Detect innovation pattern
        if (uniqueProjectTypes.size() >= 3 || adaptationEvents.getOrDefault("innovation", 0) >= 3) {
            String patternId = "innovation_culture";
            
            CommunityPattern pattern = detectedPatterns.get(patternId);
            if (pattern == null) {
                pattern = new CommunityPattern(
                    patternId,
                    "INNOVATION_CULTURE",
                    "Culture of innovation and adaptation",
                    agents.stream().map(AgentEntity::getUUID).collect(HashSet::new, Set::add, Set::addAll),
                    findCommunityCenter(agents),
                    currentTime
                );
                
                detectedPatterns.put(patternId, pattern);
                recordEmergentEvent("INNOVATION_CULTURE_FORMED",
                    "Community developed culture of innovation",
                    pattern.involvedAgents, pattern.epicenter, currentTime);
            } else {
                pattern.reinforce(currentTime);
            }
        }
        
        // Update innovation and diversity metrics
        double diversityScore = (uniqueProjectTypes.size() + uniqueProfessions.size()) / 10.0;
        metrics.updateMetric("diversity", (diversityScore - 0.5) * 0.05);
        
        double innovationScore = adaptationEvents.getOrDefault("innovation", 0) / 10.0;
        metrics.updateMetric("innovation", innovationScore * 0.1);
    }
    
    private void updateCommunityMetrics(List<AgentEntity> agents) {
        // Update cohesion based on clustering
        long clusterCount = detectedPatterns.values().stream()
            .filter(p -> p.type.equals("SPATIAL_CLUSTERING"))
            .filter(CommunityPattern::isActive)
            .count();
        
        double cohesionChange = (clusterCount - 2.0) * 0.01; // Optimal ~2 clusters
        metrics.updateMetric("cohesion", cohesionChange);
        
        // Update stability based on pattern consistency
        double averagePatternStrength = detectedPatterns.values().stream()
            .mapToDouble(p -> p.strength)
            .average()
            .orElse(0.5);
        
        metrics.updateMetric("stability", (averagePatternStrength - 0.5) * 0.05);
        
        // Update resilience based on active patterns during challenges
        // (This would need integration with challenge/emergency detection)
        
        // Log metrics periodically for analysis
        if (agents.size() > 0 && agents.get(0).level().getGameTime() % 2400 == 0) { // Every 2 minutes
            logCommunityMetrics();
        }
    }
    
    private void checkForTraditionFormation(long currentTime) {
        // Analyze emergent events for tradition patterns
        Map<String, List<EmergentEvent>> eventsByType = new HashMap<>();
        
        emergentEvents.forEach(event -> 
            eventsByType.computeIfAbsent(event.type, k -> new ArrayList<>()).add(event));
        
        for (Map.Entry<String, List<EmergentEvent>> entry : eventsByType.entrySet()) {
            if (entry.getValue().size() >= TRADITION_FORMATION_EVENTS) {
                String traditionName = generateTraditionName(entry.getKey());
                
                if (!traditions.containsKey(traditionName)) {
                    CommunityTradition tradition = new CommunityTradition(
                        traditionName,
                        "Community tradition based on " + entry.getKey(),
                        entry.getKey()
                    );
                    
                    // Set up tradition activities
                    setupTraditionActivities(tradition, entry.getValue());
                    
                    traditions.put(traditionName, tradition);
                    
                    recordEmergentEvent("TRADITION_FORMED",
                        "New community tradition established: " + traditionName,
                        new HashSet<>(), new BlockPos(0, 0, 0), currentTime);
                }
            }
        }
    }
    
    private void decayPatterns(long currentTime) {
        // Decay patterns that haven't been reinforced recently
        detectedPatterns.values().forEach(pattern -> {
            if (currentTime - pattern.lastOccurrence > PATTERN_TIMEOUT) {
                pattern.decay();
            }
        });
        
        // Remove very weak patterns
        detectedPatterns.entrySet().removeIf(entry -> entry.getValue().strength < 0.1);
    }
    
    // === HELPER METHODS ===
    
    private void recordEmergentEvent(String type, String description, 
                                   Set<UUID> participants, BlockPos location, long timestamp) {
        EmergentEvent event = new EmergentEvent(type, description, participants, location, timestamp);
        emergentEvents.add(event);
        
        // Keep only recent events
        if (emergentEvents.size() > MAX_EMERGENT_EVENTS) {
            emergentEvents.remove(0);
        }
    }
    
    private void establishCommunityHub(BlockPos location, List<AgentEntity> agents, long timestamp) {
        // Create special pattern for community hub
        Set<UUID> hubUsers = new HashSet<>();
        agents.forEach(agent -> hubUsers.add(agent.getUUID()));
        
        CommunityPattern hub = new CommunityPattern(
            "community_hub_" + location.toString(),
            "COMMUNITY_HUB",
            "Established community hub at " + location,
            hubUsers,
            location,
            timestamp
        );
        
        hub.strength = 1.0; // Maximum strength
        detectedPatterns.put(hub.id, hub);
        
        recordEmergentEvent("HUB_ESTABLISHED",
            "Community hub established at " + location,
            hubUsers, location, timestamp);
    }
    
    private void establishWorkGroup(Set<UUID> members, long timestamp) {
        String groupId = "work_group_" + members.hashCode();
        
        recordEmergentEvent("WORK_GROUP_FORMED",
            "Permanent work group of " + members.size() + " members established",
            members, new BlockPos(0, 0, 0), timestamp);
    }
    
    private void analyzeAgentSpecialization(AgentEntity agent, Map<Item, List<AgentEntity>> specialists) {
        // Check if agent shows specialization in certain resources
        if (agent.getProfessionSystem().hasProfession()) {
            Item[] professionItems = agent.getProfessionSystem().getCurrentProfession().getProfessionItems();
            
            for (Item item : professionItems) {
                int itemCount = getItemCount(agent, item);
                if (itemCount > 16) { // Has significant quantity
                    specialists.computeIfAbsent(item, k -> new ArrayList<>()).add(agent);
                }
            }
        }
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
    
    private BlockPos findCenterPoint(List<AgentEntity> agents, Set<UUID> targetAgents) {
        List<AgentEntity> relevantAgents = agents.stream()
            .filter(agent -> targetAgents.contains(agent.getUUID()))
            .toList();
        
        if (relevantAgents.isEmpty()) {
            return new BlockPos(0, 0, 0);
        }
        
        double avgX = relevantAgents.stream().mapToDouble(AgentEntity::getX).average().orElse(0);
        double avgY = relevantAgents.stream().mapToDouble(AgentEntity::getY).average().orElse(0);
        double avgZ = relevantAgents.stream().mapToDouble(AgentEntity::getZ).average().orElse(0);
        
        return new BlockPos((int)avgX, (int)avgY, (int)avgZ);
    }
    
    private BlockPos findAgentPosition(List<AgentEntity> agents, UUID targetAgent) {
        return agents.stream()
            .filter(agent -> agent.getUUID().equals(targetAgent))
            .findFirst()
            .map(AgentEntity::blockPosition)
            .orElse(new BlockPos(0, 0, 0));
    }
    
    private BlockPos findCommunityCenter(List<AgentEntity> agents) {
        if (agents.isEmpty()) {
            return new BlockPos(0, 0, 0);
        }
        
        double avgX = agents.stream().mapToDouble(AgentEntity::getX).average().orElse(0);
        double avgY = agents.stream().mapToDouble(AgentEntity::getY).average().orElse(0);
        double avgZ = agents.stream().mapToDouble(AgentEntity::getZ).average().orElse(0);
        
        return new BlockPos((int)avgX, (int)avgY, (int)avgZ);
    }
    
    private String generateTraditionName(String eventType) {
        switch (eventType) {
            case "CLUSTER_FORMED": return "Community Gathering Tradition";
            case "TEAM_FORMED": return "Collaborative Work Tradition";
            case "TRADE_ROUTE_ESTABLISHED": return "Trading Circle Tradition";
            case "LEADER_EMERGED": return "Leadership Recognition Tradition";
            case "PROJECT_COMPLETED": return "Achievement Celebration Tradition";
            default: return "Community " + eventType.toLowerCase().replace("_", " ") + " Tradition";
        }
    }
    
    private void setupTraditionActivities(CommunityTradition tradition, List<EmergentEvent> events) {
        switch (tradition.trigger) {
            case "CLUSTER_FORMED":
                tradition.activities.addAll(Arrays.asList(
                    "Community gathering", "Resource sharing", "Group planning"
                ));
                break;
            case "TEAM_FORMED":
                tradition.activities.addAll(Arrays.asList(
                    "Team building", "Skill sharing", "Collaborative projects"
                ));
                break;
            case "PROJECT_COMPLETED":
                tradition.activities.addAll(Arrays.asList(
                    "Success celebration", "Achievement recognition", "Community feast"
                ));
                break;
            default:
                tradition.activities.add("Community gathering");
        }
    }
    
    private void logCommunityMetrics() {
        // This would log to console or file for analysis
        // For now, just a simple placeholder
        System.out.println("Community Health Score: " + 
            String.format("%.2f", metrics.getOverallHealthScore()));
    }
    
    // === PUBLIC INTERFACE ===
    
    public List<CommunityPattern> getActivePatterns() {
        return detectedPatterns.values().stream()
            .filter(CommunityPattern::isActive)
            .toList();
    }
    
    public CommunityMetrics getCommunityMetrics() {
        return metrics;
    }
    
    public List<EmergentEvent> getRecentEmergentEvents() {
        return new ArrayList<>(emergentEvents);
    }
    
    public Map<String, CommunityTradition> getCommunityTraditions() {
        return new HashMap<>(traditions);
    }
    
    public CommunityPattern getPattern(String patternId) {
        return detectedPatterns.get(patternId);
    }
    
    public void forcePatternDetection(String type, Set<UUID> agents, BlockPos location, long timestamp) {
        String patternId = type.toLowerCase() + "_" + timestamp;
        
        CommunityPattern pattern = new CommunityPattern(
            patternId,
            type,
            "Manually detected pattern: " + type,
            agents,
            location,
            timestamp
        );
        
        detectedPatterns.put(patternId, pattern);
        recordEmergentEvent("PATTERN_DETECTED", "Pattern manually detected: " + type, 
                          agents, location, timestamp);
    }
    
    /**
     * Get community status summary
     */
    public String getCommunityStatusSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("=== Community Status ===\n");
        summary.append(String.format("Health Score: %.2f\n", metrics.getOverallHealthScore()));
        summary.append(String.format("Active Patterns: %d\n", getActivePatterns().size()));
        summary.append(String.format("Traditions: %d\n", traditions.size()));
        summary.append(String.format("Recent Events: %d\n", emergentEvents.size()));
        
        summary.append("\n=== Key Metrics ===\n");
        summary.append(String.format("Cohesion: %.2f\n", metrics.cohesion));
        summary.append(String.format("Cooperation: %.2f\n", metrics.cooperation));
        summary.append(String.format("Leadership: %.2f\n", metrics.leadership));
        summary.append(String.format("Innovation: %.2f\n", metrics.innovation));
        summary.append(String.format("Stability: %.2f\n", metrics.stability));

        return summary.toString();
    }

    /**
     * Trigger community-wide events based on patterns
     */
    public void triggerCommunityEvent(String eventType, List<AgentEntity> agents, long currentTime) {
        switch (eventType.toLowerCase()) {
            case "celebration":
                triggerCelebration(agents, currentTime);
                break;
            case "emergency_response":
                triggerEmergencyResponse(agents, currentTime);
                break;
            case "innovation_festival":
                triggerInnovationFestival(agents, currentTime);
                break;
            case "tradition_ceremony":
                triggerTraditionCeremony(agents, currentTime);
                break;
            case "community_meeting":
                triggerCommunityMeeting(agents, currentTime);
                break;
        }
    }

    private void triggerCelebration(List<AgentEntity> agents, long currentTime) {
        if (agents.isEmpty()) return;

        BlockPos celebrationCenter = findCommunityCenter(agents);
        Set<UUID> participants = agents.stream()
                .map(AgentEntity::getUUID)
                .collect(HashSet::new, Set::add, Set::addAll);

        // Initiate community celebration project
        AgentEntity organizer = agents.get(new Random().nextInt(agents.size()));
        CoordinationSystem.GroupProject celebration =
                organizer.getCoordinationSystem().initiateCelebration(
                        organizer, "Community Achievement", celebrationCenter);

        if (celebration != null) {
            // Invite everyone
            agents.forEach(agent -> {
                if (agent != organizer) {
                    celebration.invitedAgents.add(agent.getUUID());
                }
            });

            recordEmergentEvent("COMMUNITY_CELEBRATION",
                    "Community-wide celebration triggered",
                    participants, celebrationCenter, currentTime);

            // Update community metrics positively
            metrics.updateMetric("cohesion", 0.1);
            metrics.updateMetric("cooperation", 0.05);
        }
    }

    private void triggerEmergencyResponse(List<AgentEntity> agents, long currentTime) {
        if (agents.isEmpty()) return;

        // Simulate emergency at community edge
        BlockPos center = findCommunityCenter(agents);
        BlockPos emergencyLocation = center.offset(
                40 + new Random().nextInt(20), 0, 40 + new Random().nextInt(20));

        String[] emergencyTypes = {"Monster attack", "Fire hazard", "Resource shortage", "Structural damage"};
        String emergencyType = emergencyTypes[new Random().nextInt(emergencyTypes.length)];

        // Find best responder (highest reputation or leadership)
        AgentEntity responder = agents.stream()
                .max((a1, a2) -> {
                    int rep1 = a1.getGossipSystem().getReputation(a1.getUUID());
                    int rep2 = a2.getGossipSystem().getReputation(a2.getUUID());
                    return Integer.compare(rep1, rep2);
                })
                .orElse(agents.get(0));

        // Initiate emergency response
        CoordinationSystem.GroupProject emergency =
                responder.getCoordinationSystem().initiateEmergencyResponse(
                        responder, emergencyType, emergencyLocation, "Community emergency response");

        if (emergency != null) {
            recordEmergentEvent("EMERGENCY_RESPONSE",
                    "Community emergency response activated: " + emergencyType,
                    Set.of(responder.getUUID()), emergencyLocation, currentTime);

            // Test resilience
            metrics.updateMetric("resilience", 0.1);
            metrics.updateMetric("leadership", 0.05);
        }
    }

    private void triggerInnovationFestival(List<AgentEntity> agents, long currentTime) {
        if (agents.isEmpty()) return;

        BlockPos festivalLocation = findCommunityCenter(agents);

        // Find most innovative agents (those with diverse professions/projects)
        List<AgentEntity> innovators = agents.stream()
                .filter(agent -> agent.getProfessionSystem().hasProfession())
                .filter(agent -> hasInnovativeHistory(agent))
                .limit(5)
                .toList();

        if (!innovators.isEmpty()) {
            AgentEntity organizer = innovators.get(0);

            // Create innovation showcase project
            CoordinationSystem.GroupProject festival =
                    organizer.getCoordinationSystem().initiateCelebration(
                            organizer, "Innovation Festival", festivalLocation);

            if (festival != null) {
                festival.projectData.put("festival_type", "innovation");
                festival.projectData.put("featured_innovations", new ArrayList<String>());

                // Add innovation activities
                List<String> innovations = Arrays.asList(
                        "New building techniques", "Efficient resource gathering",
                        "Creative problem solving", "Novel collaboration methods"
                );
                festival.projectData.put("featured_innovations", innovations);

                recordEmergentEvent("INNOVATION_FESTIVAL",
                        "Community innovation festival organized",
                        innovators.stream().map(AgentEntity::getUUID).collect(HashSet::new, Set::add, Set::addAll),
                        festivalLocation, currentTime);

                metrics.updateMetric("innovation", 0.15);
                metrics.updateMetric("diversity", 0.1);
            }
        }
    }

    private void triggerTraditionCeremony(List<AgentEntity> agents, long currentTime) {
        if (traditions.isEmpty() || agents.isEmpty()) return;

        // Select most significant tradition
        CommunityTradition tradition = traditions.values().stream()
                .max(Comparator.comparingDouble(t -> t.significance))
                .orElse(null);

        if (tradition != null) {
            BlockPos ceremonyLocation = findCommunityCenter(agents);

            // Find tradition practitioners
            List<AgentEntity> practitioners = agents.stream()
                    .filter(agent -> tradition.practitioners.contains(agent.getUUID()))
                    .toList();

            if (!practitioners.isEmpty()) {
                AgentEntity ceremonyLeader = practitioners.get(0);

                // Organize tradition ceremony
                CoordinationSystem.GroupProject ceremony =
                        ceremonyLeader.getCoordinationSystem().initiateCelebration(
                                ceremonyLeader, "Traditional " + tradition.name, ceremonyLocation);

                if (ceremony != null) {
                    ceremony.projectData.put("tradition", tradition.name);
                    ceremony.projectData.put("activities", tradition.activities);

                    // Perform the tradition
                    tradition.perform(practitioners.stream().map(AgentEntity::getUUID)
                            .collect(HashSet::new, Set::add, Set::addAll), currentTime);

                    recordEmergentEvent("TRADITION_CEREMONY",
                            "Traditional ceremony performed: " + tradition.name,
                            ceremony.participants, ceremonyLocation, currentTime);

                    metrics.updateMetric("stability", 0.1);
                    metrics.updateMetric("cohesion", 0.08);
                }
            }
        }
    }

    private void triggerCommunityMeeting(List<AgentEntity> agents, long currentTime) {
        if (agents.isEmpty()) return;

        BlockPos meetingLocation = findCommunityCenter(agents);

        // Find natural leader to organize meeting
        AgentEntity leader = agents.stream()
                .filter(agent -> {
                    CoordinationSystem.LeadershipProfile profile =
                            agent.getCoordinationSystem().getLeadershipProfile(agent.getUUID());
                    return profile != null && profile.leadershipScore > 60;
                })
                .max((a1, a2) -> {
                    CoordinationSystem.LeadershipProfile p1 =
                            a1.getCoordinationSystem().getLeadershipProfile(a1.getUUID());
                    CoordinationSystem.LeadershipProfile p2 =
                            a2.getCoordinationSystem().getLeadershipProfile(a2.getUUID());
                    return Integer.compare(p1.leadershipScore, p2.leadershipScore);
                })
                .orElse(agents.get(0));

        // Create community meeting project
        CoordinationSystem.GroupProject meeting =
                leader.getCoordinationSystem().initiateCelebration(
                        leader, "Community Meeting", meetingLocation);

        if (meeting != null) {
            meeting.projectData.put("meeting_type", "community_planning");
            meeting.projectData.put("agenda", Arrays.asList(
                    "Review community progress",
                    "Plan future projects",
                    "Address community concerns",
                    "Strengthen cooperation"
            ));

            // Invite key community members
            agents.stream()
                    .filter(agent -> agent != leader)
                    .filter(agent -> agent.getGossipSystem().getReputation(agent.getUUID()) > 30)
                    .forEach(agent -> meeting.invitedAgents.add(agent.getUUID()));

            recordEmergentEvent("COMMUNITY_MEETING",
                    "Community meeting organized for planning and coordination",
                    Set.of(leader.getUUID()), meetingLocation, currentTime);

            metrics.updateMetric("leadership", 0.1);
            metrics.updateMetric("cooperation", 0.08);
        }
    }

    private boolean hasInnovativeHistory(AgentEntity agent) {
        // Check if agent has shown innovative behavior
        List<CoordinationSystem.GroupProject> projects =
                agent.getCoordinationSystem().getActiveProjects();

        // Count different project types participated in
        long diverseProjects = projects.stream()
                .filter(project -> project.participants.contains(agent.getUUID()))
                .map(project -> project.type)
                .distinct()
                .count();

        return diverseProjects >= 2 || // Participated in 2+ different project types
                agent.getProfessionSystem().hasProfession(); // Has developed a profession
    }

    /**
     * Adaptive community responses to challenges
     */
    public void respondToCommunityChallenge(String challengeType, List<AgentEntity> agents,
                                            BlockPos challengeLocation, long currentTime) {

        switch (challengeType.toLowerCase()) {
            case "resource_scarcity":
                organizeResourceRecovery(agents, challengeLocation, currentTime);
                break;
            case "external_threat":
                organizeDefenseResponse(agents, challengeLocation, currentTime);
                break;
            case "leadership_crisis":
                facilitateLeadershipTransition(agents, currentTime);
                break;
            case "social_conflict":
                organizePeacekeeping(agents, currentTime);
                break;
            case "infrastructure_failure":
                organizeRepairEffort(agents, challengeLocation, currentTime);
                break;
        }

        // Test and update resilience
        metrics.updateMetric("resilience", 0.05);
    }

    private void organizeResourceRecovery(List<AgentEntity> agents, BlockPos location, long currentTime) {
        // Find agents with gathering professions
        List<AgentEntity> gatherers = agents.stream()
                .filter(agent -> agent.getProfessionSystem().hasProfession())
                .filter(agent -> {
                    String profession = agent.getProfessionSystem().getCurrentProfession().getName();
                    return profession.equals("Miner") || profession.equals("Gatherer") ||
                            profession.equals("Farmer");
                })
                .toList();

        if (!gatherers.isEmpty()) {
            AgentEntity coordinator = gatherers.get(0);

            CoordinationSystem.GroupProject recovery =
                    coordinator.getCoordinationSystem().initiateResourceExpedition(
                            coordinator, net.minecraft.world.item.Items.IRON_INGOT, location);

            if (recovery != null) {
                recovery.projectData.put("challenge_response", "resource_scarcity");
                recovery.priority = CoordinationSystem.TaskPriority.HIGH;

                recordEmergentEvent("RESOURCE_RECOVERY",
                        "Community organized resource recovery effort",
                        gatherers.stream().map(AgentEntity::getUUID).collect(HashSet::new, Set::add, Set::addAll),
                        location, currentTime);
            }
        }
    }

    private void organizeDefenseResponse(List<AgentEntity> agents, BlockPos threat, long currentTime) {
        // Find strongest/most capable defenders
        List<AgentEntity> defenders = agents.stream()
                .filter(agent -> agent.getHealth() > agent.getMaxHealth() * 0.8)
                .sorted((a1, a2) -> Float.compare(a2.getHealth(), a1.getHealth()))
                .limit(Math.max(3, agents.size() / 2))
                .toList();

        if (!defenders.isEmpty()) {
            AgentEntity defenseLeader = defenders.get(0);

            CoordinationSystem.GroupProject defense =
                    defenseLeader.getCoordinationSystem().initiateProject(
                            defenseLeader, CoordinationSystem.ProjectType.DEFENSE_PATROL,
                            threat, "Emergency defense against external threat");

            if (defense != null) {
                defense.priority = CoordinationSystem.TaskPriority.CRITICAL;

                // Auto-recruit all capable defenders
                defenders.forEach(defender -> {
                    if (defender != defenseLeader) {
                        defense.invitedAgents.add(defender.getUUID());
                    }
                });

                recordEmergentEvent("DEFENSE_MOBILIZED",
                        "Community defense mobilized against external threat",
                        defenders.stream().map(AgentEntity::getUUID).collect(HashSet::new, Set::add, Set::addAll),
                        threat, currentTime);

                metrics.updateMetric("resilience", 0.15);
            }
        }
    }

    private void facilitateLeadershipTransition(List<AgentEntity> agents, long currentTime) {
        // Find potential new leaders
        List<AgentEntity> candidates = agents.stream()
                .filter(agent -> {
                    CoordinationSystem.LeadershipProfile profile =
                            agent.getCoordinationSystem().getLeadershipProfile(agent.getUUID());
                    return profile != null && profile.leadershipScore > 40;
                })
                .filter(agent -> agent.getGossipSystem().getReputation(agent.getUUID()) > 40)
                .sorted((a1, a2) -> {
                    CoordinationSystem.LeadershipProfile p1 =
                            a1.getCoordinationSystem().getLeadershipProfile(a1.getUUID());
                    CoordinationSystem.LeadershipProfile p2 =
                            a2.getCoordinationSystem().getLeadershipProfile(a2.getUUID());
                    return Integer.compare(p2.leadershipScore, p1.leadershipScore);
                })
                .limit(3)
                .toList();

        if (!candidates.isEmpty()) {
            AgentEntity newLeader = candidates.get(0);

            recordEmergentEvent("LEADERSHIP_TRANSITION",
                    "New community leader emerged: " + newLeader.getBaseName(),
                    candidates.stream().map(AgentEntity::getUUID).collect(HashSet::new, Set::add, Set::addAll),
                    newLeader.blockPosition(), currentTime);

            // Boost leadership metrics
            metrics.updateMetric("leadership", 0.2);
            metrics.updateMetric("stability", 0.1);
        }
    }

    private void organizePeacekeeping(List<AgentEntity> agents, long currentTime) {
        // Find diplomatic agents
        List<AgentEntity> diplomats = agents.stream()
                .filter(agent -> {
                    CoordinationSystem.LeadershipProfile profile =
                            agent.getCoordinationSystem().getLeadershipProfile(agent.getUUID());
                    return profile != null && profile.isDiplomatic;
                })
                .toList();

        if (diplomats.isEmpty()) {
            // Fall back to well-regarded agents
            diplomats = agents.stream()
                    .filter(agent -> agent.getGossipSystem().getReputation(agent.getUUID()) > 60)
                    .limit(2)
                    .toList();
        }

        if (!diplomats.isEmpty()) {
            AgentEntity mediator = diplomats.get(0);
            BlockPos mediationLocation = findCommunityCenter(agents);

            CoordinationSystem.GroupProject peacemaking =
                    mediator.getCoordinationSystem().initiateCelebration(
                            mediator, "Community Reconciliation", mediationLocation);

            if (peacemaking != null) {
                peacemaking.projectData.put("purpose", "conflict_resolution");

                recordEmergentEvent("PEACEMAKING_EFFORT",
                        "Community peacemaking effort organized",
                        diplomats.stream().map(AgentEntity::getUUID).collect(HashSet::new, Set::add, Set::addAll),
                        mediationLocation, currentTime);

                metrics.updateMetric("cohesion", 0.15);
                metrics.updateMetric("stability", 0.1);
            }
        }
    }

    private void organizeRepairEffort(List<AgentEntity> agents, BlockPos failureLocation, long currentTime) {
        // Find builders and engineers
        List<AgentEntity> builders = agents.stream()
                .filter(agent -> agent.getProfessionSystem().hasProfession())
                .filter(agent -> {
                    String profession = agent.getProfessionSystem().getCurrentProfession().getName();
                    return profession.equals("Builder") || profession.equals("Engineer") ||
                            profession.equals("Architect");
                })
                .toList();

        if (builders.isEmpty()) {
            // Fall back to any capable agents
            builders = agents.stream()
                    .filter(agent -> hasGatheringTools(agent) || hasBuildingMaterials(agent))
                    .limit(4)
                    .toList();
        }

        if (!builders.isEmpty()) {
            AgentEntity repairLeader = builders.get(0);

            CoordinationSystem.GroupProject repair =
                    repairLeader.getCoordinationSystem().initiateCommunityBuild(
                            repairLeader, "Infrastructure Repair", failureLocation,
                            Arrays.asList(net.minecraft.world.item.Items.STONE,
                                    net.minecraft.world.item.Items.OAK_PLANKS));

            if (repair != null) {
                repair.projectData.put("repair_type", "infrastructure_restoration");
                repair.priority = CoordinationSystem.TaskPriority.HIGH;

                recordEmergentEvent("REPAIR_MOBILIZED",
                        "Community infrastructure repair effort mobilized",
                        builders.stream().map(AgentEntity::getUUID).collect(HashSet::new, Set::add, Set::addAll),
                        failureLocation, currentTime);

                metrics.updateMetric("resilience", 0.1);
                metrics.updateMetric("productivity", 0.05);
            }
        }
    }

    private boolean hasGatheringTools(AgentEntity agent) {
        return agent.getInventory().hasAnyOf(Set.of(
                net.minecraft.world.item.Items.WOODEN_PICKAXE,
                net.minecraft.world.item.Items.STONE_PICKAXE,
                net.minecraft.world.item.Items.IRON_PICKAXE,
                net.minecraft.world.item.Items.WOODEN_AXE,
                net.minecraft.world.item.Items.STONE_AXE,
                net.minecraft.world.item.Items.IRON_AXE
        ));
    }

    private boolean hasBuildingMaterials(AgentEntity agent) {
        return agent.getInventory().hasAnyOf(Set.of(
                net.minecraft.world.item.Items.COBBLESTONE,
                net.minecraft.world.item.Items.OAK_PLANKS,
                net.minecraft.world.item.Items.OAK_LOG,
                net.minecraft.world.item.Items.STONE
        ));
    }
}