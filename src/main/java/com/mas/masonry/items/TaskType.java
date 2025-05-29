package com.mas.masonry.items;

import com.mas.masonry.AgentEntity;
import net.minecraft.network.chat.Component;

public enum TaskType {
    BUILD("Build", AgentEntity.AgentState.PLACE_CONSTRUCTION_BLOCK),
    HARVEST("Harvest", AgentEntity.AgentState.HARVEST_BLOCK), // Assumes HARVEST_BLOCK exists in AgentState
    WANDER("Wander", AgentEntity.AgentState.WANDER),
    IDLE("Idle", AgentEntity.AgentState.IDLE);

    private final String displayName;
    private final AgentEntity.AgentState agentState;

    TaskType(String displayName, AgentEntity.AgentState agentState) {
        this.displayName = displayName;
        this.agentState = agentState;
    }

    public String getDisplayName() {
        return displayName;
    }

    public AgentEntity.AgentState getAgentState() {
        return agentState;
    }

    public Component getDisplayComponent() {
        return Component.translatable("item.masonry.task_paper.task_type." + this.name().toLowerCase());
    }

    public TaskType next() {
        TaskType[] values = values();
        int nextOrdinal = (this.ordinal() + 1) % values.length;
        return values[nextOrdinal];
    }

    public static TaskType fromOrdinal(int ordinal) {
        TaskType[] values = values();
        if (ordinal < 0 || ordinal >= values.length) {
            return values[0]; // Default to the first task if ordinal is out of bounds
        }
        return values[ordinal];
    }
}
