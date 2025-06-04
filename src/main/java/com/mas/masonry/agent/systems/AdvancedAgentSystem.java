package com.mas.masonry.agent.systems;

import com.mas.masonry.AgentEntity;
import com.mas.masonry.MASONRY;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Master system that coordinates all advanced agent behaviors and emergent community dynamics
 */
public class AdvancedAgentSystem {
    private final Map<UUID, AgentProfile> agentProfiles = new ConcurrentHashMap<>();
    private final EmergentBehaviorSystem emergentSystem = new EmergentBehaviorSystem();
    private final CommunityEvolutionTracker evolutionTracker = new CommunityEvolutionTracker();
    
    // Global timers and intervals
    private static final int EMERGENT_ANALYSIS_INTERVAL = 1200; // Every minute
    private static final int COMMUNITY_EVENT_INTERVAL = 6000;   // Every 5 minutes
    private static final int EVOLUTION_CHECK_INTERVAL = 12000;  // Every 10 minutes
    private static final int CHALLENGE_GENERATION_INTERVAL = 24000; // Every 20 minutes
    
    private long lastEmergentAnalysis = 0;
    private long lastCommunityEvent = 0;
    private long lastEvolutionCheck = 0;
    private long lastChallengeGeneration = 0;
    
    public static class AgentProfile {
        public final UUID agentId;
        public final String agentName;
        public final Map<String, Double> behaviorTendencies = new HashMap<>();
        public final List<String> participatedProjects = new ArrayList<>();
        public final Map<String, Integer> skillExperience = new HashMap<>();
        public final Set<UUID> preferredCollaborators = new HashSet<>();
        public final List<String> innovationHistory = new ArrayList<>();
        
        public double adaptability = 0.5;
        public double socialInfluence = 0.5;
        public double problemSolvingAbility = 0.5;
        public double communityCommitment = 0.5;
        public long totalExperiencePoints = 0;
        
        public AgentProfile(UUID id, String name) {
            this.agentId = id;
            this.agentName = name;
            
            // Initialize behavior tendencies
            behaviorTendencies.put("leadership", 0.3 + new Random().nextDouble() * 0.4);
            behaviorTendencies.put("cooperation", 0.4 + new Random().nextDouble() * 0.4);
            behaviorTendencies.put("innovation", 0.2 + new Random().nextDouble() * 0.6);
            behaviorTendencies.put("exploration", 0.3 + new Random().nextDouble() * 0.4);
            behaviorTendencies.put("building", 0.3 + new Random().nextDouble() * 0.4);
            behaviorTendencies.put("trading", 0.2 + new Random().nextDouble() * 0.6);
        }
        
        public void gainExperience(String skillType, int amount) {
            skillExperience.merge(skillType, amount, Integer::sum);
            totalExperiencePoints += amount;
            
            // Improve related abilities
            switch (skillType.toLowerCase()) {
                case "leadership":
                    socialInfluence = Math.min(1.0, socialInfluence + 0.01);
                    behaviorTendencies.put("leadership", 
                        Math.min(1.0, behaviorTendencies.get("leadership") + 0.02));
                    break;
                case "collaboration":
                    communityCommitment = Math.min(1.0, communityCommitment + 0.01);
                    behaviorTendencies.put("cooperation", 
                        Math.min(1.0, behaviorTendencies.get("cooperation") + 0.02));
                    break;
                case "problem_solving":
                    problemSolvingAbility = Math.min(1.0, problemSolvingAbility + 0.01);
                    adaptability = Math.min(1.0, adaptability + 0.01);
                    break;
                case "innovation":
                    behaviorTendencies.put("innovation", 
                        Math.min(1.0, behaviorTendencies.get("innovation") + 0.02));
                    break;
            }
        }
        
        public double getBehaviorTendency(String behavior) {
            return behaviorTendencies.getOrDefault(behavior, 0.5);
        }
        
        public int getSkillLevel(String skill) {
            return skillExperience.getOrDefault(skill, 0) / 100; // 100 XP per level
        }
    }
    
    public static class CommunityEvolutionTracker {
        public final Map<String, Double> evolutionaryPressures = new HashMap<>();
        public final List<EvolutionEvent> evolutionHistory = new ArrayList<>();
        public double evolutionRate = 1.0;
        
