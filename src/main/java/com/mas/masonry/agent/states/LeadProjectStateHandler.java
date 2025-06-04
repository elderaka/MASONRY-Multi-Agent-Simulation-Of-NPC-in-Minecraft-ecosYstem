package com.mas.masonry.agent.states;

import com.mas.masonry.AgentEntity;
import com.mas.masonry.MASONRY;
import com.mas.masonry.agent.systems.CoordinationSystem;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

import java.util.Random;

public class LeadProjectStateHandler implements IAgentStateHandler {
    
    private static final int LEADERSHIP_CHECK_INTERVAL = 100; // 5 seconds
    private static final int PROGRESS_UPDATE_INTERVAL = 200; // 10 seconds
    
    @Override
    public void handle(AgentEntity agent) {
        // Find the project this agent is leading
        CoordinationSystem.GroupProject project = findLeadingProject(agent);
        
        if (project == null) {
            // No project to lead, return to idle
            agent.setCurrentState(AgentEntity.AgentState.IDLE);
            return;
        }
        
        // Leadership activities based on time in state
        int ticksInState = agent.getMemory().getTicksInCurrentState();
        
        if (ticksInState % LEADERSHIP_CHECK_INTERVAL == 0) {
            performLeadershipActivities(agent, project);
        }
        
        if (ticksInState % PROGRESS_UPDATE_INTERVAL == 0) {
            checkProjectProgress(agent, project);
        }
        
        // Move towards project location if needed
        BlockPos agentPos = agent.blockPosition();
        double distance = Math.sqrt(agentPos.distSqr(project.targetLocation));


        if (distance > 8.0) {
            agent.getNavigation().moveTo(project.targetLocation.getX(), 
                                       project.targetLocation.getY(), 
                                       project.targetLocation.getZ(), 1.0);
        }
        
        // End leadership if project is completed or cancelled
        if (project.status == CoordinationSystem.ProjectStatus.COMPLETED ||
            project.status == CoordinationSystem.ProjectStatus.CANCELLED) {
            
            provideFinalReport(agent, project);
            agent.setCurrentState(AgentEntity.AgentState.IDLE);
        }
    }
    
    private CoordinationSystem.GroupProject findLeadingProject(AgentEntity agent) {
        return agent.getCoordinationSystem().getActiveProjects().stream()
            .filter(project -> project.projectLeader.equals(agent.getUUID()))
            .filter(project -> project.isActive())
            .findFirst()
            .orElse(null);
    }
    
    private void performLeadershipActivities(AgentEntity leader, CoordinationSystem.GroupProject project) {
        // Assign tasks to participants
        leader.getCoordinationSystem().assignOptimalTasks(project);
        
        // Check if more participants are needed
        if (project.needsMoreParticipants()) {
            recruitMoreParticipants(leader, project);
        }
        
        // Coordinate with other project leaders
        leader.getCoordinationSystem().coordinateAcrossProjects(leader);
        
        // Provide leadership updates
        String updateMessage = generateLeadershipUpdate(project);
        Component chatMessage = Component.literal("<" + leader.getBaseName() + "> " + updateMessage);
        leader.sendChatMessage(chatMessage);
        
        MASONRY.LOGGER.debug("{} performed leadership activities for {}", 
            leader.getBaseName(), project.title);
    }
    
    private void recruitMoreParticipants(AgentEntity leader, CoordinationSystem.GroupProject project) {
        // Find potential recruits nearby
        leader.level().getEntitiesOfClass(AgentEntity.class, 
            leader.getBoundingBox().inflate(32.0))
            .stream()
            .filter(agent -> agent != leader)
            .filter(agent -> !project.participants.contains(agent.getUUID()))
            .filter(agent -> !agent.getGossipSystem().hasBadReputation(agent.getUUID()))
            .limit(2) // Recruit up to 2 agents at a time
            .forEach(recruit -> {
                project.invitedAgents.add(recruit.getUUID());
                
                String invitation = "Hei " + recruit.getBaseName() + 
                    ", mau join project " + project.title + "? Kita butuh bantuan!";
                Component chatMessage = Component.literal("<" + leader.getBaseName() + "> " + invitation);
                leader.sendChatMessage(chatMessage);
                
                // Create recruitment gossip
                leader.getGossipSystem().createGeneralGossip(leader,
                    "Lagi cari team member untuk " + project.title + ". Interested?");
            });
    }
    
