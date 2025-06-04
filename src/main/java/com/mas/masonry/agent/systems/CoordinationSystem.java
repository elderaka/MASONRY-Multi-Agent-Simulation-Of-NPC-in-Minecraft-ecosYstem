package com.mas.masonry.agent.systems;

import com.mas.masonry.AgentEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages group coordination, leadership, and collaborative projects
 */
public class CoordinationSystem {
    private final Map<String, GroupProject> activeProjects = new ConcurrentHashMap<>();
    private final Map<UUID, LeadershipProfile> leadershipProfiles = new HashMap<>();
    private final List<CoordinationEvent> recentEvents = new ArrayList<>();
    private final Map<String, CollaborativeTask> activeTasks = new HashMap<>();

    // Constants
    private static final int MAX_ACTIVE_PROJECTS = 3;
    private static final int PROJECT_TIMEOUT_TICKS = 12000; // 10 minutes
    private static final int LEADERSHIP_COOLDOWN = 6000; // 5 minutes between leadership attempts
    private static final int MIN_GROUP_SIZE = 2;
    private static final int MAX_GROUP_SIZE = 6;

    public enum ProjectType {
        COMMUNITY_BUILD,     // Building shared structures
        RESOURCE_EXPEDITION, // Group mining/gathering expeditions
        DEFENSE_PATROL,      // Coordinated area defense
        TRADE_CARAVAN,       // Group trading missions
        EXPLORATION_PARTY,   // Coordinated exploration
        EMERGENCY_RESPONSE,  // Disaster response coordination
        INFRASTRUCTURE,      // Roads, bridges, utilities
        CELEBRATION         // Community events
    }

    public enum TaskPriority {
        CRITICAL(1.0),      // Immediate response needed
        HIGH(0.8),          // Important, should be done soon
        MEDIUM(0.6),        // Normal priority
        LOW(0.4),           // Can wait
        BACKGROUND(0.2);    // Whenever convenient

        public final double multiplier;
        TaskPriority(double multiplier) { this.multiplier = multiplier; }
    }

    public static class GroupProject {
        public final String id;
        public final ProjectType type;
        public final String title;
        public final String description;
        public final UUID initiator;
        public final BlockPos targetLocation;
        public final long creationTime;
        public TaskPriority priority;

        public final Set<UUID> participants;
        public final Set<UUID> invitedAgents;
        public final Map<String, Object> projectData;
        public final List<String> completedTasks = new ArrayList<>();
        public final List<String> remainingTasks = new ArrayList<>();

        public UUID projectLeader;
        public ProjectStatus status;
        public double completionPercentage = 0.0;
        public long lastActivityTime;

        public GroupProject(String id, ProjectType type, String title, String description,
                            UUID initiator, BlockPos location, TaskPriority priority, long creationTime) {
            this.id = id;
            this.type = type;
            this.title = title;
            this.description = description;
            this.initiator = initiator;
            this.targetLocation = location;
            this.priority = priority;
            this.creationTime = creationTime;
            this.lastActivityTime = creationTime;
            this.projectLeader = initiator;
            this.status = ProjectStatus.RECRUITING;

            // Initialize the collections BEFORE using them
            this.participants = new HashSet<>();
            this.invitedAgents = new HashSet<>();
            this.projectData = new HashMap<>();

            // Now we can safely add to participants
            this.participants.add(initiator);
        }

        public boolean isActive() {
            return status == ProjectStatus.ACTIVE || status == ProjectStatus.RECRUITING;
        }

        public boolean isTimeout(long currentTime) {
            return (currentTime - lastActivityTime) > PROJECT_TIMEOUT_TICKS;
        }

        public void updateActivity(long currentTime) {
            this.lastActivityTime = currentTime;
        }

        public boolean needsMoreParticipants() {
            return participants.size() < getRequiredParticipants();
        }

        public int getRequiredParticipants() {
            switch (type) {
                case EMERGENCY_RESPONSE: return 3;
                case COMMUNITY_BUILD: return 4;
                case RESOURCE_EXPEDITION: return 3;
                case DEFENSE_PATROL: return 2;
                case TRADE_CARAVAN: return 2;
                case EXPLORATION_PARTY: return 3;
                case INFRASTRUCTURE: return 4;
                case CELEBRATION: return 5;
                default: return 2;
            }
        }
    }

    public enum ProjectStatus {
        RECRUITING,  // Looking for participants
        ACTIVE,      // In progress
        COMPLETED,   // Successfully finished
        CANCELLED,   // Abandoned
        ON_HOLD      // Temporarily paused
    }

    public static class LeadershipProfile {
        public final UUID agentId;
        public int leadershipScore; // 0-100
        public int successfulProjects;
        public int failedProjects;
        public long lastLeadershipAttempt;
        public double charisma; // Natural leadership ability
        public String leadershipStyle; // Authoritative, Democratic, Supportive

        // Leadership traits
        public boolean isDecisive;
        public boolean isOrganized;
        public boolean isInspiring;
        public boolean isDiplomatic;