        public static class EvolutionEvent {
            public final String type;
            public final String description;
            public final long timestamp;
            public final double impact;
            
            public EvolutionEvent(String type, String description, long timestamp, double impact) {
                this.type = type;
                this.description = description;
                this.timestamp = timestamp;
                this.impact = impact;
            }
        }
        
        public void addEvolutionaryPressure(String pressureType, double intensity) {
            evolutionaryPressures.merge(pressureType, intensity, Double::sum);
        }
        
        public void recordEvolutionEvent(String type, String description, long timestamp, double impact) {
            evolutionHistory.add(new EvolutionEvent(type, description, timestamp, impact));
            
            // Keep only recent history
            if (evolutionHistory.size() > 50) {
                evolutionHistory.remove(0);
            }
        }
        
        public double getTotalEvolutionaryPressure() {
            return evolutionaryPressures.values().stream().mapToDouble(Double::doubleValue).sum();
        }
    }
    
    /**
     * Main system tick - coordinates all advanced behaviors
     */
    public void tick(Level level, List<AgentEntity> agents) {
        if (agents.isEmpty()) return;
        
        long currentTime = level.getGameTime();
        
        // Update agent profiles
        updateAgentProfiles(agents, currentTime);
        
        // Emergent behavior analysis
        if (currentTime - lastEmergentAnalysis >= EMERGENT_ANALYSIS_INTERVAL) {
            performEmergentAnalysis(agents, currentTime);
            lastEmergentAnalysis = currentTime;
        }
        
        // Community events
        if (currentTime - lastCommunityEvent >= COMMUNITY_EVENT_INTERVAL) {
            considerCommunityEvents(agents, currentTime);
            lastCommunityEvent = currentTime;
        }
        
        // Evolution checking
        if (currentTime - lastEvolutionCheck >= EVOLUTION_CHECK_INTERVAL) {
            checkCommunityEvolution(agents, currentTime);
            lastEvolutionCheck = currentTime;
        }
        
        // Challenge generation
        if (currentTime - lastChallengeGeneration >= CHALLENGE_GENERATION_INTERVAL) {
            considerGeneratingChallenge(agents, currentTime);
            lastChallengeGeneration = currentTime;
        }
        
        // Individual agent advanced behaviors
        for (AgentEntity agent : agents) {
            processAdvancedAgentBehavior(agent, agents, currentTime);
        }
    }
    
    private void updateAgentProfiles(List<AgentEntity> agents, long currentTime) {
        for (AgentEntity agent : agents) {
            AgentProfile profile = agentProfiles.computeIfAbsent(
                agent.getUUID(), 
                id -> new AgentProfile(id, agent.getBaseName())
            );
            
            // Update based on current activities
            updateProfileFromActivities(agent, profile, currentTime);
            
            // Update preferred collaborators based on successful projects
            updateCollaboratorPreferences(agent, profile, agents);
        }
    }
    
    private void updateProfileFromActivities(AgentEntity agent, AgentProfile profile, long currentTime) {
        // Check current state and activities
        AgentEntity.AgentState currentState = agent.getCurrentState();
        
        switch (currentState) {
            case LEAD_PROJECT:
                profile.gainExperience("leadership", 2);
                break;
            case PARTICIPATE_IN_PROJECT:
                profile.gainExperience("collaboration", 1);
                break;
            case TRADING:
                profile.gainExperience("trading", 1);
                break;
            case GATHERING:
            case MINING:
            case FARMING:
                profile.gainExperience("resource_management", 1);
                break;
            case BUILDING:
                profile.gainExperience("construction", 1);
                break;
        }
        
        // Check for problem-solving (completing difficult projects)
        List<CoordinationSystem.GroupProject> projects = 
            agent.getCoordinationSystem().getActiveProjects();
        
        for (CoordinationSystem.GroupProject project : projects) {
            if (project.participants.contains(agent.getUUID()) && 
                project.status == CoordinationSystem.ProjectStatus.COMPLETED &&
                project.priority == CoordinationSystem.TaskPriority.CRITICAL) {
                
                profile.gainExperience("problem_solving", 5);
                profile.participatedProjects.add(project.title);
            }
        }
        
        // Check for innovation (new project types, creative solutions)
        if (hasShowedInnovation(agent, profile)) {
            profile.gainExperience("innovation", 3);
            profile.innovationHistory.add("Innovation at " + currentTime);
        }
    }
    
