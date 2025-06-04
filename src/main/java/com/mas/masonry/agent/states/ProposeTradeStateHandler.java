package com.mas.masonry.agent.states;

import com.mas.masonry.AgentChatter;
import com.mas.masonry.AgentEntity;
import com.mas.masonry.MASONRY;
import com.mas.masonry.agent.systems.TradingSystem;
import com.mas.masonry.agent.systems.ScheduleSystem;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;

public class ProposeTradeStateHandler implements IAgentStateHandler {

    private static final int MIN_PROPOSE_TIME = 100; // 5 seconds minimum
    private static final int MAX_PROPOSE_TIME = 600; // 30 seconds maximum
    private static final double CLOSE_ENOUGH_TO_TARGET = 5.0;
    private static final int TRADE_SEARCH_RADIUS = 16;
    private static final int TRADE_PROPOSAL_DURATION = 200; // 10 seconds
    @Override
    public void handle(AgentEntity agent) {
        if (agent.getMemory().getTicksInCurrentState() < 40) {
            return;
        }
        if (agent.getMemory().getTicksInCurrentState() < MIN_PROPOSE_TIME) {
            return; // Stay in this state for minimum time
        }
        if (!agent.getScheduleSystem().isSocialTime(agent.level())) {
            // Check what activity we should be doing
            ScheduleSystem.AgentActivity currentActivity = agent.getScheduleSystem().determineActivity(agent.level(), agent);

            if (currentActivity == ScheduleSystem.AgentActivity.WORK) {
                agent.setCurrentState(AgentEntity.AgentState.GO_TO_WORK);
            } else if (currentActivity == ScheduleSystem.AgentActivity.BREAK) {
                agent.setCurrentState(AgentEntity.AgentState.TAKE_WORK_BREAK);
            } else {
                agent.setCurrentState(AgentEntity.AgentState.IDLE);
            }
            return;
        }





        // Find someone to trade with
        AgentEntity target;

        if (!agent.getMemory().getTargetEntity().isPresent()) {
            target = findTradePartner(agent);
            if (target == null) {
                // No one to trade with - go back to previous activity
                agent.setCurrentState(AgentEntity.AgentState.IDLE);
                return;
            }
            agent.getMemory().setTargetEntity(target);
        } else {
            target = (AgentEntity) agent.getMemory().getTargetEntity().get();

            // Check if target is still valid
            if (target.isRemoved() || agent.distanceTo(target) > 32.0) {
                agent.getMemory().clearTargetEntity();
                agent.setCurrentState(AgentEntity.AgentState.IDLE);
                return;
            }
        }



        // Move close to target
        double distance = agent.distanceTo(target);
        if (distance > 4.0) {
            agent.getNavigation().moveTo(target, 1.0);
            return;
        }
        
        agent.getNavigation().stop();
        lookAtTarget(agent, target);

        // Make trade proposal once only
        if (agent.getMemory().getTicksInCurrentState() == 80) { // After 4 seconds
            boolean proposed = proposeTradeToTarget(agent, target);

            if (!proposed) {
                MASONRY.LOGGER.debug("{} couldn't propose trade to {}",
                        agent.getBaseName(), target.getBaseName());
                agent.setCurrentState(AgentEntity.AgentState.IDLE);
                return;
            }
        }


        // End after duration
        if (agent.getMemory().getTicksInCurrentState() > TRADE_PROPOSAL_DURATION) {
            agent.getMemory().clearTargetEntity();
            agent.setCurrentState(AgentEntity.AgentState.IDLE);
        }

    }

    private AgentEntity findTradePartner(AgentEntity agent) {
        return agent.level().getEntitiesOfClass(AgentEntity.class,
                        agent.getBoundingBox().inflate(16.0)) // Reduced search radius
                .stream()
                .filter(otherAgent -> otherAgent != agent)
                .filter(otherAgent -> !agent.getMemory().hasRecentlyCommunicated(otherAgent))
                .filter(otherAgent -> otherAgent.getCurrentState() != AgentEntity.AgentState.PROPOSE_TRADE) // Don't interrupt other traders
                .min((a1, a2) -> Double.compare(agent.distanceTo(a1), agent.distanceTo(a2)))
                .orElse(null);
    }


    private boolean proposeTradeToTarget(AgentEntity proposer, AgentEntity target) {
        // Suggest a trade automatically
        TradingSystem.TradeOffer suggestedTrade = proposer.getTradingSystem().suggestTrade(proposer);
        
        if (suggestedTrade == null) {
            // No good trade to propose
            Component message = Component.literal("<" + proposer.getBaseName() + "> " +
                "Hmm, gue lagi nggak ada yang bisa ditukerin nih...");
            proposer.sendChatMessage(message);
            return false;
        }
        
        // Create the trade offer
        boolean offerCreated = proposer.getTradingSystem().createTradeOffer(
            proposer,
            suggestedTrade.offeredItem,
            suggestedTrade.offeredQuantity,
            suggestedTrade.requestedItem,
            suggestedTrade.requestedQuantity
        );
        
        if (offerCreated) {
            // Send trade proposal message
            String message = "Eh " + target.getBaseName() + ", mau tukar nggak? " +
                "Gue kasih " + suggestedTrade.offeredQuantity + " " + 
                suggestedTrade.offeredItem.getDescription().getString() +
                " tuker sama " + suggestedTrade.requestedQuantity + " " +
                suggestedTrade.requestedItem.getDescription().getString() + "!";
            
            Component chatMessage = Component.literal("<" + proposer.getBaseName() + "> " + message);
            proposer.sendChatMessage(chatMessage);
            
            // Record communication
            proposer.getMemory().recordCommunication(target);
            
            return true;
        }
        
        return false;
    }
    
    private void lookAtTarget(AgentEntity agent, AgentEntity target) {
        double dx = target.getX() - agent.getX();
        double dz = target.getZ() - agent.getZ();
        float yaw = (float) (Math.atan2(dz, dx) * 180.0 / Math.PI) - 90.0F;
        agent.setYRot(yaw);
        agent.setYHeadRot(yaw);
    }
}