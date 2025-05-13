package com.mas.masonry;

import com.mas.masonry.AgentGoals;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.EnumMap;
import java.util.List;
import java.util.Optional;

/**
 * Base entity class for intelligent agents that use Finite State Machine (FSM)
 * for decision-making and behavior control.
 */
public class AgentEntity extends PathfinderMob {

    /**
     * Possible states for the Agent's FSM
     */
    public enum AgentState {
        IDLE,           // Default state, minimal activity
        WANDER,         // Randomly moving around
        SEEK_RESOURCE,  // Looking for specific resources (food, items)
        FLEE,           // Running away from danger
        ATTACK,         // Attacking a target
        HELP_ALLY       // Moving to assist another agent
    }

    /**
     * Agent's memory class to store information about the environment and internal state
     */
    public class AgentMemory {
        // Internal metrics
        private int hungerLevel = 0;        // 0-100, 0 = full, 100 = starving
        private int healthPercent = 100;    // 0-100 percentage of max health
        private int fearLevel = 0;          // 0-100, higher means more likely to flee
        
        // Entity/environment awareness
        private boolean dangerNearby = false;
        private boolean resourceNearby = false;
        private boolean allyNearby = false;
        
        // Target tracking
        private Optional<LivingEntity> targetEntity = Optional.empty();
        private Optional<Vec3> targetLocation = Optional.empty();
        
        // Additional memory for state persistence
        private int ticksInCurrentState = 0;
        private int lastStateChangeTime = 0;
        
        // Getters/setters
        public int getHungerLevel() { return hungerLevel; }
        public void setHungerLevel(int hungerLevel) { this.hungerLevel = hungerLevel; }
        
        public int getHealthPercent() { return healthPercent; }
        public void setHealthPercent(int healthPercent) { this.healthPercent = healthPercent; }
        
        public int getFearLevel() { return fearLevel; }
        public void setFearLevel(int fearLevel) { this.fearLevel = fearLevel; }
        
        public boolean isDangerNearby() { return dangerNearby; }
        public void setDangerNearby(boolean dangerNearby) { this.dangerNearby = dangerNearby; }
        
        public boolean isResourceNearby() { return resourceNearby; }
        public void setResourceNearby(boolean resourceNearby) { this.resourceNearby = resourceNearby; }
        
        public boolean isAllyNearby() { return allyNearby; }
        public void setAllyNearby(boolean allyNearby) { this.allyNearby = allyNearby; }
        
        public Optional<LivingEntity> getTargetEntity() { return targetEntity; }
        public void setTargetEntity(LivingEntity entity) { this.targetEntity = Optional.ofNullable(entity); }
        public void clearTargetEntity() { this.targetEntity = Optional.empty(); }
        
        public Optional<Vec3> getTargetLocation() { return targetLocation; }
        public void setTargetLocation(Vec3 location) { this.targetLocation = Optional.ofNullable(location); }
        public void clearTargetLocation() { this.clearTargetLocation = Optional.empty(); }
        
        public int getTicksInCurrentState() { return ticksInCurrentState; }
        public void incrementTicksInState() { this.ticksInCurrentState++; }
        public void resetTicksInState() { this.ticksInCurrentState = 0; }
        
        public void updateLastStateChangeTime(int gameTime) { this.lastStateChangeTime = gameTime; }
        public int getLastStateChangeTime() { return lastStateChangeTime; }
        
        /**
         * Updates the agent's perception of its health
         */
        public void updateHealthAwareness() {
            if (getHealth() > 0) {
                healthPercent = (int) ((getHealth() / getMaxHealth()) * 100);
            } else {
                healthPercent = 0;
            }
        }
        
        /**
         * Reset all perception flags, called at the beginning of each perception update
         */
        public void resetPerceptionFlags() {
            dangerNearby = false;
            resourceNearby = false;
            allyNearby = false;
        }
    }
    
    // Current state in the FSM
    private AgentState currentState = AgentState.IDLE;
    
    // Agent's memory
    private final AgentMemory memory;
    
    // Map of behaviors for each state
    private final EnumMap<AgentState, Runnable> stateBehaviors;
    
    // Perception radius 
    private final double perceptionRadius = 16.0;
    
    /**
     * Constructor for the agent entity
     */
    public AgentEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
        
        // Initialize memory
        this.memory = new AgentMemory();
        