    private void checkProjectProgress(AgentEntity leader, CoordinationSystem.GroupProject project) {
        double previousProgress = (Double) project.projectData.getOrDefault("last_reported_progress", 0.0);
        double currentProgress = project.completionPercentage;
        
        if (currentProgress - previousProgress >= 0.2) { // 20% progress milestone
            String progressReport = String.format(
                "Update: %s sudah %d%% selesai! Good progress, team!",
                project.title, (int)(currentProgress * 100)
            );
            
            Component chatMessage = Component.literal("<" + leader.getBaseName() + "> " + progressReport);
            leader.sendChatMessage(chatMessage);
            
            project.projectData.put("last_reported_progress", currentProgress);
            
            // Create progress gossip
            leader.getGossipSystem().createGeneralGossip(leader,
                project.title + " making good progress! " + (int)(currentProgress * 100) + "% done.");
        }
        
        // Check for stalled progress
        if (currentProgress == previousProgress && 
            (System.currentTimeMillis() - project.lastActivityTime) > 3000) { // 3 seconds no activity
            
            motivateTeam(leader, project);
        }
    }
    
    private void motivateTeam(AgentEntity leader, CoordinationSystem.GroupProject project) {
        String[] motivationalMessages = {
            "Ayo team, kita bisa! Let's push through!",
            "Progress might be slow, tapi kita steady. Keep going!",
            "Ingat tujuan kita! This project will benefit everyone!",
            "Good teamwork everyone! Mari kita selesaikan ini!",
            "Almost there! Sedikit lagi dan kita finish!"
        };
        
        String message = motivationalMessages[leader.getRandom().nextInt(motivationalMessages.length)];
        Component chatMessage = Component.literal("<" + leader.getBaseName() + "> " + message);
        leader.sendChatMessage(chatMessage);
    }
    
    private String generateLeadershipUpdate(CoordinationSystem.GroupProject project) {
        String[] updateTemplates = {
            "Project update: Mari kita focus on %s next!",
            "Team, kita perlu coordinate better untuk %s.",
            "Good work everyone! Next phase: %s.",
            "Update koordinasi: Priority sekarang adalah %s.",
            "Team briefing: Fokus kita adalah %s untuk saat ini."
        };
        
        String nextTask = project.remainingTasks.isEmpty() ? 
            "final touches" : project.remainingTasks.get(0);
        
        String template = updateTemplates[new Random().nextInt(updateTemplates.length)];
        return String.format(template, nextTask);
    }
    
    private void provideFinalReport(AgentEntity leader, CoordinationSystem.GroupProject project) {
        String finalMessage;
        
        if (project.status == CoordinationSystem.ProjectStatus.COMPLETED) {
            finalMessage = String.format(
                "Selamat team! Project %s sudah selesai! Great collaboration everyone!",
                project.title
            );
            
            // Create success gossip
            leader.getGossipSystem().createGeneralGossip(leader,
                "Success story: " + project.title + " completed successfully!");
            
            // Update leadership reputation
            CoordinationSystem.LeadershipProfile profile = 
                leader.getCoordinationSystem().getLeadershipProfile(leader.getUUID());
            if (profile != null) {
                profile.successfulProjects++;
                profile.leadershipScore = Math.min(100, profile.leadershipScore + 10);
            }
            
        } else {
            finalMessage = String.format(
                "Project %s dibatalkan. Thanks for the effort, team. Next time!",
                project.title
            );
            
            // Update leadership reputation (smaller penalty)
            CoordinationSystem.LeadershipProfile profile = 
                leader.getCoordinationSystem().getLeadershipProfile(leader.getUUID());
            if (profile != null) {
                profile.failedProjects++;
                profile.leadershipScore = Math.max(0, profile.leadershipScore - 5);
            }
        }
        
        Component chatMessage = Component.literal("<" + leader.getBaseName() + "> " + finalMessage);
        leader.sendChatMessage(chatMessage);
        
        MASONRY.LOGGER.info("{} concluded project: {} with status: {}", 
            leader.getBaseName(), project.title, project.status);
    }
}