        public LeadershipProfile(UUID agentId) {
            this.agentId = agentId;
            this.leadershipScore = 30 + new Random().nextInt(40); // 30-70
            this.successfulProjects = 0;
            this.failedProjects = 0;
            this.lastLeadershipAttempt = 0;
            this.charisma = 0.3 + new Random().nextDouble() * 0.4; // 0.3-0.7

            // Assign random leadership style
            String[] styles = {"Authoritative", "Democratic", "Supportive"};
            this.leadershipStyle = styles[new Random().nextInt(styles.length)];

            // Random traits
            Random rand = new Random();
            this.isDecisive = rand.nextBoolean();
            this.isOrganized = rand.nextBoolean();
            this.isInspiring = rand.nextBoolean();
            this.isDiplomatic = rand.nextBoolean();
        }

        public boolean canLead(long currentTime) {
            return (currentTime - lastLeadershipAttempt) > LEADERSHIP_COOLDOWN;
        }

        public double getLeadershipEffectiveness() {
            double base = leadershipScore / 100.0;
            double experience = Math.min(1.0, successfulProjects / 5.0);
            double failurePenalty = Math.max(0.0, 1.0 - (failedProjects * 0.1));
            return (base * 0.5 + charisma * 0.3 + experience * 0.2) * failurePenalty;
        }
    }

    public static class CoordinationEvent {
        public final String type;
        public final String description;
        public final UUID primaryAgent;
        public final Set<UUID> involvedAgents;
        public final long timestamp;
        public final BlockPos location;

        public CoordinationEvent(String type, String description, UUID primary,
                                 Set<UUID> involved, long timestamp, BlockPos location) {
            this.type = type;
            this.description = description;
            this.primaryAgent = primary;
            this.involvedAgents = new HashSet<>(involved);
            this.timestamp = timestamp;
            this.location = location;
        }
    }

    public static class CollaborativeTask {
        public final String id;
        public final String description;
        public final TaskPriority priority;
        public final UUID assignedAgent;
        public final String projectId;
        public final long creationTime;
        public final Map<String, Object> taskData;

        public boolean isCompleted = false;
        public long completionTime = 0;

        public CollaborativeTask(String id, String description, TaskPriority priority,
                                 UUID assignedAgent, String projectId, long creationTime) {
            this.id = id;
            this.description = description;
            this.priority = priority;
            this.assignedAgent = assignedAgent;
            this.projectId = projectId;
            this.creationTime = creationTime;
            this.taskData = new HashMap<>();
        }
    }

    /**
     * Initiate a new group project
     */
    public GroupProject initiateProject(AgentEntity initiator, ProjectType type,
                                        BlockPos location, String customDescription) {

        if (activeProjects.size() >= MAX_ACTIVE_PROJECTS) {
            return null; // Too many active projects
        }

        String projectId = type.name() + "_" + System.currentTimeMillis();
        String title = generateProjectTitle(type, initiator.getBaseName());
        String description = customDescription != null ? customDescription :
                generateProjectDescription(type, location);

        TaskPriority priority = determineProjectPriority(type, initiator);

        GroupProject project = new GroupProject(
                projectId, type, title, description, initiator.getUUID(),
                location, priority, initiator.level().getGameTime()
        );

        // Set up project-specific data and tasks
        initializeProjectTasks(project, type);

        activeProjects.put(projectId, project);

        // Create coordination event
        recordEvent("PROJECT_INITIATED",
                initiator.getBaseName() + " started: " + title,
                initiator.getUUID(), Set.of(initiator.getUUID()),
                initiator.level().getGameTime(), location);

        // Create gossip about the project
        initiator.getGossipSystem().createGeneralGossip(initiator,
                "Gue lagi inisiasi project: " + title + "! Siapa mau join?");

        return project;
    }

    /**
     * Agent attempts to join a project
     */
    public boolean joinProject(AgentEntity agent, String projectId) {
        GroupProject project = activeProjects.get(projectId);

        if (project == null || !project.isActive() ||
                project.participants.size() >= MAX_GROUP_SIZE) {
            return false;
        }

        // Check if agent is suitable for this project
        if (!isAgentSuitableForProject(agent, project)) {
            return false;
        }

        project.participants.add(agent.getUUID());
        project.invitedAgents.remove(agent.getUUID());
        project.updateActivity(agent.level().getGameTime());

        // If we have enough participants, start the project
        if (project.participants.size() >= project.getRequiredParticipants() &&
                project.status == ProjectStatus.RECRUITING) {
            project.status = ProjectStatus.ACTIVE;

            // Assign roles and tasks
            assignProjectRoles(project);
        }

        recordEvent("AGENT_JOINED_PROJECT",
                agent.getBaseName() + " joined " + project.title,
                agent.getUUID(), project.participants,
                agent.level().getGameTime(), project.targetLocation);

        return true;
    }

