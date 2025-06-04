package com.mas.masonry.agent.states;

import com.mas.masonry.AgentEntity;
import com.mas.masonry.MASONRY;
import com.mas.masonry.agent.systems.CoordinationSystem;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import java.util.List;

public class ParticipateInProjectStateHandler implements IAgentStateHandler {
    
    private static final int TASK_CHECK_INTERVAL = 60; // 3 seconds
    private static final int PROGRESS_REPORT_INTERVAL = 400; // 20 seconds
    
    @Override
    public void handle(AgentEntity agent) {
        // Find the project this agent is participating in
        CoordinationSystem.GroupProject project = findParticipatingProject(agent);
        
        if (project == null) {
            // No project to participate in, check for invitations
            checkForProjectInvitations(agent);
            agent.setCurrentState(AgentEntity.AgentState.IDLE);
            return;
        }
        
        // Check for assigned tasks
        if (agent.getMemory().getTicksInCurrentState() % TASK_CHECK_INTERVAL == 0) {
            performAssignedTasks(agent, project);
        }
        
        // Report progress periodically
        if (agent.getMemory().getTicksInCurrentState() % PROGRESS_REPORT_INTERVAL == 0) {
            reportProgress(agent, project);
        }
        
        // Move towards project location or task location
        BlockPos targetLocation = getTaskLocation(agent, project);
        BlockPos agentPos = agent.blockPosition();
        double distance = Math.sqrt(agentPos.distSqr(targetLocation));

        if (distance > 4.0) {
            agent.getNavigation().moveTo(targetLocation.getX(), targetLocation.getY(), targetLocation.getZ(), 1.0);
        }
        
        // End participation if project is completed or cancelled
        if (project.status == CoordinationSystem.ProjectStatus.COMPLETED ||
            project.status == CoordinationSystem.ProjectStatus.CANCELLED) {
            
            celebrateOrCommiserate(agent, project);
            agent.setCurrentState(AgentEntity.AgentState.IDLE);
        }
    }
    
    private CoordinationSystem.GroupProject findParticipatingProject(AgentEntity agent) {
        return agent.getCoordinationSystem().getActiveProjects().stream()
            .filter(project -> project.participants.contains(agent.getUUID()))
            .filter(project -> !project.projectLeader.equals(agent.getUUID())) // Not leading
            .filter(project -> project.isActive())
            .findFirst()
            .orElse(null);
    }
    
    private void checkForProjectInvitations(AgentEntity agent) {
        // Look for projects that have invited this agent
        List<CoordinationSystem.GroupProject> invitations = 
            agent.getCoordinationSystem().getActiveProjects().stream()
                .filter(project -> project.invitedAgents.contains(agent.getUUID()))
                .filter(project -> project.status == CoordinationSystem.ProjectStatus.RECRUITING)
                .toList();
        
        if (!invitations.isEmpty()) {
            // Decide whether to join based on project appeal
            CoordinationSystem.GroupProject bestInvitation = invitations.stream()
                .max((p1, p2) -> Double.compare(
                    calculateProjectAppeal(agent, p1),
                    calculateProjectAppeal(agent, p2)
                ))
                .orElse(null);
            
            if (bestInvitation != null && shouldJoinProject(agent, bestInvitation)) {
                boolean joined = agent.getCoordinationSystem().joinProject(agent, bestInvitation.id);
                
                if (joined) {
                    String acceptMessage = "Count me in! Gue join project " + bestInvitation.title + "!";
                    Component chatMessage = Component.literal("<" + agent.getBaseName() + "> " + acceptMessage);
                    agent.sendChatMessage(chatMessage);
                    
                    MASONRY.LOGGER.debug("{} joined project: {}", 
                        agent.getBaseName(), bestInvitation.title);
                }
            }
        }
    }
    
    private double calculateProjectAppeal(AgentEntity agent, CoordinationSystem.GroupProject project) {
        double appeal = 1.0;
        
        // Distance factor (closer is more appealing)
        BlockPos agentPos = agent.blockPosition();
        double distance = Math.sqrt(agentPos.distSqr(project.targetLocation));

        appeal *= Math.max(0.2, 1.0 - (distance / 64.0));
        
        // Priority factor
        appeal *= project.priority.multiplier;
        
        // Leader reputation factor
        if (agent.getGossipSystem().isWellRegarded(project.projectLeader)) {
            appeal *= 1.5;
        } else if (agent.getGossipSystem().hasBadReputation(project.projectLeader)) {
            appeal *= 0.5;
        }
        
        // Profession relevance
        if (isProfessionRelevant(agent, project.type)) {
            appeal *= 1.3;
        }
        
        return appeal;
    }
    
