package com.example.examplemod.entity.fsm;

/**
 * Enum representing the possible states of the EntityAgent.
 * Each state represents a different behavior pattern.
 */
public enum AgentState {
    /**
     * IDLE: The agent stays in place and looks around
     */
    IDLE,
    
    /**
     * EXPLORE: The agent moves around randomly to explore the environment
     */
    EXPLORE,
    
    /**
     * COLLECT: The agent searches for and collects items
     */
    COLLECT,
    
    /**
     * FIGHT: The agent engages in combat with detected threats
     */
    FIGHT
} 