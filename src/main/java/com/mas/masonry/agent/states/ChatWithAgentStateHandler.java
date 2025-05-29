package com.mas.masonry.agent.states;

import com.mas.masonry.AgentEntity;
import com.mas.masonry.MASONRY;

public class ChatWithAgentStateHandler implements IAgentStateHandler {

    @Override
    public void handle(AgentEntity agent) {
        // Logic for chatting with another agent will go here.
        MASONRY.LOGGER.info("{} is in CHAT_WITH_AGENT state.", agent.getName().getString());

        // TODO: Implement actual chatting logic:
        // 1. Identify a nearby friendly agent to chat with (could be the one just greeted).
        // 2. Exchange a few chat messages (e.g., using AgentChatter, perhaps with new chat categories).
        // 3. Add a timer or condition to end the chat.
        // 4. Transition to another state (e.g., IDLE or a task-related state) after chatting.

        if (agent.getMemory().getTicksInCurrentState() > 100) { // Agent chats for 5 seconds (100 ticks)
            agent.getMemory().increaseSocialMeter(10); // Increase social meter after successful chat
            agent.setCurrentState(AgentEntity.AgentState.IDLE);
        }
    }
}
