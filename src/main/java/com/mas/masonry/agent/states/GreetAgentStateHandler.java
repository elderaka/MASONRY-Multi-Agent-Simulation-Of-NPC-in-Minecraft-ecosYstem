package com.mas.masonry.agent.states;

import com.mas.masonry.AgentEntity;
import com.mas.masonry.MASONRY;

public class GreetAgentStateHandler implements IAgentStateHandler {

    @Override
    public void handle(AgentEntity agent) {
        // Logic for greeting another agent will go here.
        // For now, let's log and transition back to IDLE after a short time.
        MASONRY.LOGGER.info("{} is in GREET_AGENT state.", agent.getName().getString());

        // TODO: Implement actual greeting logic:
        // 1. Identify a nearby friendly agent to greet.
        // 2. Make the agent look at the target agent.
        // 3. Send a chat message (e.g., using AgentChatter).
        // 4. Add a cooldown to prevent re-greeting the same agent too quickly.
        // 5. Transition to another state (e.g., IDLE or CHAT_WITH_AGENT) after greeting.

        if (agent.getMemory().getTicksInCurrentState() > 40) { // Agent greets for 2 seconds (40 ticks)
            agent.getMemory().increaseSocialMeter(5); // Increase social meter after successful greeting
            agent.setCurrentState(AgentEntity.AgentState.IDLE);
        }
    }
}
