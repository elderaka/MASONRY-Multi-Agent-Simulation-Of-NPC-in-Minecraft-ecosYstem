package com.mas.masonry.agent.states;

import com.mas.masonry.AgentChatter;
import com.mas.masonry.AgentEntity;
import com.mas.masonry.MASONRY;
import com.mas.masonry.agent.systems.GossipSystem;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;

public class ShareGossipStateHandler implements IAgentStateHandler {
    
    private static final int GOSSIP_SHARE_DURATION = 120; // 6 seconds
    private static final int MESSAGE_DELAY = 40; // 2 seconds between messages
    
    @Override
    public void handle(AgentEntity agent) {
        // Find someone to gossip with
        AgentEntity target;

        if (!agent.getMemory().getTargetEntity().isPresent()) {
            target = findGossipTarget(agent);
            if (target == null) {
                agent.setCurrentState(AgentEntity.AgentState.IDLE);
                return;
            }
            agent.getMemory().setTargetEntity(target);
        } else {
            target = (AgentEntity) agent.getMemory().getTargetEntity().get();
        }

        double distance = agent.distanceTo(target);



        if (distance > 4.0) {
            agent.getNavigation().moveTo(target, 1.0);
            return;
        }
        
        agent.getNavigation().stop();
        lookAtTarget(agent, target);
        
        // Share gossip at intervals
        if (agent.getMemory().getTicksInCurrentState() % MESSAGE_DELAY == 0 && 
            agent.getMemory().getTicksInCurrentState() > 0) {
            
            boolean shared = shareGossipMessage(agent, target);
            
            if (!shared && agent.getMemory().getTicksInCurrentState() > MESSAGE_DELAY) {
                // No gossip to share, end early
                agent.setCurrentState(AgentEntity.AgentState.IDLE);
                return;
            }
        }
        
        // End gossip session
        if (agent.getMemory().getTicksInCurrentState() > GOSSIP_SHARE_DURATION) {
            agent.setCurrentState(AgentEntity.AgentState.IDLE);
        }
    }
    
    private AgentEntity findGossipTarget(AgentEntity agent) {
        return agent.level().getEntitiesOfClass(AgentEntity.class, 
            agent.getBoundingBox().inflate(16.0))
            .stream()
            .filter(otherAgent -> otherAgent != agent)
            .filter(otherAgent -> !agent.getMemory().hasRecentlyCommunicated(otherAgent))
            .filter(otherAgent -> otherAgent.getMemory().isReadyToSocialize())
            .min((a1, a2) -> Double.compare(agent.distanceTo(a1), agent.distanceTo(a2)))
            .orElse(null);
    }
    
    private boolean shareGossipMessage(AgentEntity speaker, AgentEntity listener) {
        // Try to share actual gossip
        boolean gossipShared = speaker.getGossipSystem().shareGossipWith(speaker, listener);
        
        if (gossipShared) {
            // Get a random interesting gossip to talk about
            GossipSystem.GossipEntry gossip = speaker.getGossipSystem().getRandomInterestingGossip();
            
            if (gossip != null) {
                String message = createGossipMessage(gossip);
                Component chatMessage = Component.literal("<" + speaker.getBaseName() + "> " + message);
                speaker.sendChatMessage(chatMessage);
                
                // Record communication
                speaker.getMemory().recordCommunication(listener);
                return true;
            }
        }
        
        // Fallback to general conversation
        Component fallbackMessage = AgentChatter.getConversationMessage(speaker, listener, true);
        speaker.sendChatMessage(fallbackMessage);
        return false;
    }
    
    private String createGossipMessage(GossipSystem.GossipEntry gossip) {
        switch (gossip.type) {
            case RESOURCE_LOCATION:
                return "Eh, gue denger ada " + gossip.getData("item") + " bagus di area sebelah sana!";
            case DANGER_WARNING:
                return "Hati-hati ya, katanya ada " + gossip.getData("danger_type") + " di sekitar " + gossip.getData("location") + "!";
            case AGENT_REPUTATION:
                Boolean isPositive = (Boolean) gossip.getData("is_positive");
                String subjectName = (String) gossip.getData("subject_name");
                return isPositive ? 
                    "Si " + subjectName + " itu helpful banget, worth it deh diajak team!" :
                    "Si " + subjectName + " agak susah diajak kerjasama sih...";
            case TRADING_INFO:
                String traderName = (String) gossip.getData("trader_name");
                Boolean isSelling = (Boolean) gossip.getData("is_selling");
                String action = isSelling ? "jual" : "beli";
                return traderName + " lagi " + action + " " + gossip.getData("item") + " nih, mungkin lu butuh!";
            case GENERAL_NEWS:
                return gossip.content;
            default:
                return "Ada info menarik nih, " + gossip.content;
        }
    }
    
    private void lookAtTarget(AgentEntity agent, AgentEntity target) {
        double dx = target.getX() - agent.getX();
        double dz = target.getZ() - agent.getZ();
        float yaw = (float) (Math.atan2(dz, dx) * 180.0 / Math.PI) - 90.0F;
        agent.setYRot(yaw);
        agent.setYHeadRot(yaw);
    }
}