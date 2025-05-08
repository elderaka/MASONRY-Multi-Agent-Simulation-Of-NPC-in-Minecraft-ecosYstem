package com.example.examplemod.entity.fsm;

import com.example.examplemod.entity.EntityAgent;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.player.Player;

/**
 * AgentStateMachine implements a simple Finite State Machine for the EntityAgent.
 * It manages state transitions and associated behaviors.
 */
public class AgentStateMachine {
    private final EntityAgent agent;
    private AgentState currentState;
    private Goal currentGoal;

    public AgentStateMachine(EntityAgent agent) {
        this.agent = agent;
        this.currentState = AgentState.IDLE;
        this.currentGoal = null;
    }

    /**
     * Update the state machine and execute current state behavior
     */
    public void update() {
        switch (currentState) {
            case IDLE -> handleIdleState();
            case EXPLORE -> handleExploreState();
            case COLLECT -> handleCollectState();
            case FIGHT -> handleFightState();
        }
    }

    /**
     * Set the current state of the agent
     */
    public void setCurrentState(AgentState newState) {
        if (currentState != newState) {
            // Clear current goal when changing states
            if (currentGoal != null) {
                agent.goalSelector.removeGoal(currentGoal);
                currentGoal = null;
            }
            
            currentState = newState;
            update();
        }
    }

    /**
     * Get the current state
     */
    public AgentState getCurrentState() {
        return currentState;
    }

    private void handleIdleState() {
        if (currentGoal == null) {
            currentGoal = new LookAtPlayerGoal(agent, Player.class, 8.0F);
            agent.goalSelector.addGoal(3, currentGoal);
        }
    }

    private void handleExploreState() {
        if (currentGoal == null) {
            currentGoal = new RandomStrollGoal(agent, 1.0D);
            agent.goalSelector.addGoal(3, currentGoal);
        }
    }

    private void handleCollectState() {
        // TODO: Implement collection behavior
        // This could involve finding and moving towards items
    }

    private void handleFightState() {
        if (currentGoal == null) {
            currentGoal = new MeleeAttackGoal(agent, 1.0D, true);
            agent.goalSelector.addGoal(3, currentGoal);
        }
    }
} 