    private void updateCollaboratorPreferences(AgentEntity agent, AgentProfile profile, List<AgentEntity> allAgents) {
        // Find agents this agent has successfully collaborated with
        List<CoordinationSystem.GroupProject> completedProjects = 
            agent.getCoordinationSystem().getActiveProjects().stream()
                .filter(project -> project.status == CoordinationSystem.ProjectStatus.COMPLETED)
                .filter(project -> project.participants.contains(agent.getUUID()))
                .toList();
        
        for (CoordinationSystem.GroupProject project : completedProjects) {
            for (UUID collaborator : project.participants) {
                if (!collaborator.equals(agent.getUUID())) {
                    profile.preferredCollaborators.add(collaborator);
                }
            }
        }
        
        // Limit to top 5 collaborators
        if (profile.preferredCollaborators.size() > 5) {
            // Keep the most recent ones (simplified)
            Set<UUID> topCollaborators = new HashSet<>();
            profile.preferredCollaborators.stream().limit(5).forEach(topCollaborators::add);
            profile.preferredCollaborators.clear();
            profile.preferredCollaborators.addAll(topCollaborators);
        }
    }
    
    private boolean hasShowedInnovation(AgentEntity agent, AgentProfile profile) {
        // Check if agent initiated unique project types
        List<CoordinationSystem.GroupProject> initiatedProjects = 
            agent.getCoordinationSystem().getActiveProjects().stream()
                .filter(project -> project.initiator.equals(agent.getUUID()))
                .toList();
        
        Set<CoordinationSystem.ProjectType> projectTypes = new HashSet<>();
        for (CoordinationSystem.GroupProject project : initiatedProjects) {
            projectTypes.add(project.type);
        }
        
        return projectTypes.size() >= 3; // Initiated 3+ different project types
    }
    
    private void performEmergentAnalysis(List<AgentEntity> agents, long currentTime) {
        MASONRY.LOGGER.debug("Performing emergent behavior analysis for {} agents", agents.size());
        
        // Run the emergent behavior system analysis
        emergentSystem.analyzeEmergentBehaviors(agents, currentTime);
        
        // Record community health in evolution tracker
        EmergentBehaviorSystem.CommunityMetrics metrics = emergentSystem.getCommunityMetrics();
        double healthScore = metrics.getOverallHealthScore();
        
        evolutionTracker.recordEvolutionEvent(
            "HEALTH_MEASUREMENT",
            "Community health measured at " + String.format("%.2f", healthScore),
            currentTime,
            healthScore - 0.5 // Impact relative to neutral (0.5)
        );
        
        // Add evolutionary pressures based on community state
        addEvolutionaryPressures(metrics, agents.size());
    }
    
    private void addEvolutionaryPressures(EmergentBehaviorSystem.CommunityMetrics metrics, int communitySize) {
        // Size pressure
        if (communitySize < 5) {
            evolutionTracker.addEvolutionaryPressure("SMALL_COMMUNITY", 0.1);
        } else if (communitySize > 15) {
            evolutionTracker.addEvolutionaryPressure("LARGE_COMMUNITY", 0.1);
        }
        
        // Cooperation pressure
        if (metrics.cooperation < 0.4) {
            evolutionTracker.addEvolutionaryPressure("LOW_COOPERATION", 0.2);
        }
        
        // Innovation pressure
        if (metrics.innovation < 0.3) {
            evolutionTracker.addEvolutionaryPressure("STAGNATION", 0.15);
        }
        
        // Leadership pressure
        if (metrics.leadership < 0.3) {
            evolutionTracker.addEvolutionaryPressure("LEADERSHIP_VACUUM", 0.25);
        }
        
        // Stability pressure
        if (metrics.stability > 0.9) {
            evolutionTracker.addEvolutionaryPressure("OVER_STABILITY", 0.1);
        } else if (metrics.stability < 0.3) {
            evolutionTracker.addEvolutionaryPressure("INSTABILITY", 0.2);
        }
    }
    