    /**
     * Intelligent project recommendations
     */
    public List<GroupProject> getRecommendedProjects(AgentEntity agent) {
        return activeProjects.values().stream()
                .filter(project -> project.status == ProjectStatus.RECRUITING)
                .filter(project -> !project.participants.contains(agent.getUUID()))
                .filter(project -> isAgentSuitableForProject(agent, project))
                .sorted(Comparator.comparingDouble(project ->
                        calculateProjectRelevance(agent, project)))
                .limit(3)
                .toList();
    }

    /**
     * Emergency response coordination
     */
    public GroupProject initiateEmergencyResponse(AgentEntity initiator, String emergencyType,
                                                  BlockPos location, String details) {

        GroupProject emergency = initiateProject(initiator, ProjectType.EMERGENCY_RESPONSE,
                location, "Emergency: " + emergencyType + " - " + details);

        if (emergency != null) {
            emergency.priority = TaskPriority.CRITICAL;

            // Automatically invite nearby agents
            List<AgentEntity> nearbyAgents = findNearbyAgents(initiator, 32.0);
            for (AgentEntity agent : nearbyAgents) {
                if (agent != initiator) {
                    emergency.invitedAgents.add(agent.getUUID());

                    // Send urgent gossip
                    initiator.getGossipSystem().createDangerGossip(initiator, emergencyType, location);
                }
            }

            // Broadcast emergency message
            recordEvent("EMERGENCY_DECLARED",
                    "EMERGENCY: " + emergencyType + " at " + location + " - " + details,
                    initiator.getUUID(), emergency.invitedAgents,
                    initiator.level().getGameTime(), location);
        }

        return emergency;
    }

    /**
     * Community building coordination
     */
    public GroupProject initiateCommunityBuild(AgentEntity initiator, String buildingType,
                                               BlockPos location, List<Item> requiredMaterials) {

        GroupProject buildProject = initiateProject(initiator, ProjectType.COMMUNITY_BUILD,
                location, "Community " + buildingType);

        if (buildProject != null) {
            buildProject.projectData.put("building_type", buildingType);
            buildProject.projectData.put("required_materials", requiredMaterials);
            buildProject.projectData.put("material_contributions", new HashMap<UUID, Map<Item, Integer>>());

            // Create specific tasks for building
            createBuildingTasks(buildProject, requiredMaterials);
        }

        return buildProject;
    }

    /**
     * Resource expedition coordination
     */
    public GroupProject initiateResourceExpedition(AgentEntity initiator, Item targetResource,
                                                   BlockPos expeditionSite) {

        GroupProject expedition = initiateProject(initiator, ProjectType.RESOURCE_EXPEDITION,
                expeditionSite, "Expedition for " +
                        targetResource.getDescription().getString());

        if (expedition != null) {
            expedition.projectData.put("target_resource", targetResource);
            expedition.projectData.put("expedition_site", expeditionSite);
            expedition.projectData.put("gathered_resources", new HashMap<UUID, Integer>());

            // Create expedition tasks
            createExpeditionTasks(expedition, targetResource);
        }

        return expedition;
    }

    /**
     * Leadership election/selection
     */
    public UUID electProjectLeader(GroupProject project, List<AgentEntity> candidates) {
        if (candidates.isEmpty()) {
            return project.projectLeader; // Keep current leader
        }

        // Score each candidate
        Map<UUID, Double> leadershipScores = new HashMap<>();

        for (AgentEntity candidate : candidates) {
            LeadershipProfile profile = getOrCreateLeadershipProfile(candidate.getUUID());

            double score = profile.getLeadershipEffectiveness();

            // Bonus for relevant experience
            if (hasRelevantExperience(candidate, project.type)) {
                score += 0.2;
            }

            // Bonus for reputation
            if (candidate.getGossipSystem().isWellRegarded(candidate.getUUID())) {
                score += 0.15;
            }

            // Bonus for current profession relevance
            if (isProfessionRelevant(candidate, project.type)) {
                score += 0.1;
            }

            leadershipScores.put(candidate.getUUID(), score);
        }

        // Select the best leader
        UUID newLeader = leadershipScores.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(project.projectLeader);

        if (!newLeader.equals(project.projectLeader)) {
            project.projectLeader = newLeader;

            recordEvent("LEADERSHIP_CHANGED",
                    "New project leader elected for " + project.title,
                    newLeader, project.participants,
                    System.currentTimeMillis(), project.targetLocation);
        }

        return newLeader;
    }

