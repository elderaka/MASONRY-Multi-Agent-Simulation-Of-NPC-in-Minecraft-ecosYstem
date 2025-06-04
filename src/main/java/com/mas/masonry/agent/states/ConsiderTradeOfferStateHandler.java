package com.mas.masonry.agent.states;

import com.mas.masonry.AgentEntity;
import com.mas.masonry.MASONRY;
import com.mas.masonry.agent.systems.TradingSystem;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import java.util.List;

public class ConsiderTradeOfferStateHandler implements IAgentStateHandler {
    
    private static final int CONSIDERATION_TIME = 60; // 3 seconds to think
    
    @Override
    public void handle(AgentEntity agent) {
        // Think for a moment
        if (agent.getMemory().getTicksInCurrentState() < CONSIDERATION_TIME) {
            if (agent.getMemory().getTicksInCurrentState() == 20) {
                Component thinkingMessage = Component.literal("<" + agent.getBaseName() + "> " +
                    "Hmm, let me think about these trade offers...");
                agent.sendChatMessage(thinkingMessage);
            }
            return;
        }
        
        // Look for trade offers that interest us
        Item neededItem = findMostNeededItem(agent);
        
        if (neededItem != null) {
            List<TradingSystem.TradeOffer> offers = 
                agent.getTradingSystem().findSuitableOffers(agent, neededItem);
            
            if (!offers.isEmpty()) {
                // Accept the best offer
                TradingSystem.TradeOffer bestOffer = offers.get(0);
                boolean accepted = agent.getTradingSystem().acceptTradeOffer(agent, bestOffer);
                
                if (accepted) {
                    String message = "Deal! Gue terima tawaran trade " + 
                        bestOffer.offeredItem.getDescription().getString() + "!";
                    Component chatMessage = Component.literal("<" + agent.getBaseName() + "> " + message);
                    agent.sendChatMessage(chatMessage);
                    
                    MASONRY.LOGGER.debug("{} accepted trade offer for {}", 
                        agent.getBaseName(), bestOffer.offeredItem.getDescription().getString());
                } else {
                    Component failMessage = Component.literal("<" + agent.getBaseName() + "> " +
                        "Waduh, trade nya gagal... Maybe next time!");
                    agent.sendChatMessage(failMessage);
                }
            } else {
                Component noOffersMessage = Component.literal("<" + agent.getBaseName() + "> " +
                    "Nggak ada offer yang menarik sih...");
                agent.sendChatMessage(noOffersMessage);
            }
        }
        
        agent.setCurrentState(AgentEntity.AgentState.IDLE);
    }
    
    private Item findMostNeededItem(AgentEntity agent) {
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
        
        return null;
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
}