    private void considerCommunityEvents(List<AgentEntity> agents, long currentTime) {
        EmergentBehaviorSystem.CommunityMetrics metrics = emergentSystem.getCommunityMetrics();
        Random random = new Random();
        
        // Determine if an event should occur
        double eventChance = calculateEventProbability(metrics, agents.size());
        
        if (random.nextDouble() < eventChance) {
            String eventType = selectEventType(metrics, random);
            
            MASONRY.LOGGER.info("Triggering community event: {}", eventType);
            emergentSystem.triggerCommunityEvent(eventType, agents, currentTime);
        }
    }
    
    private double calculateEventProbability(EmergentBehaviorSystem.CommunityMetrics metrics, int communitySize) {
        double baseChance = 0.3; // 30% base chance
        
        // More likely with higher cohesion
        baseChance += metrics.cohesion * 0.2;
        
        // More likely with larger communities
        baseChance += Math.min(0.3, communitySize / 20.0);
        
        // Less likely if recent events
        // (This would need event history tracking)
        
        return Math.min(0.8, baseChance); // Cap at 80%
    }
    
    private String selectEventType(EmergentBehaviorSystem.CommunityMetrics metrics, Random random) {
        List<String> candidateEvents = new ArrayList<>();
        
        // Add events based on community state
        if (metrics.cooperation > 0.6) {
            candidateEvents.add("celebration");
        }
        
        if (metrics.innovation > 0.5) {
            candidateEvents.add("innovation_festival");
        }
        
        if (metrics.stability > 0.7) {
            candidateEvents.add("tradition_ceremony");
        }
        
        if (metrics.leadership > 0.5 || metrics.cooperation > 0.5) {
            candidateEvents.add("community_meeting");
        }
        
        // Always possible events
        candidateEvents.add("celebration");
        candidateEvents.add("community_meeting");
        
        return candidateEvents.get(random.nextInt(candidateEvents.size()));
    }
    
    private void checkCommunityEvolution(List<AgentEntity> agents, long currentTime) {
        EmergentBehaviorSystem.CommunityMetrics metrics = emergentSystem.getCommunityMetrics();
        
        // Check for significant evolutionary changes
        double totalPressure = evolutionTracker.getTotalEvolutionaryPressure();
        
        if (totalPressure > 1.0) { // Significant pressure threshold
            triggerEvolutionaryResponse(agents, metrics, totalPressure, currentTime);
        }
        
        // Decay evolutionary pressures over time
        evolutionTracker.evolutionaryPressures.replaceAll((k, v) -> Math.max(0.0, v * 0.9));
        
        // Remove negligible pressures
        evolutionTracker.evolutionaryPressures.entrySet().removeIf(entry -> entry.getValue() < 0.01);
    }
    
    private void triggerEvolutionaryResponse(List<AgentEntity> agents, 
                                           EmergentBehaviorSystem.CommunityMetrics metrics,
                                           double pressure, long currentTime) {
        
        String dominantPressure = evolutionTracker.evolutionaryPressures.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse("UNKNOWN");
        
        MASONRY.LOGGER.info("Community evolution triggered by pressure: {} (intensity: {})", 
            dominantPressure, String.format("%.2f", pressure));
        
        switch (dominantPressure) {
            case "LOW_COOPERATION":
                encourageCooperation(agents, currentTime);
                break;
            case "LEADERSHIP_VACUUM":
                promoteLeadershipDevelopment(agents, currentTime);
                break;
            case "STAGNATION":
                stimulateInnovation(agents, currentTime);
                break;
            case "SMALL_COMMUNITY":
                encourageGrowth(agents, currentTime);
                break;
            case "LARGE_COMMUNITY":
                organizeSubCommunities(agents, currentTime);
                break;
            case "INSTABILITY":
                stabilizeCommunity(agents, currentTime);
                break;
        }
        
        evolutionTracker.recordEvolutionEvent(
            "EVOLUTIONARY_RESPONSE",
            "Community evolved in response to " + dominantPressure,
            currentTime,
            pressure
        );
    }
    