    /**
     * Dynamic task assignment based on agent capabilities
     */
    public void assignOptimalTasks(GroupProject project) {
        List<AgentEntity> availableAgents = getProjectParticipants(project);
        List<String> unassignedTasks = new ArrayList<>(project.remainingTasks);

        // Score each agent for each task
        Map<String, Map<UUID, Double>> taskAgentScores = new HashMap<>();

        for (String taskId : unassignedTasks) {
            taskAgentScores.put(taskId, new HashMap<>());

            for (AgentEntity agent : availableAgents) {
                double score = calculateTaskSuitability(agent, taskId, project);
                taskAgentScores.get(taskId).put(agent.getUUID(), score);
            }
        }

        // Assign tasks optimally (Hungarian algorithm simplified)
        while (!unassignedTasks.isEmpty() && !availableAgents.isEmpty()) {
            // Find the best task-agent pairing
            String bestTask = null;
            UUID bestAgent = null;
            double bestScore = -1.0;

            for (String taskId : unassignedTasks) {
                for (AgentEntity agent : availableAgents) {
                    double score = taskAgentScores.get(taskId).get(agent.getUUID());
                    if (score > bestScore) {
                        bestScore = score;
                        bestTask = taskId;
                        bestAgent = agent.getUUID();
                    }
                }
            }

            if (bestTask != null && bestAgent != null) {
                // Create and assign the task
                CollaborativeTask task = new CollaborativeTask(
                        bestTask,
                        generateTaskDescription(bestTask, project),
                        project.priority,
                        bestAgent,
                        project.id,
                        System.currentTimeMillis()
                );

                activeTasks.put(bestTask, task);
                unassignedTasks.remove(bestTask);
                UUID finalBestAgent = bestAgent;
                availableAgents.removeIf(agent -> agent.getUUID().equals(finalBestAgent));
            } else {
                break; // No more suitable assignments
            }
        }
    }

    /**
     * Adaptive coordination strategies
     */
    public void updateCoordinationStrategy(GroupProject project) {
        double efficiency = calculateProjectEfficiency(project);

        if (efficiency < 0.5) {
            // Low efficiency - need better coordination

            if (project.participants.size() > 4) {
                // Too many cooks - create sub-teams
                createSubTeams(project);
            }

            // Consider leadership change
            if (efficiency < 0.3) {
                List<AgentEntity> participants = getProjectParticipants(project);
                electProjectLeader(project, participants);
            }

            // Simplify tasks
            simplifyProjectTasks(project);
        }

        // Update project data with lessons learned
        project.projectData.put("efficiency_history",
                ((List<Double>) project.projectData.getOrDefault("efficiency_history", new ArrayList<>()))
                        .stream().limit(10).toList()); // Keep last 10 efficiency measurements
    }

    /**
     * Cross-project coordination
     */
    public void coordinateAcrossProjects(AgentEntity coordinator) {
        List<GroupProject> activeProjectsList = new ArrayList<>(activeProjects.values());

        // Look for resource conflicts
        Map<Item, List<GroupProject>> resourceDemands = new HashMap<>();

        for (GroupProject project : activeProjectsList) {
            List<Item> neededItems = getProjectResourceNeeds(project);
            for (Item item : neededItems) {
                resourceDemands.computeIfAbsent(item, k -> new ArrayList<>()).add(project);
            }
        }

        // Resolve conflicts by priority
        for (Map.Entry<Item, List<GroupProject>> entry : resourceDemands.entrySet()) {
            if (entry.getValue().size() > 1) {
                // Multiple projects need the same resource
                resolveResourceConflict(entry.getKey(), entry.getValue(), coordinator);
            }
        }

        // Look for collaboration opportunities
        identifyCollaborationOpportunities(activeProjectsList, coordinator);
    }

    /**
     * Celebration and community events
     */
    public GroupProject initiateCelebration(AgentEntity initiator, String occasion, BlockPos location) {
        GroupProject celebration = initiateProject(initiator, ProjectType.CELEBRATION,
                location, "Community celebration: " + occasion);

        if (celebration != null) {
            celebration.projectData.put("occasion", occasion);
            celebration.projectData.put("activities", generateCelebrationActivities());

            // Invite everyone in the area
            List<AgentEntity> allNearbyAgents = findNearbyAgents(initiator, 64.0);
            for (AgentEntity agent : allNearbyAgents) {
                if (agent != initiator) {
                    celebration.invitedAgents.add(agent.getUUID());
                }
            }

            // Create festive gossip
            initiator.getGossipSystem().createGeneralGossip(initiator,
                    "Ada celebration " + occasion + " nih! Everyone's invited!");
        }

        return celebration;
    }

    // === HELPER METHODS ===

    private String generateProjectTitle(ProjectType type, String initiatorName) {
        switch (type) {
            case COMMUNITY_BUILD: return initiatorName + "'s Community Building Project";
            case RESOURCE_EXPEDITION: return "Resource Gathering Expedition";
            case DEFENSE_PATROL: return "Area Defense Initiative";
            case TRADE_CARAVAN: return "Trading Expedition";
            case EXPLORATION_PARTY: return "Exploration Mission";
            case EMERGENCY_RESPONSE: return "Emergency Response Team";
            case INFRASTRUCTURE: return "Infrastructure Development";
            case CELEBRATION: return "Community Celebration";
            default: return "Collaborative Project";
        }
    }