    private boolean shouldJoinProject(AgentEntity agent, CoordinationSystem.GroupProject project) {
        double appeal = calculateProjectAppeal(agent, project);
        double threshold = 0.6; // Minimum appeal to join
        
        // Random factor for personality variation
        threshold += (agent.getRandom().nextDouble() - 0.5) * 0.3; // ±0.15 variation
        
        return appeal >= threshold;
    }
    
    private void performAssignedTasks(AgentEntity agent, CoordinationSystem.GroupProject project) {
        List<CoordinationSystem.CollaborativeTask> tasks = 
            agent.getCoordinationSystem().getAgentTasks(agent.getUUID());
        
        if (tasks.isEmpty()) {
            // No specific tasks, contribute generally
            contributeToProject(agent, project);
            return;
        }
        
        // Work on the highest priority task
        CoordinationSystem.CollaborativeTask currentTask = tasks.stream()
            .max((t1, t2) -> Double.compare(t1.priority.multiplier, t2.priority.multiplier))
            .orElse(null);
        
        if (currentTask != null) {
            workOnTask(agent, currentTask, project);
        }
    }
    
    private void workOnTask(AgentEntity agent, CoordinationSystem.CollaborativeTask task, 
                           CoordinationSystem.GroupProject project) {
        
        // Task-specific work logic
        switch (getTaskType(task.id)) {
            case "gather":
                performGatheringTask(agent, task, project);
                break;
            case "build":
                performBuildingTask(agent, task, project);
                break;
            case "scout":
                performScoutingTask(agent, task, project);
                break;
            case "transport":
                performTransportTask(agent, task, project);
                break;
            default:
                performGenericTask(agent, task, project);
        }
        
        // Chance to complete task
        if (agent.getRandom().nextInt(10) == 0) { // 10% chance per check
            completeTask(agent, task, project);
        }
    }
    
    private void performGatheringTask(AgentEntity agent, CoordinationSystem.CollaborativeTask task, 
                                    CoordinationSystem.GroupProject project) {
        // Look for resources to gather
        String gatherTarget = extractGatherTarget(task.description);
        
        if (agent.getMemory().getTicksInCurrentState() % 100 == 0) { // Every 5 seconds
            String workMessage = "Working on gathering " + gatherTarget + " for " + project.title + "...";
            Component chatMessage = Component.literal("<" + agent.getBaseName() + "> " + workMessage);
            agent.sendChatMessage(chatMessage);
        }
        
        // Simulate gathering work
        agent.swing(agent.getUsedItemHand());
    }
    
    private void performBuildingTask(AgentEntity agent, CoordinationSystem.CollaborativeTask task, 
                                   CoordinationSystem.GroupProject project) {
        // Move to construction site
        BlockPos buildSite = project.targetLocation;
        BlockPos agentPos = agent.blockPosition();
        double distance = Math.sqrt(agentPos.distSqr(buildSite));


        if (distance <= 4.0) {
            if (agent.getMemory().getTicksInCurrentState() % 80 == 0) { // Every 4 seconds
                String buildMessage = "Constructing " + extractBuildTarget(task.description) + "...";
                Component chatMessage = Component.literal("<" + agent.getBaseName() + "> " + buildMessage);
                agent.sendChatMessage(chatMessage);
            }
            
            // Simulate building work
            agent.swing(agent.getUsedItemHand());
            
            // Try to place blocks if we have building materials
            placeBuildingBlocks(agent, buildSite);
        }
    }
    