    private void encourageCooperation(List<AgentEntity> agents, long currentTime) {
        // Boost cooperation tendencies for all agents
        for (AgentEntity agent : agents) {
            AgentProfile profile = agentProfiles.get(agent.getUUID());
            if (profile != null) {
                double currentCoop = profile.getBehaviorTendency("cooperation");
                profile.behaviorTendencies.put("cooperation", Math.min(1.0, currentCoop + 0.1));
            }
        }
        
        // Trigger community cooperation event
        emergentSystem.triggerCommunityEvent("community_meeting", agents, currentTime);
    }
    
    private void promoteLeadershipDevelopment(List<AgentEntity> agents, long currentTime) {
        // Find potential leaders and boost their leadership tendencies
        agents.stream()
            .filter(agent -> agent.getGossipSystem().getReputation(agent.getUUID()) > 40)
            .limit(3)
            .forEach(agent -> {
                AgentProfile profile = agentProfiles.get(agent.getUUID());
                if (profile != null) {
                    double currentLeadership = profile.getBehaviorTendency("leadership");
                    profile.behaviorTendencies.put("leadership", Math.min(1.0, currentLeadership + 0.15));
                    profile.gainExperience("leadership", 10);
                }
            });
    }
    
    private void stimulateInnovation(List<AgentEntity> agents, long currentTime) {
        // Boost innovation tendencies
        for (AgentEntity agent : agents) {
            AgentProfile profile = agentProfiles.get(agent.getUUID());
            if (profile != null) {
                double currentInnovation = profile.getBehaviorTendency("innovation");
                profile.behaviorTendencies.put("innovation", Math.min(1.0, currentInnovation + 0.1));
            }
        }
        
        // Trigger innovation event
        emergentSystem.triggerCommunityEvent("innovation_festival", agents, currentTime);
    }
    
    private void encourageGrowth(List<AgentEntity> agents, long currentTime) {
        // Boost exploration and trading to attract new members
        for (AgentEntity agent : agents) {
            AgentProfile profile = agentProfiles.get(agent.getUUID());
            if (profile != null) {
                double currentExploration = profile.getBehaviorTendency("exploration");
                double currentTrading = profile.getBehaviorTendency("trading");
                
                profile.behaviorTendencies.put("exploration", Math.min(1.0, currentExploration + 0.1));
                profile.behaviorTendencies.put("trading", Math.min(1.0, currentTrading + 0.1));
            }
        }
    }
    
    private void organizeSubCommunities(List<AgentEntity> agents, long currentTime) {
        // Create specialized groups within the large community
        List<EmergentBehaviorSystem.CommunityPattern> patterns = emergentSystem.getActivePatterns();
        
        for (EmergentBehaviorSystem.CommunityPattern pattern : patterns) {
            if (pattern.type.equals("SPATIAL_CLUSTERING") && pattern.involvedAgents.size() >= 4) {
                // This cluster becomes a semi-autonomous sub-community
                pattern.patternData.put("sub_community", true);
                pattern.patternData.put("autonomy_level", 0.7);
            }
        }
    }
    
    private void stabilizeCommunity(List<AgentEntity> agents, long currentTime) {
        // Boost stability-promoting behaviors
        for (AgentEntity agent : agents) {
            AgentProfile profile = agentProfiles.get(agent.getUUID());
            if (profile != null) {
                profile.communityCommitment = Math.min(1.0, profile.communityCommitment + 0.1);
                
                double currentBuilding = profile.getBehaviorTendency("building");
                profile.behaviorTendencies.put("building", Math.min(1.0, currentBuilding + 0.1));
            }
        }
        
        // Trigger stabilizing event
        emergentSystem.triggerCommunityEvent("tradition_ceremony", agents, currentTime);
    }
    
    private void considerGeneratingChallenge(List<AgentEntity> agents, long currentTime) {
        EmergentBehaviorSystem.CommunityMetrics metrics = emergentSystem.getCommunityMetrics();
        
        // Generate challenges based on community state and evolution pressure
        double challengeProbability = calculateChallengeProbability(metrics, agents.size());
        
        if (new Random().nextDouble() < challengeProbability) {
            String challengeType = selectChallengeType(metrics);
            generateChallenge(challengeType, agents, currentTime);
        }
    }
    