    private String generateProjectDescription(ProjectType type, BlockPos location) {
        switch (type) {
            case COMMUNITY_BUILD:
                return "Mari kita bangun sesuatu yang berguna untuk komunitas di " + location + "!";
            case RESOURCE_EXPEDITION:
                return "Ekspedisi pencarian resource di area " + location + ". Safety in numbers!";
            case DEFENSE_PATROL:
                return "Patroli keamanan area " + location + " untuk melindungi komunitas.";
            case TRADE_CARAVAN:
                return "Misi trading bersama ke " + location + ". More profits, shared risks!";
            case EXPLORATION_PARTY:
                return "Eksplorasi area " + location + " untuk mencari opportunities baru.";
            case EMERGENCY_RESPONSE:
                return "Tim respons darurat untuk situasi di " + location + ".";
            case INFRASTRUCTURE:
                return "Pembangunan infrastruktur di " + location + " untuk kepentingan bersama.";
            case CELEBRATION:
                return "Perayaan komunitas di " + location + "! Mari kita celebrate together!";
            default:
                return "Collaborative project di area " + location + ".";
        }
    }

    private TaskPriority determineProjectPriority(ProjectType type, AgentEntity initiator) {
        switch (type) {
            case EMERGENCY_RESPONSE: return TaskPriority.CRITICAL;
            case DEFENSE_PATROL: return TaskPriority.HIGH;
            case COMMUNITY_BUILD: return TaskPriority.MEDIUM;
            case INFRASTRUCTURE: return TaskPriority.MEDIUM;
            case RESOURCE_EXPEDITION: return TaskPriority.MEDIUM;
            case TRADE_CARAVAN: return TaskPriority.LOW;
            case EXPLORATION_PARTY: return TaskPriority.LOW;
            case CELEBRATION: return TaskPriority.BACKGROUND;
            default: return TaskPriority.MEDIUM;
        }
    }

    private void initializeProjectTasks(GroupProject project, ProjectType type) {
        List<String> tasks = new ArrayList<>();

        switch (type) {
            case COMMUNITY_BUILD:
                tasks.addAll(Arrays.asList(
                        "gather_materials", "clear_area", "lay_foundation",
                        "build_structure", "add_details", "final_cleanup"
                ));
                break;
            case RESOURCE_EXPEDITION:
                tasks.addAll(Arrays.asList(
                        "prepare_equipment", "travel_to_site", "establish_camp",
                        "gather_resources", "return_journey", "distribute_resources"
                ));
                break;
            case DEFENSE_PATROL:
                tasks.addAll(Arrays.asList(
                        "scout_area", "identify_threats", "establish_perimeter",
                        "patrol_routes", "report_status"
                ));
                break;
            case EMERGENCY_RESPONSE:
                tasks.addAll(Arrays.asList(
                        "assess_situation", "establish_safety", "provide_aid",
                        "coordinate_rescue", "cleanup_aftermath"
                ));
                break;
            default:
                tasks.addAll(Arrays.asList(
                        "planning", "preparation", "execution", "completion"
                ));
        }

        project.remainingTasks.addAll(tasks);
    }

    private double calculateProjectRelevance(AgentEntity agent, GroupProject project) {
        double relevance = 1.0;

        // Distance factor
        BlockPos agentPos = agent.blockPosition();
        double distance = Math.sqrt(agentPos.distSqr(project.targetLocation));
        relevance *= Math.max(0.1, 1.0 - (distance / 64.0));

        // Priority factor
        relevance *= project.priority.multiplier;

        // Profession relevance
        if (isProfessionRelevant(agent, project.type)) {
            relevance *= 1.5;
        }

        // Reputation with project leader
        if (agent.getGossipSystem().isWellRegarded(project.projectLeader)) {
            relevance *= 1.2;
        }

        return relevance;
    }

    private boolean isAgentSuitableForProject(AgentEntity agent, GroupProject project) {
        // Check reputation
        if (agent.getGossipSystem().hasBadReputation(agent.getUUID())) {
            return false;
        }

        // Check if already in too many projects
        long participatingProjects = activeProjects.values().stream()
                .filter(p -> p.participants.contains(agent.getUUID()))
                .count();

        if (participatingProjects >= 2) {
            return false; // Already busy
        }

        // Project-specific requirements
        switch (project.type) {
            case EMERGENCY_RESPONSE:
                return agent.getHealth() > agent.getMaxHealth() * 0.7; // Need healthy agents
            case RESOURCE_EXPEDITION:
                return hasGatheringTools(agent);
            case COMMUNITY_BUILD:
                return hasBuildingMaterials(agent) || hasGatheringTools(agent);
            default:
                return true;
        }
    }

    private boolean hasGatheringTools(AgentEntity agent) {
        return agent.getInventory().hasAnyOf(Set.of(
                Items.WOODEN_PICKAXE, Items.STONE_PICKAXE, Items.IRON_PICKAXE,
                Items.WOODEN_AXE, Items.STONE_AXE, Items.IRON_AXE,
                Items.WOODEN_SHOVEL, Items.STONE_SHOVEL, Items.IRON_SHOVEL
        ));
    }

