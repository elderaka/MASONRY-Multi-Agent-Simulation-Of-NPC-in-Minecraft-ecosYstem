package com.example.examplemod.entity;

import com.example.examplemod.entity.fsm.AgentState;
import com.example.examplemod.entity.fsm.AgentStateMachine;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

/**
 * EntityAgent is a custom entity that uses a Finite State Machine (FSM) for behavior control.
 * It can switch between different states: Idle, Explore, Collect, and Fight.
 */
public class EntityAgent extends PathfinderMob {
    private final AgentStateMachine stateMachine;
    private static final double DETECTION_RANGE = 16.0;
    private static final double ATTACK_RANGE = 2.0;

    public EntityAgent(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
        this.stateMachine = new AgentStateMachine(this);
        this.stateMachine.setCurrentState(AgentState.IDLE);
    }

    /**
     * Register the entity's attributes (health, movement speed, etc.)
     */
    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.ATTACK_DAMAGE, 3.0D)
                .add(Attributes.FOLLOW_RANGE, DETECTION_RANGE);
    }

    @Override
    protected void registerGoals() {
        // Basic goals that are always active
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(2, new RandomLookAroundGoal(this));
    }

    @Override
    public void tick() {
        super.tick();
        
        // Update the state machine
        stateMachine.update();

        // Check for nearby players
        Player nearestPlayer = level().getNearestPlayer(this, DETECTION_RANGE);
        if (nearestPlayer != null) {
            double distance = distanceTo(nearestPlayer);
            
            if (distance <= ATTACK_RANGE) {
                stateMachine.setCurrentState(AgentState.FIGHT);
            } else if (distance <= DETECTION_RANGE) {
                stateMachine.setCurrentState(AgentState.EXPLORE);
            }
        } else {
            stateMachine.setCurrentState(AgentState.IDLE);
        }
    }

    /**
     * Get the current state of the agent
     */
    public AgentState getCurrentState() {
        return stateMachine.getCurrentState();
    }

    /**
     * Set the current state of the agent
     */
    public void setState(AgentState state) {
        stateMachine.setCurrentState(state);
    }
} 