    private double calculateChallengeProbability(EmergentBehaviorSystem.CommunityMetrics metrics, int communitySize) {
        double baseChance = 0.2; // 20% base chance
        
        // Higher chance if community is too stable (needs growth)
        if (metrics.stability > 0.8 && metrics.innovation < 0.4) {
            baseChance += 0.3;
        }
        
        // Higher chance if community is very healthy (can handle challenges)
        if (metrics.getOverallHealthScore() > 0.7) {
            baseChance += 0.2;
        }
        
        // Lower chance if community is struggling
        if (metrics.getOverallHealthScore() < 0.4) {
            baseChance -= 0.1;
        }
        
        return Math.max(0.05, Math.min(0.5, baseChance)); // 5-50% range
    }
    
    private String selectChallengeType(EmergentBehaviorSystem.CommunityMetrics metrics) {
        Random random = new Random();
        
        // Weighted selection based on what the community can handle
        if (metrics.resilience > 0.6 && metrics.cooperation > 0.5) {
            return random.nextBoolean() ? "external_threat" : "resource_scarcity";
        }
        
        if (metrics.leadership < 0.4) {
            return "leadership_crisis";
        }
        
        if (metrics.cohesion < 0.4) {
            return "social_conflict";
        }
        
        // Default challenges
        String[] challenges = {"resource_scarcity", "infrastructure_failure", "external_threat"};
        return challenges[random.nextInt(challenges.length)];
    }
    
    private void generateChallenge(String challengeType, List<AgentEntity> agents, long currentTime) {
        if (agents.isEmpty()) return;
        
        BlockPos challengeLocation = findChallengLocation(agents, challengeType);
        
        MASONRY.LOGGER.info("Generating community challenge: {} at {}", challengeType, challengeLocation);
        
        emergentSystem.respondToCommunityChallenge(challengeType, agents, challengeLocation, currentTime);
        
        // Add evolutionary pressure from the challenge
        evolutionTracker.addEvolutionaryPressure("CHALLENGE_" + challengeType.toUpperCase(), 0.3);
        
        evolutionTracker.recordEvolutionEvent(
            "CHALLENGE_GENERATED",
            "Community faced challenge: " + challengeType,
            currentTime,
            0.5 // Moderate impact
        );
    }
    
    private BlockPos findChallengLocation(List<AgentEntity> agents, String challengeType) {
        // Find center of community
        double avgX = agents.stream().mapToDouble(AgentEntity::getX).average().orElse(0);
        double avgZ = agents.stream().mapToDouble(AgentEntity::getZ).average().orElse(0);
        double avgY = agents.stream().mapToDouble(AgentEntity::getY).average().orElse(64);
        
        BlockPos center = new BlockPos((int)avgX, (int)avgY, (int)avgZ);
        
        // Adjust location based on challenge type
        switch (challengeType) {
            case "external_threat":
                // On the edge of community
                return center.offset(50 + new Random().nextInt(20), 0, 50 + new Random().nextInt(20));
            case "infrastructure_failure":
                // At community center
                return center;
            case "resource_scarcity":
                // At a distance where they usually gather resources
                return center.offset(30 + new Random().nextInt(30), 0, 30 + new Random().nextInt(30));
            default:
                return center;
        }
    }
    
    private void processAdvancedAgentBehavior(AgentEntity agent, List<AgentEntity> allAgents, long currentTime) {
        AgentProfile profile = agentProfiles.get(agent.getUUID());
        if (profile == null) return;
        
        // Personality-driven behavior modifications
        modifyBehaviorBasedOnPersonality(agent, profile);
        
        // Social influence processing
        processSocialInfluence(agent, profile, allAgents);
        
        // Adaptive decision making
        makeAdaptiveDecisions(agent, profile, allAgents, currentTime);
    }
    