    private void performScoutingTask(AgentEntity agent, CoordinationSystem.CollaborativeTask task, 
                                   CoordinationSystem.GroupProject project) {
        // Move around the project area
        BlockPos scoutCenter = project.targetLocation;
        
        if (agent.getMemory().getTicksInCurrentState() % 120 == 0) { // Every 6 seconds
            String scoutMessage = "Scouting area for " + project.title + ", checking for threats...";
            Component chatMessage = Component.literal("<" + agent.getBaseName() + "> " + scoutMessage);
            agent.sendChatMessage(chatMessage);
            
            // Create danger gossip if threats found (randomly)
            if (agent.getRandom().nextInt(20) == 0) { // 5% chance
                agent.getGossipSystem().createDangerGossip(agent, "potential threats", scoutCenter);
            }
        }
        
        // Random movement for scouting
        if (!agent.getNavigation().isDone()) {
            return;
        }
        
        double angle = agent.getRandom().nextDouble() * 2 * Math.PI;
        double radius = 8 + agent.getRandom().nextDouble() * 16; // 8-24 blocks
        
        BlockPos scoutTarget = scoutCenter.offset(
            (int)(Math.cos(angle) * radius),
            0,
            (int)(Math.sin(angle) * radius)
        );
        
        agent.getNavigation().moveTo(scoutTarget.getX(), scoutTarget.getY(), scoutTarget.getZ(), 1.2);
    }
    
    private void performTransportTask(AgentEntity agent, CoordinationSystem.CollaborativeTask task, 
                                    CoordinationSystem.GroupProject project) {
        if (agent.getMemory().getTicksInCurrentState() % 100 == 0) { // Every 5 seconds
            String transportMessage = "Transporting materials for " + project.title + "...";
            Component chatMessage = Component.literal("<" + agent.getBaseName() + "> " + transportMessage);
            agent.sendChatMessage(chatMessage);
        }
        
        // Simulate material transport
        organizeInventory(agent);
    }
    
    private void performGenericTask(AgentEntity agent, CoordinationSystem.CollaborativeTask task, 
                                  CoordinationSystem.GroupProject project) {
        if (agent.getMemory().getTicksInCurrentState() % 100 == 0) { // Every 5 seconds
            String workMessage = "Working on " + task.description + "...";
            Component chatMessage = Component.literal("<" + agent.getBaseName() + "> " + workMessage);
            agent.sendChatMessage(chatMessage);
        }
    }
    
    private void contributeToProject(AgentEntity agent, CoordinationSystem.GroupProject project) {
        // General contribution when no specific task assigned
        switch (project.type) {
            case COMMUNITY_BUILD:
            case INFRASTRUCTURE:
                // Try to help with building
                contributeToBuilding(agent, project);
                break;
            case RESOURCE_EXPEDITION:
                // Help gather resources
                contributeToGathering(agent, project);
                break;
            case DEFENSE_PATROL:
                // Help with defense
                contributeToDefense(agent, project);
                break;
            default:
                // General project support
                contributeGenerally(agent, project);
        }
    }
    
    private void contributeToBuilding(AgentEntity agent, CoordinationSystem.GroupProject project) {
        if (agent.getMemory().getTicksInCurrentState() % 120 == 0) { // Every 6 seconds
            String helpMessage = "Helping with construction for " + project.title + "!";
            Component chatMessage = Component.literal("<" + agent.getBaseName() + "> " + helpMessage);
            agent.sendChatMessage(chatMessage);
        }
    }
    
    private void contributeToGathering(AgentEntity agent, CoordinationSystem.GroupProject project) {
        if (agent.getMemory().getTicksInCurrentState() % 120 == 0) { // Every 6 seconds
            String helpMessage = "Contributing to resource gathering for " + project.title + "!";
            Component chatMessage = Component.literal("<" + agent.getBaseName() + "> " + helpMessage);
            agent.sendChatMessage(chatMessage);
        }
    }
    
    private void contributeToDefense(AgentEntity agent, CoordinationSystem.GroupProject project) {
        if (agent.getMemory().getTicksInCurrentState() % 120 == 0) { // Every 6 seconds
            String helpMessage = "Maintaining security for " + project.title + "!";
            Component chatMessage = Component.literal("<" + agent.getBaseName() + "> " + helpMessage);
            agent.sendChatMessage(chatMessage);
        }
    }
    
    private void contributeGenerally(AgentEntity agent, CoordinationSystem.GroupProject project) {
        if (agent.getMemory().getTicksInCurrentState() % 120 == 0) { // Every 6 seconds
            String helpMessage = "Supporting " + project.title + " however I can!";
            Component chatMessage = Component.literal("<" + agent.getBaseName() + "> " + helpMessage);
            agent.sendChatMessage(chatMessage);
        }
    }
    