    private boolean hasBuildingMaterials(AgentEntity agent) {
        return agent.getInventory().hasAnyOf(Set.of(
                Items.COBBLESTONE, Items.OAK_PLANKS, Items.OAK_LOG,
                Items.STONE, Items.IRON_INGOT
        ));
    }

    private List<AgentEntity> findNearbyAgents(AgentEntity center, double radius) {
        return center.level().getEntitiesOfClass(AgentEntity.class,
                        center.getBoundingBox().inflate(radius))
                .stream()
                .filter(agent -> agent != center)
                .toList();
    }

    private void assignProjectRoles(GroupProject project) {
        List<AgentEntity> participants = getProjectParticipants(project);

        // Assign specialized roles based on project type
        switch (project.type) {
            case COMMUNITY_BUILD:
                assignBuildingRoles(project, participants);
                break;
            case RESOURCE_EXPEDITION:
                assignExpeditionRoles(project, participants);
                break;
            case DEFENSE_PATROL:
                assignDefenseRoles(project, participants);
                break;
            default:
                assignGenericRoles(project, participants);
        }
    }

    private void assignBuildingRoles(GroupProject project, List<AgentEntity> participants) {
        String[] roles = {"architect", "gatherer", "builder", "coordinator"};

        for (int i = 0; i < participants.size() && i < roles.length; i++) {
            project.projectData.put("role_" + participants.get(i).getUUID(), roles[i]);
        }
    }

    private void assignExpeditionRoles(GroupProject project, List<AgentEntity> participants) {
        String[] roles = {"scout", "gatherer", "guard", "carrier"};

        for (int i = 0; i < participants.size() && i < roles.length; i++) {
            project.projectData.put("role_" + participants.get(i).getUUID(), roles[i]);
        }
    }

    private void assignDefenseRoles(GroupProject project, List<AgentEntity> participants) {
        String[] roles = {"patrol_leader", "perimeter_guard", "scout", "backup"};

        for (int i = 0; i < participants.size() && i < roles.length; i++) {
            project.projectData.put("role_" + participants.get(i).getUUID(), roles[i]);
        }
    }

    private void assignGenericRoles(GroupProject project, List<AgentEntity> participants) {
        String[] roles = {"leader", "coordinator", "worker", "support"};

        for (int i = 0; i < participants.size() && i < roles.length; i++) {
            project.projectData.put("role_" + participants.get(i).getUUID(), roles[i]);
        }
    }

    private List<AgentEntity> getProjectParticipants(GroupProject project) {
        // This would need to be implemented to find actual agent entities
        // For now, return empty list as placeholder
        return new ArrayList<>();
    }

    private void recordEvent(String type, String description, UUID primary,
                             Set<UUID> involved, long timestamp, BlockPos location) {
        CoordinationEvent event = new CoordinationEvent(
                type, description, primary, involved, timestamp, location
        );

        recentEvents.add(event);

        // Keep only recent events (last 20)
        if (recentEvents.size() > 20) {
            recentEvents.remove(0);
        }
    }

    private LeadershipProfile getOrCreateLeadershipProfile(UUID agentId) {
        return leadershipProfiles.computeIfAbsent(agentId, LeadershipProfile::new);
    }

    private boolean hasRelevantExperience(AgentEntity agent, ProjectType type) {
        // Check if agent has completed similar projects before
        // This would be tracked in their profile/memory
        return false; // Placeholder
    }

    private boolean isProfessionRelevant(AgentEntity agent, ProjectType type) {
        if (!agent.getProfessionSystem().hasProfession()) {
            return false;
        }

        String profession = agent.getProfessionSystem().getCurrentProfession().getName();

        switch (type) {
            case COMMUNITY_BUILD:
            case INFRASTRUCTURE:
                return profession.equals("Builder") || profession.equals("Architect");
            case RESOURCE_EXPEDITION:
                return profession.equals("Miner") || profession.equals("Gatherer");
            case TRADE_CARAVAN:
                return profession.equals("Trader") || profession.equals("Explorer");
            default:
                return false;
        }
    }

    private double calculateTaskSuitability(AgentEntity agent, String taskId, GroupProject project) {
        double suitability = 0.5; // Base suitability

        // Factor in profession
        if (isProfessionRelevant(agent, project.type)) {
            suitability += 0.3;
        }

        // Factor in tools/resources
        if (hasRelevantTools(agent, taskId)) {
            suitability += 0.2;
        }

        // Factor in agent's role in project
        String role = (String) project.projectData.get("role_" + agent.getUUID());
        if (isTaskSuitableForRole(taskId, role)) {
            suitability += 0.3;
        }

        return Math.min(1.0, suitability);
    }

    private boolean hasRelevantTools(AgentEntity agent, String taskId) {
        switch (taskId) {
            case "gather_materials":
            case "gather_resources":
                return hasGatheringTools(agent);
            case "build_structure":
            case "lay_foundation":
                return hasBuildingMaterials(agent);
            default:
                return true;
        }
    }