    private void modifyBehaviorBasedOnPersonality(AgentEntity agent, AgentProfile profile) {
        // Influence agent state transitions based on personality
        if (agent.getCurrentState() == AgentEntity.AgentState.IDLE) {
            
            // High leadership tendency agents are more likely to initiate projects
            if (profile.getBehaviorTendency("leadership") > 0.7 && new Random().nextInt(100) < 5) {
                // Try to initiate a project
                considerProjectInitiation(agent, profile);
            }
            
            // High cooperation tendency agents seek group activities
            if (profile.getBehaviorTendency("cooperation") > 0.6 && new Random().nextInt(100) < 8) {
                // Look for projects to join
                considerProjectParticipation(agent, profile);
            }
            
            // High innovation tendency agents try new things
            if (profile.getBehaviorTendency("innovation") > 0.6 && new Random().nextInt(100) < 3) {
                // Try something innovative
                considerInnovativeAction(agent, profile);
            }
        }
    }
    
    private void processSocialInfluence(AgentEntity agent, AgentProfile profile, List<AgentEntity> allAgents) {
        // High social influence agents affect others nearby
        if (profile.socialInfluence > 0.7) {
            List<AgentEntity> nearbyAgents = allAgents.stream()
                .filter(other -> other != agent)
                .filter(other -> agent.distanceTo(other) < 16.0)
                .toList();
            
            for (AgentEntity nearby : nearbyAgents) {
                AgentProfile nearbyProfile = agentProfiles.get(nearby.getUUID());
                if (nearbyProfile != null && new Random().nextInt(200) == 0) { // 0.5% chance per tick
                    
                    // Influence based on agent's strongest tendency
                    String strongestTendency = profile.behaviorTendencies.entrySet().stream()
                        .max(Map.Entry.comparingByValue())
                        .map(Map.Entry::getKey)
                        .orElse("cooperation");
                    
                    double currentValue = nearbyProfile.getBehaviorTendency(strongestTendency);
                    double influence = profile.socialInfluence * 0.02; // Small influence
                    
                    nearbyProfile.behaviorTendencies.put(strongestTendency, 
                        Math.min(1.0, currentValue + influence));
                }
            }
        }
    }
    
    private void makeAdaptiveDecisions(AgentEntity agent, AgentProfile profile, 
                                     List<AgentEntity> allAgents, long currentTime) {
        
        // High adaptability agents respond better to changing conditions
        if (profile.adaptability > 0.6) {
            
            // Check community state and adapt accordingly
            EmergentBehaviorSystem.CommunityMetrics metrics = emergentSystem.getCommunityMetrics();
            
            // If cooperation is low, increase cooperative behavior
            if (metrics.cooperation < 0.4 && profile.getBehaviorTendency("cooperation") < 0.8) {
                profile.behaviorTendencies.put("cooperation", 
                    Math.min(1.0, profile.getBehaviorTendency("cooperation") + 0.05));
            }
            
            // If leadership is low and agent has potential, step up
            if (metrics.leadership < 0.4 && profile.getBehaviorTendency("leadership") > 0.5) {
                profile.behaviorTendencies.put("leadership", 
                    Math.min(1.0, profile.getBehaviorTendency("leadership") + 0.1));
            }
            
            // If innovation is low, try to be more creative
            if (metrics.innovation < 0.3 && profile.problemSolvingAbility > 0.5) {
                profile.behaviorTendencies.put("innovation", 
                    Math.min(1.0, profile.getBehaviorTendency("innovation") + 0.08));
            }
        }
    }
    
    private void considerProjectInitiation(AgentEntity agent, AgentProfile profile) {
        // Determine project type based on profile
        CoordinationSystem.ProjectType projectType = selectProjectTypeForAgent(profile);
        
        // Find suitable location
        BlockPos location = agent.blockPosition().offset(
            new Random().nextInt(21) - 10, 0, new Random().nextInt(21) - 10);
        
        // Initiate project
        agent.getCoordinationSystem().initiateProject(agent, projectType, location, null);
        
        profile.gainExperience("leadership", 3);
    }
    
    private void considerProjectParticipation(AgentEntity agent, AgentProfile profile) {
        // Look for suitable projects to join
        List<CoordinationSystem.GroupProject> recommendations = 
            agent.getCoordinationSystem().getRecommendedProjects(agent);
        
        if (!recommendations.isEmpty()) {
            // Join the most appealing project
            CoordinationSystem.GroupProject bestProject = recommendations.get(0);
            
            boolean joined = agent.getCoordinationSystem().joinProject(agent, bestProject.id);
            if (joined) {
                profile.gainExperience("collaboration", 2);
            }
        }
    }
    