        // Initialize state behaviors map
        this.stateBehaviors = new EnumMap<>(AgentState.class);
        initializeStateBehaviors();
    }

    /**
     * Create default attributes for the agent
     */
    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.ATTACK_DAMAGE, 3.0D)
                .add(Attributes.FOLLOW_RANGE, 16.0D);
    }
    
    /**
     * Sets up default behaviors for each state
     */
    private void initializeStateBehaviors() {
        stateBehaviors.put(AgentState.IDLE, this::handleIdleState);
        stateBehaviors.put(AgentState.WANDER, this::handleWanderState);
        stateBehaviors.put(AgentState.SEEK_RESOURCE, this::handleSeekResourceState);
        stateBehaviors.put(AgentState.FLEE, this::handleFleeState);
        stateBehaviors.put(AgentState.ATTACK, this::handleAttackState);
        stateBehaviors.put(AgentState.HELP_ALLY, this::handleHelpAllyState);
    }
    
    @Override
    protected void registerGoals() {
        // Basic goals that apply regardless of state
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 8.0F));
        
        // Register custom state-specific goals
        this.goalSelector.addGoal(1, new AgentGoals.SeekResourceGoal(this, 1.0D, 16.0F));
        this.goalSelector.addGoal(1, new AgentGoals.FleeGoal(this, 1.2D));
        this.goalSelector.addGoal(1, new AgentGoals.HelpAllyGoal(this, 1.0D, 16.0F));
        
        // Combat goals
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0D, true));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
    }
    
    /**
     * Main AI update method
     */
    @Override
    public void aiStep() {
        super.aiStep();
        
        // Update the agent's memory with perceptions
        updatePerceptions();
        
        // Execute the finite state machine logic
        tickAI();
        
        // Increment the ticks in current state
        memory.incrementTicksInState();
    }
    
    /**
     * FSM tick method to handle current state's behavior and transitions
     */
    private void tickAI() {
        // Execute behavior for current state
        Runnable currentBehavior = stateBehaviors.get(currentState);
        if (currentBehavior != null) {
            currentBehavior.run();
        }
        
        // Check for state transitions
        AgentState nextState = determineNextState();
        
        // If state should change, transition to it
        if (nextState != currentState) {
            transitionToState(nextState);
        }
    }
    
    /**
     * Determines what state the agent should transition to based on current perceptions
     */
    private AgentState determineNextState() {
        // Default is to stay in current state
        AgentState nextState = currentState;
        
        switch (currentState) {
            case IDLE:
                if (memory.isDangerNearby() && memory.getFearLevel() > 50) {
                    nextState = AgentState.FLEE;
                } else if (memory.isResourceNearby() && memory.getHungerLevel() > 30) {
                    nextState = AgentState.SEEK_RESOURCE;
                } else if (memory.isAllyNearby() && this.getTarget() == null && random.nextFloat() < 0.05f) {
                    nextState = AgentState.HELP_ALLY;
                } else if (random.nextFloat() < 0.01f) { // 1% chance per tick to start wandering
                    nextState = AgentState.WANDER;
                }
                break;
                
            case WANDER:
                if (memory.isDangerNearby() && memory.getFearLevel() > 30) {
                    nextState = AgentState.FLEE;
                } else if (memory.isResourceNearby() && memory.getHungerLevel() > 20) {
                    nextState = AgentState.SEEK_RESOURCE;
                } else if (this.getTarget() != null) {
                    nextState = AgentState.ATTACK;
                } else if (memory.getTicksInCurrentState() > 200 && random.nextFloat() < 0.1f) {
                    nextState = AgentState.IDLE; // Return to idle after wandering for a while
                }
                break;
                
            case SEEK_RESOURCE:
                if (memory.isDangerNearby() && memory.getFearLevel() > 70) {
                    nextState = AgentState.FLEE;
                } else if (this.getTarget() != null) {
                    nextState = AgentState.ATTACK;
                } else if (!memory.isResourceNearby() || memory.getHungerLevel() < 10) {
                    nextState = AgentState.IDLE;
                }
                break;
                
            case FLEE:
                if (!memory.isDangerNearby() || memory.getFearLevel() < 20) {
                    nextState = AgentState.IDLE;
                }
                break;
                
            case ATTACK:
                if (memory.getHealthPercent() < 30) {
                    nextState = AgentState.FLEE;
                } else if (this.getTarget() == null || !this.getTarget().isAlive()) {
                    nextState = AgentState.IDLE;
                }
                break;
                
            case HELP_ALLY:
                if (memory.isDangerNearby() && memory.getHealthPercent() < 20) {
                    nextState = AgentState.FLEE;
                } else if (this.getTarget() != null) {
                    nextState = AgentState.ATTACK;
                } else if (!memory.isAllyNearby() || memory.getTicksInCurrentState() > 100) {
                    nextState = AgentState.IDLE;
                }
                break;
        }
        
        return nextState;
    }
    
    /**
     * Handles the transition to a new state
     */
    private void transitionToState(AgentState newState) {
        System.out.println(this.getName().getString() + " transitioning from " + currentState + " to " + newState);
        
        // Update state and reset counters
        this.currentState = newState;
        this.memory.resetTicksInState();
        this.memory.updateLastStateChangeTime((int) level().getGameTime());
        
        // Clear any goal-specific targets if needed
        if (newState == AgentState.IDLE) {
            this.memory.clearTargetEntity();
            this.memory.clearTargetLocation();
        }
        
        // Update goals based on new state
        updateGoalsForState(newState);
    }
    
    /**
     * Updates the entity's goals based on its current state
     */
    private void updateGoalsForState(AgentState state) {
        // Clear non-essential goals
        this.goalSelector.removeGoal(new RandomStrollGoal(this, 1.0D));
        
        // Add state-specific goals
        switch (state) {
            case WANDER:
                this.goalSelector.addGoal(1, new RandomStrollGoal(this, 1.0D));
                break;
            case ATTACK:
                // The MeleeAttackGoal is always registered, but we could set a target here if needed
                if (memory.getTargetEntity().isPresent()) {
                    this.setTarget((LivingEntity) memory.getTargetEntity().get());
                }
                break;
            default:
                break;
        }
    }
    
    /**
     * Updates the agent's perceptions by scanning the environment
     */
    private void updatePerceptions() {
        // Reset perception flags
        memory.resetPerceptionFlags();
        
        // Update health awareness
        memory.updateHealthAwareness();
        
        // Scan for entities nearby
        List<LivingEntity> nearbyEntities = level().getEntitiesOfClass(
            LivingEntity.class, 
            this.getBoundingBox().inflate(perceptionRadius),
            entity -> entity != this
        );
        
        // Process nearby entities
        for (LivingEntity entity : nearbyEntities) {
            // Check for danger sources
            if (isEntityDangerous(entity)) {
                memory.setDangerNearby(true);
                adjustFearLevel(entity);
            }
            
            // Check for allies
            if (isEntityAlly(entity)) {
                memory.setAllyNearby(true);
            }
        }
        
        // Use the seek resource goal's detection instead of simulating it
        // This connects to real game mechanics
        AgentGoals.SeekResourceGoal.checkForResources(this, perceptionRadius, memory);
        
        // Simulate hunger increasing over time
        if (random.nextFloat() < 0.01f) {
            memory.setHungerLevel(Math.min(100, memory.getHungerLevel() + 1));
        }
    }
    
    /**
     * Determine if an entity is considered dangerous
     */
    private boolean isEntityDangerous(LivingEntity entity) {
        // Check if entity is hostile mob
        if (entity instanceof Monster) {
            return true;
        }

        // Check if it's a player with weapons
        if (entity instanceof Player player) {
            ItemStack mainHand = player.getMainHandItem();
            ItemStack offHand = player.getOffhandItem();
            return mainHand.isDamageableItem() || offHand.isDamageableItem();
        }

        // Check if entity has attacked us recently
        if (entity == this.getLastHurtByMob() &&
                (level().getGameTime() - this.getLastHurtByMobTimestamp()) < 200) {
            return true;
        }

        return false;
    }
    
    /**
     * Determine if an entity is an ally
     */
    private boolean isEntityAlly(LivingEntity entity) {
        // This is a simple implementation - could be more complex
        return entity.getType() == this.getType();
    }
    
    /**
     * Adjust fear level based on dangerous entity
     */
    private void adjustFearLevel(LivingEntity dangerSource) {
        // Increase fear based on proximity and entity type
        double distance = this.distanceTo(dangerSource);
        int fearIncrease = (int)((perceptionRadius - distance) / perceptionRadius * 20);
        memory.setFearLevel(Math.min(100, memory.getFearLevel() + fearIncrease));
    }
    
    /**
     * State behavior methods
     */
    private void handleIdleState() {
        // In idle state, the agent mostly just observes
        // Gradually reduce fear level while idle
        if (memory.getFearLevel() > 0 && random.nextFloat() < 0.1f) {
            memory.setFearLevel(memory.getFearLevel() - 1);
        }
    }
    
    private void handleWanderState() {
        // Wandering behavior is mostly handled by the RandomStrollGoal
        // But we could add additional logic here
    }
    
    private void handleSeekResourceState() {
        // The actual resource-seeking is handled by the SeekResourceGoal
    }
    
    private void handleFleeState() {
        // The actual fleeing is handled by the FleeGoal
    }
    
    private void handleAttackState() {
        // Most attack logic is handled by built-in goals
    }
    
    private void handleHelpAllyState() {
        // Most help ally logic is handled by the HelpAllyGoal
    }
    
    /**
     * Get the current state of the agent
     */
    public AgentState getCurrentState() {
        return currentState;
    }
    
    /**
     * Get the agent's memory
     */
    public AgentMemory getMemory() {
        return memory;
    }
}