    private boolean isTaskSuitableForRole(String taskId, String role) {
        if (role == null) return true;

        switch (role) {
            case "gatherer":
                return taskId.contains("gather");
            case "builder":
                return taskId.contains("build") || taskId.contains("construct");
            case "scout":
                return taskId.contains("scout") || taskId.contains("explore");
            case "guard":
                return taskId.contains("patrol") || taskId.contains("defend");
            default:
                return true;
        }
    }

    private String generateTaskDescription(String taskId, GroupProject project) {
        switch (taskId) {
            case "gather_materials": return "Gather construction materials for " + project.title;
            case "clear_area": return "Clear and prepare the construction area";
            case "lay_foundation": return "Build the foundation structure";
            case "build_structure": return "Construct the main building";
            case "scout_area": return "Scout the area for threats and opportunities";
            case "patrol_routes": return "Patrol assigned routes for security";
            default: return "Complete task: " + taskId + " for " + project.title;
        }
    }

    private double calculateProjectEfficiency(GroupProject project) {
        if (project.completedTasks.isEmpty()) {
            return 0.5; // Neutral when no tasks completed yet
        }

        double completionRate = (double) project.completedTasks.size() /
                (project.completedTasks.size() + project.remainingTasks.size());

        long timeSpent = System.currentTimeMillis() - project.creationTime;
        double expectedProgress = Math.min(1.0, timeSpent / (double) PROJECT_TIMEOUT_TICKS);

        return Math.min(1.0, completionRate / Math.max(0.1, expectedProgress));
    }

    private void createSubTeams(GroupProject project) {
        List<AgentEntity> participants = getProjectParticipants(project);

        if (participants.size() >= 4) {
            int subTeamSize = Math.max(2, participants.size() / 2);

            project.projectData.put("subteam_1",
                    participants.subList(0, subTeamSize).stream()
                            .map(AgentEntity::getUUID).toList());

            project.projectData.put("subteam_2",
                    participants.subList(subTeamSize, participants.size()).stream()
                            .map(AgentEntity::getUUID).toList());
        }
    }

    private void simplifyProjectTasks(GroupProject project) {
        // Combine similar tasks
        List<String> simplifiedTasks = new ArrayList<>();

        for (String task : project.remainingTasks) {
            if (task.contains("gather")) {
                if (!simplifiedTasks.contains("gather_all_materials")) {
                    simplifiedTasks.add("gather_all_materials");
                }
            } else if (task.contains("build")) {
                if (!simplifiedTasks.contains("complete_construction")) {
                    simplifiedTasks.add("complete_construction");
                }
            } else {
                simplifiedTasks.add(task);
            }
        }

        project.remainingTasks.clear();
        project.remainingTasks.addAll(simplifiedTasks);
    }

    private void createBuildingTasks(GroupProject project, List<Item> materials) {
        project.remainingTasks.clear();
        project.remainingTasks.addAll(Arrays.asList(
                "plan_construction",
                "gather_" + materials.get(0).getDescription().getString(),
                "prepare_foundation",
                "construct_walls",
                "add_roof",
                "finishing_touches"
        ));
    }

    private void createExpeditionTasks(GroupProject project, Item targetResource) {
        project.remainingTasks.clear();
        project.remainingTasks.addAll(Arrays.asList(
                "prepare_expedition",
                "travel_to_site",
                "establish_base_camp",
                "gather_" + targetResource.getDescription().getString(),
                "return_with_resources",
                "distribute_findings"
        ));
    }

    private List<Item> getProjectResourceNeeds(GroupProject project) {
        // Extract needed resources from project data
        List<Item> needs = new ArrayList<>();

        @SuppressWarnings("unchecked")
        List<Item> requiredMaterials = (List<Item>) project.projectData.get("required_materials");
        if (requiredMaterials != null) {
            needs.addAll(requiredMaterials);
        }

        Item targetResource = (Item) project.projectData.get("target_resource");
        if (targetResource != null) {
            needs.add(targetResource);
        }

        return needs;
    }

    private void resolveResourceConflict(Item resource, List<GroupProject> conflictingProjects,
                                         AgentEntity coordinator) {
        // Sort by priority
        conflictingProjects.sort(Comparator.comparing((GroupProject p) -> p.priority)
                .reversed());

        // Higher priority project gets preference
        GroupProject priorityProject = conflictingProjects.get(0);

        recordEvent("RESOURCE_CONFLICT_RESOLVED",
                "Resource conflict resolved: " + resource.getDescription().getString() +
                        " allocated to " + priorityProject.title,
                coordinator.getUUID(),
                priorityProject.participants,
                coordinator.level().getGameTime(),
                priorityProject.targetLocation);
    }

    private void identifyCollaborationOpportunities(List<GroupProject> projects, AgentEntity coordinator) {
        for (int i = 0; i < projects.size(); i++) {
            for (int j = i + 1; j < projects.size(); j++) {
                GroupProject project1 = projects.get(i);
                GroupProject project2 = projects.get(j);

                if (canCollaborate(project1, project2)) {
                    recordEvent("COLLABORATION_OPPORTUNITY",
                            "Potential collaboration between " + project1.title +
                                    " and " + project2.title,
                            coordinator.getUUID(),
                            Set.of(), // Empty for now
                            coordinator.level().getGameTime(),
                            project1.targetLocation);
                }
            }
        }
    }