    private void considerInnovativeAction(AgentEntity agent, AgentProfile profile) {
        // Try to do something innovative
        Random random = new Random();
        
        switch (random.nextInt(3)) {
            case 0:
                // Try a new profession if possible
                if (!agent.getProfessionSystem().hasProfession()) {
                    // This would trigger profession selection with innovation bias
                    profile.gainExperience("innovation", 2);
                }
                break;
            case 1:
                // Initiate an unusual project type
                CoordinationSystem.ProjectType[] unusualTypes = {
                    CoordinationSystem.ProjectType.EXPLORATION_PARTY,
                    CoordinationSystem.ProjectType.CELEBRATION,
                    CoordinationSystem.ProjectType.INFRASTRUCTURE
                };
                CoordinationSystem.ProjectType innovativeType = 
                    unusualTypes[random.nextInt(unusualTypes.length)];
                
                BlockPos location = agent.blockPosition().offset(
                    random.nextInt(31) - 15, 0, random.nextInt(31) - 15);
                
                agent.getCoordinationSystem().initiateProject(agent, innovativeType, location, 
                    "Innovative " + innovativeType.name().toLowerCase().replace("_", " "));
                
                profile.gainExperience("innovation", 4);
                break;
            case 2:
                // Try innovative trading
                if (profile.getBehaviorTendency("trading") > 0.4) {
                    // This would trigger creative trading behavior
                    profile.gainExperience("innovation", 1);
                }
                break;
        }
    }
    
    private CoordinationSystem.ProjectType selectProjectTypeForAgent(AgentProfile profile) {
        // Select project type based on agent's strongest tendencies
        String strongestTendency = profile.behaviorTendencies.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse("building");
        
        switch (strongestTendency) {
            case "building":
                return CoordinationSystem.ProjectType.COMMUNITY_BUILD;
            case "exploration":
                return CoordinationSystem.ProjectType.EXPLORATION_PARTY;
            case "trading":
                return CoordinationSystem.ProjectType.TRADE_CARAVAN;
            case "innovation":
                return CoordinationSystem.ProjectType.INFRASTRUCTURE;
            default:
                return CoordinationSystem.ProjectType.COMMUNITY_BUILD;
        }
    }
    
    // === PUBLIC INTERFACE ===
    
    public AgentProfile getAgentProfile(UUID agentId) {
        return agentProfiles.get(agentId);
    }
    
    public EmergentBehaviorSystem getEmergentSystem() {
        return emergentSystem;
    }
    
    public CommunityEvolutionTracker getEvolutionTracker() {
        return evolutionTracker;
    }
    
    public String getSystemStatus() {
        StringBuilder status = new StringBuilder();
        status.append("=== Advanced Agent System Status ===\n");
        status.append(String.format("Tracked Agents: %d\n", agentProfiles.size()));
        status.append(String.format("Evolution Events: %d\n", evolutionTracker.evolutionHistory.size()));
        status.append(String.format("Evolutionary Pressures: %d\n", evolutionTracker.evolutionaryPressures.size()));
        status.append(String.format("Total Evolution Pressure: %.2f\n", evolutionTracker.getTotalEvolutionaryPressure()));
        
        status.append("\n");
        status.append(emergentSystem.getCommunityStatusSummary());
        
        return status.toString();
    }
    
    public void forceEvolutionEvent(String eventType, List<AgentEntity> agents) {
        long currentTime = agents.isEmpty() ? 0 : agents.get(0).level().getGameTime();
        
        switch (eventType.toLowerCase()) {
            case "cooperation_boost":
                encourageCooperation(agents, currentTime);
                break;
            case "leadership_development":
                promoteLeadershipDevelopment(agents, currentTime);
                break;
            case "innovation_stimulus":
                stimulateInnovation(agents, currentTime);
                break;
            case "community_challenge":
                generateChallenge("external_threat", agents, currentTime);
                break;
        }
    }
}