    private void completeTask(AgentEntity agent, CoordinationSystem.CollaborativeTask task, 
                            CoordinationSystem.GroupProject project) {
        
        boolean completed = agent.getCoordinationSystem().completeTask(task.id, agent.getUUID());
        
        if (completed) {
            String completionMessage = "Task completed: " + task.description + "! ✓";
            Component chatMessage = Component.literal("<" + agent.getBaseName() + "> " + completionMessage);
            agent.sendChatMessage(chatMessage);
            
            // Create positive reputation gossip for good work
            if (agent.getRandom().nextInt(3) == 0) { // 33% chance
                agent.getGossipSystem().createReputationGossip(
                    agent, agent, true, "Great teamwork on " + project.title
                );
            }
            
            MASONRY.LOGGER.debug("{} completed task: {} for project: {}", 
                agent.getBaseName(), task.description, project.title);
        }
    }
    
    private void reportProgress(AgentEntity agent, CoordinationSystem.GroupProject project) {
        String[] progressReports = {
            "Making good progress on " + project.title + "!",
            "Work is going smoothly untuk " + project.title + ".",
            project.title + " is coming along nicely!",
            "Steady progress untuk project kita!",
            "Things are looking good for " + project.title + "!"
        };
        
        String report = progressReports[agent.getRandom().nextInt(progressReports.length)];
        Component chatMessage = Component.literal("<" + agent.getBaseName() + "> " + report);
        agent.sendChatMessage(chatMessage);
    }
    
    private void celebrateOrCommiserate(AgentEntity agent, CoordinationSystem.GroupProject project) {
        String message;
        
        if (project.status == CoordinationSystem.ProjectStatus.COMPLETED) {
            String[] celebrationMessages = {
                "Awesome! " + project.title + " completed successfully!",
                "Great teamwork everyone! Project done!",
                "Fantastic collaboration on " + project.title + "!",
                "Mission accomplished untuk " + project.title + "!",
                "Excellent work team! Proud of this project!"
            };
            message = celebrationMessages[agent.getRandom().nextInt(celebrationMessages.length)];
            
            // Create success gossip
            agent.getGossipSystem().createGeneralGossip(agent,
                "Just finished working on " + project.title + " - great experience!");
            
        } else {
            String[] commiserationMessages = {
                "Too bad " + project.title + " didn't work out...",
                "Project cancelled, but good effort everyone!",
                "Maybe next time untuk " + project.title + ".",
                "Not all projects succeed, that's okay!",
                "Good experience anyway dengan " + project.title + "."
            };
            message = commiserationMessages[agent.getRandom().nextInt(commiserationMessages.length)];
        }
        
        Component chatMessage = Component.literal("<" + agent.getBaseName() + "> " + message);
        agent.sendChatMessage(chatMessage);
    }
    
    // === HELPER METHODS ===
    
    private BlockPos getTaskLocation(AgentEntity agent, CoordinationSystem.GroupProject project) {
        // For now, return project location. In a full implementation,
        // this would determine task-specific locations
        return project.targetLocation;
    }
    
    private String getTaskType(String taskId) {
        if (taskId.contains("gather")) return "gather";
        if (taskId.contains("build") || taskId.contains("construct")) return "build";
        if (taskId.contains("scout") || taskId.contains("patrol")) return "scout";
        if (taskId.contains("transport") || taskId.contains("carry")) return "transport";
        return "generic";
    }
    
    private String extractGatherTarget(String description) {
        // Extract what to gather from task description
        if (description.contains("materials")) return "construction materials";
        if (description.contains("wood")) return "wood";
        if (description.contains("stone")) return "stone";
        if (description.contains("iron")) return "iron";
        return "resources";
    }
    
    private String extractBuildTarget(String description) {
        // Extract what to build from task description
        if (description.contains("foundation")) return "foundation";
        if (description.contains("walls")) return "walls";
        if (description.contains("roof")) return "roof";
        if (description.contains("structure")) return "main structure";
        return "building components";
    }
    
    private void placeBuildingBlocks(AgentEntity agent, BlockPos buildSite) {
        // Simulate placing blocks for construction
        // In a full implementation, this would actually place blocks
        if (agent.getRandom().nextInt(20) == 0) { // 5% chance per attempt
            agent.swing(agent.getUsedItemHand());
        }
    }
    
    private void organizeInventory(AgentEntity agent) {
        // Simulate organizing inventory for efficiency
        // In a full implementation, this would optimize item placement
    }
    
    private boolean isProfessionRelevant(AgentEntity agent, CoordinationSystem.ProjectType type) {
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
            case DEFENSE_PATROL:
                return profession.equals("Guard") || profession.equals("Scout");
            default:
                return false;
        }
    }
}