    private boolean canCollaborate(GroupProject project1, GroupProject project2) {
        // Check if projects are in similar locations
        double distance = project1.targetLocation.distSqr(project2.targetLocation);
        if (distance > 1024) { // 32 blocks squared
            return false;
        }

        // Check for complementary project types
        return isComplementaryTypes(project1.type, project2.type);
    }

    private boolean isComplementaryTypes(ProjectType type1, ProjectType type2) {
        // Infrastructure + Community Build
        if ((type1 == ProjectType.INFRASTRUCTURE && type2 == ProjectType.COMMUNITY_BUILD) ||
                (type1 == ProjectType.COMMUNITY_BUILD && type2 == ProjectType.INFRASTRUCTURE)) {
            return true;
        }

        // Resource Expedition + Community Build (materials for building)
        if ((type1 == ProjectType.RESOURCE_EXPEDITION && type2 == ProjectType.COMMUNITY_BUILD) ||
                (type1 == ProjectType.COMMUNITY_BUILD && type2 == ProjectType.RESOURCE_EXPEDITION)) {
            return true;
        }

        return false;
    }

    private List<String> generateCelebrationActivities() {
        return Arrays.asList(
                "community_feast",
                "building_showcase",
                "skill_demonstrations",
                "story_sharing",
                "group_projects_review",
                "future_planning"
        );
    }

    // === PUBLIC INTERFACE METHODS ===

    public List<GroupProject> getActiveProjects() {
        return new ArrayList<>(activeProjects.values());
    }

    public GroupProject getProject(String projectId) {
        return activeProjects.get(projectId);
    }

    public List<CollaborativeTask> getAgentTasks(UUID agentId) {
        return activeTasks.values().stream()
                .filter(task -> task.assignedAgent.equals(agentId))
                .filter(task -> !task.isCompleted)
                .toList();
    }

    public boolean completeTask(String taskId, UUID completedBy) {
        CollaborativeTask task = activeTasks.get(taskId);
        if (task != null && task.assignedAgent.equals(completedBy)) {
            task.isCompleted = true;
            task.completionTime = System.currentTimeMillis();

            // Update project progress
            GroupProject project = activeProjects.get(task.projectId);
            if (project != null) {
                project.completedTasks.add(taskId);
                project.remainingTasks.remove(taskId);
                project.updateActivity(task.completionTime);

                // Calculate completion percentage
                int total = project.completedTasks.size() + project.remainingTasks.size();
                project.completionPercentage = total > 0 ?
                        (double) project.completedTasks.size() / total : 0.0;

                // Check if project is complete
                if (project.remainingTasks.isEmpty()) {
                    project.status = ProjectStatus.COMPLETED;

                    recordEvent("PROJECT_COMPLETED",
                            "Project completed: " + project.title,
                            project.projectLeader,
                            project.participants,
                            task.completionTime,
                            project.targetLocation);
                }
            }

            return true;
        }
        return false;
    }

    public void cancelProject(String projectId, UUID cancelledBy) {
        GroupProject project = activeProjects.get(projectId);
        if (project != null) {
            project.status = ProjectStatus.CANCELLED;

            recordEvent("PROJECT_CANCELLED",
                    "Project cancelled: " + project.title,
                    cancelledBy,
                    project.participants,
                    System.currentTimeMillis(),
                    project.targetLocation);

            // Remove associated tasks
            activeTasks.entrySet().removeIf(entry ->
                    entry.getValue().projectId.equals(projectId));
        }
    }

    public List<CoordinationEvent> getRecentEvents() {
        return new ArrayList<>(recentEvents);
    }

    public LeadershipProfile getLeadershipProfile(UUID agentId) {
        return leadershipProfiles.get(agentId);
    }

    /**
     * System tick for cleanup and updates
     */
    public void tick(AgentEntity agent) {
        long currentTime = agent.level().getGameTime();

        // Clean up timed out projects
        activeProjects.entrySet().removeIf(entry -> {
            GroupProject project = entry.getValue();
            if (project.isTimeout(currentTime)) {
                recordEvent("PROJECT_TIMEOUT",
                        "Project timed out: " + project.title,
                        project.projectLeader,
                        project.participants,
                        currentTime,
                        project.targetLocation);
                return true;
            }
            return false;
        });

        // Clean up completed tasks
        activeTasks.entrySet().removeIf(entry -> {
            CollaborativeTask task = entry.getValue();
            return task.isCompleted &&
                    (currentTime - task.completionTime) > 6000; // 5 minutes after completion
        });

        // Update project coordination strategies periodically
        if (currentTime % 1200 == 0) { // Every minute
            for (GroupProject project : activeProjects.values()) {
                if (project.status == ProjectStatus.ACTIVE) {
                    updateCoordinationStrategy(project);
                }
            }
        }
    }
}