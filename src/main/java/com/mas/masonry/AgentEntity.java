package com.mas.masonry;

import com.mas.masonry.agent.states.*;
import com.mas.masonry.agent.states.GoToBedStateHandler;
import com.mas.masonry.agent.states.LookForHomeStateHandler;
import com.mas.masonry.agent.states.ReturnHomeStateHandler;
import com.mas.masonry.agent.systems.*;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.npc.InventoryCarrier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.wrapper.InvWrapper;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.level.block.Block;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.block.Blocks; // For default target block
import net.minecraft.world.phys.AABB;

import java.util.*;

/**
 * Base entity class for intelligent agents that use Finite State Machine (FSM)
 * for decision-making and behavior control.
 */
public class AgentEntity extends PathfinderMob implements InventoryCarrier {
    private int nextChatTicks = 0;
    // Change these to:
    private static final int MIN_CHAT_INTERVAL_TICKS = 400;  // 20 seconds
    private static final int MAX_CHAT_INTERVAL_TICKS = 1200; // 60 seconds

    public static final int MIN_TICKS_BETWEEN_PLACEMENT = 20; // 1 second between block placements
    public static final int TICKS_TO_HARVEST_BLOCK = 40;      // 2 seconds to harvest a block (base time)

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return inventoryCapability.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        inventoryCapability.invalidate();
    }

    // reviveCaps might not be strictly necessary if the inventory object doesn't change after construction,
    // but it's good practice to include if there's any scenario where capabilities might be re-initialized.
    // For ItemStackHandler, this is often simple.
    @Override
    public void reviveCaps() {
        super.reviveCaps();
        // If inventoryCapability could become invalid and needs re-creation, do it here.
        // For this setup, LazyOptional.of should handle re-validation if the handler itself is still valid.
    }

    private long lastDamageChatTime = 0;
    private static final long DAMAGE_CHAT_COOLDOWN_TICKS = 60; // Approx 3 seconds (20 ticks/sec)

    private static final int AGENT_CONTAINER_SLOTS = 36; // Number of slots in the agent's main inventory
    private final SimpleContainer inventory = new SimpleContainer(AGENT_CONTAINER_SLOTS);
    private LazyOptional<IItemHandler> inventoryCapability;
    private static final int MAX_TICKS_TO_REACH_BLOCK = 500; // Max ticks to reach block before giving up
    private ResourceRequestSystem resourceRequestSystem;


    // Fields for block targeting and harvesting
    // targetBlockType and targetBlockPos are declared further down
    private int findBlockScanRadius = 8; // How far out to scan for the target block
    private int findBlockAttempts = 0; // Counter for attempts to find the block
    private static final int MAX_FIND_BLOCK_ATTEMPTS = 5; // Max attempts before giving up


    // Add systems
    private GossipSystem gossipSystem;
    private TradingSystem tradingSystem;
    private HomeLocationSystem homeLocationSystem;
    private POIManager poiManager;
    private ScheduleSystem scheduleSystem;
    private ProfessionSystem professionSystem;
    private CoordinationSystem coordinationSystem; // Add this line
    private AdvancedAgentSystem advancedSystem = new AdvancedAgentSystem();

    /**
     * Possible states for the Agent's FSM
     */

    public enum AgentState {
        IDLE,           // Default state, minimal activity
        WANDER,         // Randomly moving around
        SEEK_RESOURCE,  // Looking for specific resources (food, items)
        FLEE,           // Running away from danger
        ATTACK,         // Attacking a target
        HELP_ALLY,      // Moving to assist another agent
        FIND_TARGET_BLOCK, // Searching for a specific type of block
        MOVE_TO_TARGET_BLOCK, // Moving towards a found target block
        HARVEST_BLOCK,    // Breaking and collecting the target block
        PLACE_CONSTRUCTION_BLOCK, // Placing a block for construction

        // New states for social interaction & communication
        CHAT_WITH_AGENT,
        SHARE_RESOURCE_LOCATION,
        REQUEST_ITEM_FROM_AGENT,
        GIVE_ITEM_TO_AGENT,
        FOLLOW_AGENT,
        GREET_AGENT,
        WARN_AGENT_OF_DANGER,

        // New states for work & task management
        LOOK_FOR_TASK,
        TRAVEL_TO_TASK_LOCATION,
        PERFORM_TASK,
        RETURN_TO_BASE,
        DEPOSIT_RESOURCES,
        RETRIEVE_ITEM,
        CRAFT_ITEM,

        // New states for needs & reactions
        SEEK_SHELTER,
        SEEK_REST,
        SEEK_HEALING_ITEM,
        REACT_TO_WEATHER,
        REACT_TO_TIME_OF_DAY,

        // Phase 1: Home and POI states
        RETURN_HOME,
        GO_TO_BED,
        LOOK_FOR_HOME,
        VISIT_POI,

        GO_TO_WORK,
        WORK_AT_JOB_SITE,
        TAKE_WORK_BREAK,
        GO_TO_MEETING,
        SOCIALIZE_AT_MEETING,
        LOOK_FOR_JOB,
        WAKE_UP_ROUTINE,

        // Phase 2.5: Resource sharing states
        WAIT_FOR_RESOURCE_DELIVERY,
        CONSIDER_RESOURCE_REQUEST,
        DELIVER_RESOURCE_TO_AGENT,
        SHARE_GOSSIP,
        LISTEN_TO_GOSSIP,
        PROPOSE_TRADE,
        CONSIDER_TRADE_OFFER,
        EXECUTE_TRADE,

        // Phase 4: Advanced coordination states
        LEAD_PROJECT,
        PARTICIPATE_IN_PROJECT,
        TRADING,
        GATHERING,
        MINING,
        FARMING,
        BUILDING
    }
    /**
     * Agent's memory class to store information about the environment and internal state
     */
    public class AgentMemory {
        // Internal metrics
        private int hungerLevel = 0;        // 0-100, 0 = full, 100 = starving
        private int healthPercent = 100;    // 0-100 percentage of max health
        private int fearLevel = 0;          // 0-100, higher means more likely to flee
        private int socialMeter = AgentEntity.this.random.nextInt(51) + 25; // 0-100, higher means more sociable. Random 25-75.
        private int apathyLevel = AgentEntity.this.random.nextInt(31); // 0-30, higher means less likely to help others



        private int stateTransitionCooldown = 0;
        private static final int MIN_STATE_DURATION = 100; // 5 seconds minimum per state
        private AgentState lastState = AgentState.IDLE;



        // Communication memory
        private Map<UUID, Integer> recentCommunications = new HashMap<>();
        private static final int GREETING_COOLDOWN_TICKS = 6000; // 5 minutes (at 20 ticks/sec)
        private static final int WARNING_COOLDOWN_TICKS = 1200;  // 1 minute
        private int socialInteractionCooldown = 0;
        private static final int MIN_SOCIAL_COOLDOWN = 600; // 30 seconds
        private static final int MAX_SOCIAL_COOLDOWN = 1800; // 90 seconds
        public boolean isReadyToSocialize() {
            return socialInteractionCooldown <= 0;
        }
        /**
         * Resets the social interaction cooldown to a random time between MIN and MAX cooldown
         */
        public void resetSocialCooldown() {
            this.socialInteractionCooldown = AgentEntity.this.random.nextInt(
                    MAX_SOCIAL_COOLDOWN - MIN_SOCIAL_COOLDOWN + 1) + MIN_SOCIAL_COOLDOWN;
            MASONRY.LOGGER.debug("{} reset social cooldown to {} ticks ({}s)",
                    AgentEntity.this.getBaseName(),
                    socialInteractionCooldown,
                    socialInteractionCooldown/20.0);
        }






        // Known danger locations shared by other agents
        private List<DangerInfo> knownDangers = new ArrayList<>();



        // Entity/environment awareness
        private boolean dangerNearby = false;
        private boolean resourceNearby = false;
        private boolean allyNearby = false;


        public boolean canChangeState() {
            return stateTransitionCooldown <= 0;
        }


        public void resetStateTransitionCooldown() {
            this.stateTransitionCooldown = MIN_STATE_DURATION;
        }

        public void updateStateTransitionCooldown() {
            if (stateTransitionCooldown > 0) {
                stateTransitionCooldown--;
            }
        }

        public void updateTimers() {
            updateCommunicationTimers();
            updateStateTransitionCooldown(); // Add this line
            if (socialInteractionCooldown > 0) {
                socialInteractionCooldown--;
            }
        }




        // Target tracking
        private Optional<LivingEntity> targetEntity = Optional.empty();
        private Optional<LivingEntity> attackTarget = Optional.empty();
        private Optional<Vec3> targetLocation = Optional.empty();
        // Removed redundant clearTargetLocation field
        public void clearTargetLocation() { this.targetLocation = Optional.empty(); } // Now clears the main targetLocation

        // Additional memory for state persistence
        private int ticksInCurrentState = 0;
        private int lastStateChangeTime = 0;
        private int ticksSinceLastBlockPlace = MIN_TICKS_BETWEEN_PLACEMENT; // Initialize to allow immediate placement first time
        
        // Getters/setters
        public int getHungerLevel() { return hungerLevel; }
        public void setHungerLevel(int hungerLevel) { this.hungerLevel = hungerLevel; }
        
        public int getHealthPercent() { return healthPercent; }
        public void setHealthPercent(int healthPercent) { this.healthPercent = healthPercent; }
        
        public int getFearLevel() { return fearLevel; }
        public void setFearLevel(int fearLevel) { this.fearLevel = fearLevel; }
        
        public int getSocialMeter() { return socialMeter; }
        public void setSocialMeter(int socialMeter) {
            this.socialMeter = Math.max(0, Math.min(100, socialMeter)); // Clamp between 0 and 100
        }
        public int getApathyLevel() { return apathyLevel; }
        public void setApathyLevel(int apathyLevel) { this.apathyLevel = Math.max(0, Math.min(100, apathyLevel)); }
        public void increaseApathyLevel(int amount) { setApathyLevel(apathyLevel + amount); }
        public void decreaseApathyLevel(int amount) { setApathyLevel(apathyLevel - amount); }

        public void increaseSocialMeter(int amount) {
            setSocialMeter(this.socialMeter + amount);
        }
        public void decreaseSocialMeter(int amount) {
            setSocialMeter(this.socialMeter - amount);
        }
        
        public boolean isDangerNearby() { return dangerNearby; }
        public void setDangerNearby(boolean dangerNearby) { this.dangerNearby = dangerNearby; }
        
        public boolean isResourceNearby() { return resourceNearby; }
        public void setResourceNearby(boolean resourceNearby) { this.resourceNearby = resourceNearby; }
        
        public boolean isAllyNearby() { return allyNearby; }
        public void setAllyNearby(boolean allyNearby) { this.allyNearby = allyNearby; }
        
        public Optional<LivingEntity> getTargetEntity() { return targetEntity; }

        public void setTargetEntity(LivingEntity entity) {
            // Prevent setting self as target
            if (entity == AgentEntity.this) {
                MASONRY.LOGGER.debug("{} attempted to set itself as target, ignoring", AgentEntity.this.getBaseName());
                return;
            }
            this.targetEntity = Optional.ofNullable(entity);
        }
        public void clearTargetEntity() { this.targetEntity = Optional.empty(); }

        public Optional<LivingEntity> getAttackTarget() { return attackTarget; }
        public void setAttackTarget(LivingEntity target) { this.attackTarget = Optional.ofNullable(target); }
        
        public Optional<Vec3> getTargetLocation() { return targetLocation; }
        public void setTargetLocation(Vec3 location) { this.targetLocation = Optional.ofNullable(location); }

        public int getTicksInCurrentState() { return ticksInCurrentState; }
        public void incrementTicksInState() { this.ticksInCurrentState++; }
        public void resetTicksInState() { this.ticksInCurrentState = 0; }

        public int getTicksSinceLastBlockPlace() { return ticksSinceLastBlockPlace; }
        public void resetTicksSinceLastBlockPlace() { this.ticksSinceLastBlockPlace = 0; }
        public void incrementTicksSinceLastBlockPlace() { 
            if (this.ticksSinceLastBlockPlace < MIN_TICKS_BETWEEN_PLACEMENT) { 
                this.ticksSinceLastBlockPlace++; 
            }
        }

        public LivingEntity getNearestAlly() {
            double closestDistSqr = Double.MAX_VALUE;
            LivingEntity closestAlly = null;

            for (LivingEntity entity : AgentEntity.this.level().getEntitiesOfClass(
                    LivingEntity.class,
                    AgentEntity.this.getBoundingBox().inflate(AgentEntity.this.perceptionRadius),
                    e -> AgentEntity.this.isEntityAlly(e))) {

                double distSqr = AgentEntity.this.distanceToSqr(entity);
                if (distSqr < closestDistSqr) {
                    closestDistSqr = distSqr;
                    closestAlly = entity;
                }
            }

            return closestAlly;
        }


        public void updateLastStateChangeTime(int gameTime) { this.lastStateChangeTime = gameTime; }
        public int getLastStateChangeTime() { return lastStateChangeTime; }
        
        /**
         * Finds and returns the nearest dangerous entity within perception radius.
         * @return The nearest dangerous LivingEntity, or null if none found.
         */
        public LivingEntity getNearestDanger() {
            // Get all entities within perception radius
            AABB searchArea = new AABB(
                AgentEntity.this.getX() - AgentEntity.this.perceptionRadius, 
                AgentEntity.this.getY() - AgentEntity.this.perceptionRadius, 
                AgentEntity.this.getZ() - AgentEntity.this.perceptionRadius,
                AgentEntity.this.getX() + AgentEntity.this.perceptionRadius, 
                AgentEntity.this.getY() + AgentEntity.this.perceptionRadius, 
                AgentEntity.this.getZ() + AgentEntity.this.perceptionRadius
            );
            
            List<LivingEntity> nearbyEntities = AgentEntity.this.level().getEntitiesOfClass(
                LivingEntity.class, 
                searchArea,
                entity -> entity != AgentEntity.this && AgentEntity.this.isEntityDangerous(entity)
            );
            
            // Find the closest dangerous entity
            LivingEntity nearestDanger = null;
            double closestDistanceSq = Double.MAX_VALUE;
            
            for (LivingEntity entity : nearbyEntities) {
                double distanceSq = AgentEntity.this.distanceToSqr(entity);
                if (distanceSq < closestDistanceSq) {
                    nearestDanger = entity;
                    closestDistanceSq = distanceSq;
                }
            }
            
            return nearestDanger;
        }

        
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

        // Update the AgentMemory class to fix the recordCommunication method

        /**
         * Records that this agent has communicated with another agent
         * @param otherAgent The UUID of the agent communicated with
         * @param communicationType Type of communication (affects cooldown period)
         */
        public void recordCommunication(UUID otherAgent, CommunicationType communicationType) {
            int cooldownTicks = switch (communicationType) {
                case GREETING -> GREETING_COOLDOWN_TICKS;
                case WARNING -> WARNING_COOLDOWN_TICKS;
                case DANGER_WARNING -> WARNING_COOLDOWN_TICKS;
                case RESOURCE_SHARING, TRADE_PROPOSAL, GOSSIP_SHARING -> 2400; // 2 minutes
                case GENERAL_CHAT -> 1200; // 1 minute
            };
            recentCommunications.put(otherAgent, AgentEntity.this.tickCount + cooldownTicks);
        }

        /**
         * Overloaded method for backward compatibility
         */
        public void recordCommunication(AgentEntity otherAgent, CommunicationType communicationType) {
            recordCommunication(otherAgent.getUUID(), communicationType);
        }

        /**
         * Simple version for backward compatibility
         */
        public void recordCommunication(AgentEntity otherAgent) {
            recordCommunication(otherAgent.getUUID(), CommunicationType.GENERAL_CHAT);
        }

        /**
         * Checks if this agent has recently communicated with another agent
         * @param entity The entity to check
         * @return true if communication is on cooldown, false otherwise
         */
        public boolean hasRecentlyCommunicated(LivingEntity entity) {
            if (!(entity instanceof AgentEntity)) return false;

            UUID entityId = entity.getUUID();
            return recentCommunications.containsKey(entityId) &&
                    recentCommunications.get(entityId) > 0;
        }

        /**
         * Records information about a danger
         * @param dangerType Type of entity that is dangerous
         * @param position Position of the danger
         */
        public void recordDanger(EntityType<?> dangerType, Vec3 position) {
            knownDangers.add(new DangerInfo(dangerType, position, WARNING_COOLDOWN_TICKS));
        }

        /**
         * Checks if a warning has been given about a specific danger
         * @param dangerEntity The dangerous entity
         * @return true if already warned about this danger, false otherwise
         */
        public boolean hasWarnedAboutDanger(LivingEntity dangerEntity) {
            // If there's no danger entity, there's nothing to warn about
            if (dangerEntity == null) {
                return false;
            }

            // Consider a danger warned about if it's within 10 blocks of a known danger of the same type
            return knownDangers.stream()
                    .anyMatch(danger ->
                            danger.entityType == dangerEntity.getType() &&
                                    danger.position.distanceToSqr(dangerEntity.position()) < 100 && // 10 blocks squared
                                    danger.cooldownTicks > 0);
        }

        /**
         * Updates communication cooldowns
         */
        public void updateCommunicationTimers() {
            // Update recent communications cooldowns
            recentCommunications.entrySet().removeIf(entry -> {
                int updatedValue = entry.getValue() - 1;
                if (updatedValue <= 0) {
                    return true; // Remove expired communications
                } else {
                    entry.setValue(updatedValue);
                    return false;
                }
            });

            // Update danger info cooldowns
            knownDangers.removeIf(danger -> {
                danger.cooldownTicks--;
                return danger.cooldownTicks <= 0;
            });
        }



        /**
         * Info about a known danger
         */
        private static class DangerInfo {
            private final EntityType<?> entityType;
            private final Vec3 position;
            private int cooldownTicks;

            public DangerInfo(EntityType<?> entityType, Vec3 position, int cooldownTicks) {
                this.entityType = entityType;
                this.position = position;
                this.cooldownTicks = cooldownTicks;
            }
        }
    }
    
    // Current state in the FSM
    private AgentState currentState = AgentState.IDLE;
    
    // Agent's memory
    private final AgentMemory memory;

    // Target block for resource gathering
    private Block targetBlockType; 
    private BlockPos targetBlockPos;

    // Construction related fields
    private BlockPos constructionOrigin = null; // Starting point for construction blueprints
    private int currentBlueprintIndex = 0;    // Current step in the active blueprint
    private Vec3 targetPos = null; // For general movement targets, and construction site sub-targets
    
    // Map of behaviors for each state
    private EnumMap<AgentState, IAgentStateHandler> stateBehaviors;
    
    // Perception radius 
    private final double perceptionRadius = 16.0;
    private double attackRange = 2.0D;
    private Random random; // Added for random behaviors

    /**
     * Set the display text above the agent
     * @param text The text to display
     */
    public void setDisplayText(Component text) {
        this.setCustomName(text);
    }


    /**
     * Constructor for the agent entity
     */
    public AgentEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
        this.random = new Random();
        this.memory = new AgentMemory();

        // Set up inventory capability
        this.inventoryCapability = LazyOptional.of(() -> new ItemStackHandler(AGENT_CONTAINER_SLOTS) {
            @Override
            public boolean isItemValid(int slot, @NotNull ItemStack stack) {
                return true;
            }
        });

        // Initialize the stateBehaviors map before calling initializeStateBehaviors
        this.stateBehaviors = new EnumMap<>(AgentState.class);
        initializeStateBehaviors();

        this.gossipSystem = new GossipSystem();
        this.tradingSystem = new TradingSystem();
        this.homeLocationSystem = new HomeLocationSystem();
        this.poiManager = new POIManager();
        this.scheduleSystem = new ScheduleSystem();
        this.professionSystem = new ProfessionSystem();
        this.resourceRequestSystem = new ResourceRequestSystem();
        this.coordinationSystem = new CoordinationSystem();




        // Initialize name and state display
        this.setCustomName(Component.literal(MASONRY.getRandomAgentName()).withStyle(ChatFormatting.AQUA));

        resetChatTimer();
        initializeStateBehaviors();

    }
    private void resetChatTimer() {
        this.nextChatTicks = MIN_CHAT_INTERVAL_TICKS +
                random.nextInt(MAX_CHAT_INTERVAL_TICKS - MIN_CHAT_INTERVAL_TICKS);
        this.nextChatTicks = this.random.nextInt(MAX_CHAT_INTERVAL_TICKS - MIN_CHAT_INTERVAL_TICKS + 1) + MIN_CHAT_INTERVAL_TICKS;

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

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder pBuilder) {
        super.defineSynchedData(pBuilder);
        // Add any synched data needed for inventory, e.g.:
        // pBuilder.define(YOUR_DATA_SERIALIZER, defaultValue);
    }

    @Override
    public SimpleContainer getInventory() {
        return this.inventory;
    }

    public AgentMemory getMemory() {
        return memory;
    }

    /**
     * Checks if the agent has any type of pickaxe in its inventory.
     * @return true if a pickaxe is found, false otherwise.
     */
    public boolean hasPickaxe() {
        for (int i = 0; i < this.inventory.getContainerSize(); ++i) {
            ItemStack itemStack = this.inventory.getItem(i);
            if (!itemStack.isEmpty()) {
                Item item = itemStack.getItem();
                if (item == Items.WOODEN_PICKAXE ||
                    item == Items.STONE_PICKAXE ||
                    item == Items.IRON_PICKAXE ||
                    item == Items.GOLDEN_PICKAXE ||
                    item == Items.DIAMOND_PICKAXE ||
                    item == Items.NETHERITE_PICKAXE) {
                    return true; // Found a pickaxe
                }
            }
        }
        return false; // No pickaxe found
    }

    public double getAttackRangeSqr() {
        return this.attackRange * this.attackRange;
    }

    public AgentState getCurrentState() { return currentState; }

    // --- Block Targeting Getters/Setters ---
    public Block getTargetBlockTypeToFind() { return this.targetBlockType; }
    public void setTargetBlockTypeToFind(Block blockType) { this.targetBlockType = blockType; }
    public BlockPos getTargetBlockPos() { return this.targetBlockPos; }
    public void setTargetBlockPos(BlockPos pos) { this.targetBlockPos = pos; }
    public int getFindBlockScanRadius() { return this.findBlockScanRadius; }
    public int getFindBlockAttempts() { return this.findBlockAttempts; }
    public void incrementFindBlockAttempts() { this.findBlockAttempts++; }
    public void resetFindBlockAttempts() { this.findBlockAttempts = 0; }
    public int getMaxFindBlockAttempts() { return MAX_FIND_BLOCK_ATTEMPTS; }
    public Vec3 getTargetPos() { return this.targetPos; }
    public void setTargetPos(Vec3 pos) { this.targetPos = pos; }
    public int getMaxTicksToReachBlock() { return MAX_TICKS_TO_REACH_BLOCK; }

    // --- Construction Getters/Setters ---
    public BlockPos getConstructionOrigin() { return this.constructionOrigin; }
    public void setConstructionOrigin(BlockPos origin) { this.constructionOrigin = origin; }
    public int getCurrentBlueprintIndex() { return this.currentBlueprintIndex; }
    public void setCurrentBlueprintIndex(int index) { this.currentBlueprintIndex = index; }
    public void incrementCurrentBlueprintIndex() { this.currentBlueprintIndex++; }
    // --- End Construction Getters/Setters ---
    // --- End Block Targeting Getters/Setters ---
    private int lastStateChangeTime = 0;
    private static final int MIN_STATE_CHANGE_INTERVAL = 20; // 1 second minimum between state changes

    public void setCurrentState(AgentState newState) {
        if (newState == this.currentState) {
            return; // Don't change to same state
        }

        // Prevent rapid state changes (except for emergency states like FLEE)
        int currentTime = this.tickCount;
        if (newState != AgentState.FLEE &&
                currentTime - lastStateChangeTime < MIN_STATE_CHANGE_INTERVAL) {
            return; // Too soon to change state
        }

        AgentState oldState = this.currentState;
        this.currentState = newState;
        this.lastStateChangeTime = currentTime;


        // Log state change
        MASONRY.LOGGER.info("{} transitioning from {} to {}", getBaseName(), oldState, newState);

        // Send chat message about state change (with cooldown)
        sendStateChangeMessage(newState);
    }



    private void sendStateChangeMessage(AgentState state) {
        if (this.level().isClientSide || nextChatTicks >= 0) {
            return;
        }

        // Use AgentChatter for state-based messages
        Component agentNameComponent = Component.literal(getBaseName());
        Component formattedMessage = AgentChatter.getFormattedChatMessage(state, agentNameComponent);

        if (formattedMessage != null) {
            sendChatMessage(formattedMessage);
            resetChatTimer();
        }
    }

    public ScheduleSystem getScheduleSystem() {
        return scheduleSystem;
    }

    public ProfessionSystem getProfessionSystem() {
        return professionSystem;
    }
    public ResourceRequestSystem getResourceRequestSystem() {
        return resourceRequestSystem;
    }


    @Override
    public SlotAccess getSlot(int slotIndex) {
        // For commands like /item replace ... container.<slotIndex>
        // slotIndex will directly be the index for the SimpleContainer.
        if (slotIndex >= 0 && slotIndex < this.inventory.getContainerSize()) {
            return SlotAccess.forContainer(this.inventory, slotIndex);
        }
        // For equipment slots (e.g., weapon.mainhand, armor.chest), LivingEntity.getSlot handles them.
        return super.getSlot(slotIndex);
    }

    // Save inventory with the entity
    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        ListTag listTag = new ListTag();
        for(int i = 0; i < this.inventory.getContainerSize(); ++i) {
            ItemStack itemstack = this.inventory.getItem(i);
            if (!itemstack.isEmpty()) {
                CompoundTag itemTag = new CompoundTag();
                itemTag.putByte("Slot", (byte)i);
                itemstack.save(this.level().registryAccess(), itemTag);
                listTag.add(itemTag);
            }
        }
        compound.put("Inventory", listTag);
    }

    /**
     * Generates a descriptive text for the current state to display below the agent's name
     * @return A Component with state description text
     */
    private Component getStateDescriptionText() {
        String scheduleDesc = scheduleSystem.getTimeOfDayDescription(level());
        String activityDesc = scheduleSystem.getCurrentActivity().toString().toLowerCase().replace('_', ' ');

        return switch (currentState) {
            case GO_TO_WORK -> Component.literal("Going to work (" + scheduleDesc + ")");
            case WORK_AT_JOB_SITE -> Component.literal("Working (" + activityDesc + ")");
            case TAKE_WORK_BREAK -> Component.literal("Taking a break (" + scheduleDesc + ")");
            case GO_TO_MEETING -> Component.literal("Going to meeting (" + scheduleDesc + ")");
            case LOOK_FOR_JOB -> Component.literal("Looking for work");
            case GO_TO_BED -> Component.literal("Going to bed (" + scheduleDesc + ")");
            default -> Component.literal(currentState.toString().toLowerCase().replace('_', ' '));
        };
    }

    /**
     * Updates the agent's display name to include state information
     */
    /**
     * Updates the agent's display name to include state information
     */
    private void updateDisplayName() {
        String baseName = getBaseName();
        String professionName = "";

        if (professionSystem.hasProfession()) {
            professionName = " (" + professionSystem.getProfessionDisplayName() + ")";
        }

        Component stateDesc = getStateDescriptionText();
        String fullName = baseName + professionName;

        // Don't duplicate profession names - create clean name each time
        this.setCustomName(Component.literal(fullName));
        this.setCustomNameVisible(true);
    }



    // Load inventory with the entity
    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        if (compound.contains("Inventory", 9)) { // 9 for TAG_LIST
            ListTag listTag = compound.getList("Inventory", 10); // 10 for TAG_COMPOUND
            for (int i = 0; i < listTag.size(); ++i) {
                CompoundTag itemTag = listTag.getCompound(i);
                int slot = itemTag.getByte("Slot") & 255; // Ensure positive index
                if (slot >= 0 && slot < this.inventory.getContainerSize()) {
                    ItemStack stack = ItemStack.parseOptional(this.level().registryAccess(), itemTag);
                    this.inventory.setItem(slot, stack);
                }
            }
        }

        // Load agent state
        if (compound.contains("AgentState", Tag.TAG_STRING)) {
            try {
                this.currentState = AgentState.valueOf(compound.getString("AgentState"));
            } catch (IllegalArgumentException e) {
                this.currentState = AgentState.IDLE;
            }
        }

        updateDisplayName();


    }
    @Override
    protected void dropCustomDeathLoot(DamageSource pSource, int pLooting, boolean pRecentlyHit) {
        super.dropCustomDeathLoot(pSource, pLooting, pRecentlyHit);
        // Drop all items from the SimpleContainer inventory
        for (int i = 0; i < this.inventory.getContainerSize(); ++i) {
            ItemStack itemstack = this.inventory.getItem(i);
            if (!itemstack.isEmpty()) {
                this.spawnAtLocation(itemstack);
                this.inventory.setItem(i, ItemStack.EMPTY); // Clear the slot after dropping
            }
        }
    }

    /**
     * Sets up default behaviors for each state
     */
    private void initializeStateBehaviors() {
        stateBehaviors = new EnumMap<>(AgentState.class);

        stateBehaviors.put(AgentState.IDLE, new IdleStateHandler());
        stateBehaviors.put(AgentState.WANDER, new WanderStateHandler());
        stateBehaviors.put(AgentState.SEEK_RESOURCE, new SeekResourceStateHandler());
        stateBehaviors.put(AgentState.FLEE, new FleeStateHandler());
        stateBehaviors.put(AgentState.ATTACK, new AttackStateHandler());
        stateBehaviors.put(AgentState.FIND_TARGET_BLOCK, new FindTargetBlockStateHandler());
        stateBehaviors.put(AgentState.MOVE_TO_TARGET_BLOCK, new MoveToTargetBlockStateHandler());
        stateBehaviors.put(AgentState.HARVEST_BLOCK, new HarvestBlockStateHandler());
        stateBehaviors.put(AgentState.PLACE_CONSTRUCTION_BLOCK, new PlaceConstructionBlockStateHandler());

        // New states for social interaction & communication
        stateBehaviors.put(AgentState.GREET_AGENT, new GreetAgentStateHandler());
        stateBehaviors.put(AgentState.WARN_AGENT_OF_DANGER, new WarnAgentOfDangerStateHandler());
        stateBehaviors.put(AgentState.HELP_ALLY, new HelpAllyStateHandler());
        stateBehaviors.put(AgentState.CHAT_WITH_AGENT, new ChatWithAgentStateHandler());

        stateBehaviors.put(AgentState.RETURN_HOME, new ReturnHomeStateHandler());
        stateBehaviors.put(AgentState.LOOK_FOR_HOME, new LookForHomeStateHandler());
        stateBehaviors.put(AgentState.GO_TO_BED, new GoToBedStateHandler());

        stateBehaviors.put(AgentState.GO_TO_WORK, new GoToWorkStateHandler());
        stateBehaviors.put(AgentState.WORK_AT_JOB_SITE, new WorkAtJobSiteStateHandler());
        stateBehaviors.put(AgentState.TAKE_WORK_BREAK, new TakeWorkBreakStateHandler());
        stateBehaviors.put(AgentState.GO_TO_MEETING, new GoToMeetingStateHandler());
        stateBehaviors.put(AgentState.LOOK_FOR_JOB, new LookForJobStateHandler());
        stateBehaviors.put(AgentState.WAIT_FOR_RESOURCE_DELIVERY, new WaitForResourceDeliveryStateHandler());
        stateBehaviors.put(AgentState.SHARE_GOSSIP, new ShareGossipStateHandler());
        stateBehaviors.put(AgentState.PROPOSE_TRADE, new ProposeTradeStateHandler());
        stateBehaviors.put(AgentState.CONSIDER_TRADE_OFFER, new ConsiderTradeOfferStateHandler());



    }
    
    @Override
    protected void registerGoals() {
        // Basic goals that apply regardless of state
        this.goalSelector.addGoal(0, new FloatGoal(this)); // Highest priority to prevent drowning/suffocating
        this.goalSelector.addGoal(1, new AgentGoals.FleeGoal(this, 1.2D)); // High priority for escaping danger
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0D, true)); // Combat
        this.goalSelector.addGoal(3, new AgentGoals.HelpAllyGoal(this, 1.0D, 16.0F)); // Helping allies
        this.goalSelector.addGoal(4, new AgentGoals.SeekResourceGoal(this, 1.0D, 16.0F)); // Seeking resources
        // Example: Add a WanderGoal if no other specific action is taken, with a lower priority
        this.goalSelector.addGoal(7, new RandomStrollGoal(this, 1.0D)); // Default wandering
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F)); // Low priority
        
        // Target selectors (these run independently to decide who/what to target)
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers()); // Standard target selector
        // Example: Add a NearestAttackableTargetGoal for specific entity types if needed
        // this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
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

        if (!level().isClientSide) {
            // Update Phase 1 systems
            homeLocationSystem.tick(this);
            poiManager.tick(level());

            // Update Phase 2 systems
            professionSystem.tick(this);

            // Update memory and other systems
            memory.updateTimers();
            memory.updateCommunicationTimers();
            resourceRequestSystem.tick(this);
            gossipSystem.tick(this);
            tradingSystem.tick(this);


        }


        memory.incrementTicksInState();
    }
    
    /**
     * FSM tick method to handle current state's behavior and transitions
     */
    private void tickAI() {

        IAgentStateHandler handler = stateBehaviors.get(currentState);
        if (handler != null) {
            handler.handle(this);
        } else {
            MASONRY.LOGGER.warn("No state handler found for state: {}", currentState);
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
        // Update current schedule activity
        ScheduleSystem.AgentActivity currentActivity = scheduleSystem.determineActivity(level(), this);

        // Check for danger first (highest priority)
        if (!memory.isDangerNearby()) {
            List<ResourceRequestSystem.PendingRequest> pendingRequests =
                    resourceRequestSystem.getPendingRequestsForAgent(getUUID());

            if (!pendingRequests.isEmpty()) {
                return AgentState.CONSIDER_RESOURCE_REQUEST;
            }

            // Check for accepted requests to deliver
            List<ResourceRequestSystem.PendingRequest> acceptedRequests =
                    resourceRequestSystem.getAcceptedRequestsForAgent(getUUID());

            if (!acceptedRequests.isEmpty()) {
                return AgentState.GIVE_ITEM_TO_AGENT;
            }
        }

        if (!memory.isDangerNearby() && memory.isReadyToSocialize()) {

            // Check for pending trade offers (high priority)
            if (tradingSystem.getActiveOffers().stream()
                    .anyMatch(offer -> !offer.offeredBy.equals(getUUID()))) {
                if (getRandom().nextInt(4) == 0) { // 25% chance to consider trades
                    return AgentState.CONSIDER_TRADE_OFFER;
                }
            }

            // Gossip sharing (medium priority)
            if (gossipSystem.hasInterestingGossip() && getRandom().nextInt(8) == 0) { // 12.5% chance
                return AgentState.SHARE_GOSSIP;
            }

            // Propose trades (lower priority)
            if (!tradingSystem.hasActiveOffers(getUUID()) && getRandom().nextInt(15) == 0) { // ~6.7% chance
                return AgentState.PROPOSE_TRADE;
            }
        }



        // Handle schedule-based state transitions
        switch (currentActivity) {
            case SLEEP -> {
                if (homeLocationSystem.hasHome()) {
                    return AgentState.GO_TO_BED;
                } else {
                    return AgentState.LOOK_FOR_HOME;
                }
            }
            case WAKE_UP -> {
                // Brief wake-up routine
                return AgentState.WAKE_UP_ROUTINE;
            }
            case WORK -> {
                if (professionSystem.hasProfession()) {
                    if (poiManager.hasJobSite()) {
                        // Check if already at work
                        BlockPos jobSite = poiManager.getJobSite();
                        if (jobSite != null && blockPosition().closerThan(jobSite, 4.0)) {
                            return AgentState.WORK_AT_JOB_SITE;
                        } else {
                            return AgentState.GO_TO_WORK;
                        }
                    } else {
                        return AgentState.LOOK_FOR_JOB;
                    }
                } else {
                    return AgentState.LOOK_FOR_JOB;
                }
            }
            case SOCIALIZE -> {
                if (poiManager.hasMeetingPoint()) {
                    return AgentState.GO_TO_MEETING;
                } else {
                    // Look for nearby agents to chat with
                    if (memory.isAllyNearby() && memory.getSocialMeter() > 50) {
                        return AgentState.CHAT_WITH_AGENT;
                    } else {
                        return AgentState.GO_TO_MEETING; // Will search for meeting point
                    }
                }
            }
            case BREAK -> {
                return AgentState.TAKE_WORK_BREAK;
            }
            case PANIC -> {
                return AgentState.FLEE;
            }
        }

        // Phase 1: Check if should return home (if not handled by schedule)
        if (homeLocationSystem.shouldReturnHome(this)) {
            return AgentState.RETURN_HOME;
        }

        // Check for allies that need help (lower priority than schedule)
        if (memory.getApathyLevel() < 20 && memory.getSocialMeter() > 40) {
            LivingEntity allyNeedingHelp = findNearestAllyNeedingHelp();
            if (allyNeedingHelp != null) {
                memory.setTargetEntity(allyNeedingHelp);
                return AgentState.HELP_ALLY;
            }
        }

        // Social interactions if ready and not in scheduled activity
        if (memory.isReadyToSocialize() && memory.getSocialMeter() > 50) {
            LivingEntity nearbyAgent = memory.getNearestAlly();
            if (nearbyAgent instanceof AgentEntity) {
                if (!memory.hasRecentlyCommunicated(nearbyAgent)) {
                    memory.setTargetEntity(nearbyAgent);
                    return AgentState.GREET_AGENT;
                }
            }
        }

        // Default behaviors
        if (memory.isResourceNearby()) {
            return AgentState.SEEK_RESOURCE;
        }

        // Random wander occasionally
        if (random.nextInt(300) == 0) {
            return AgentState.WANDER;
        }

        return AgentState.IDLE;
    }

    // Add these methods to AgentEntity class

    /**
     * Gets the current blueprint for construction
     */
    public List<BlockPlacement> getCurrentBlueprint() {
        // This should return the current construction blueprint
        // For now, return empty list to fix compilation
        return new ArrayList<>();
    }

    /**
     * Communication type enum for gossip system
     */
    public enum CommunicationType {
        GREETING,
        WARNING,
        DANGER_WARNING,  // Add this
        RESOURCE_SHARING,
        TRADE_PROPOSAL,
        GOSSIP_SHARING,
        GENERAL_CHAT
    }


    /**
     * Block placement data structure for blueprints
     */
    public static class BlockPlacement {
        public final Block blockType;
        public final BlockPos relativePos;

        public BlockPlacement(Block blockType, BlockPos relativePos) {
            this.blockType = blockType;
            this.relativePos = relativePos;
        }
    }

    public HomeLocationSystem getHomeLocationSystem() {
        return homeLocationSystem;
    }

    public CoordinationSystem getCoordinationSystem() {
        return coordinationSystem;
    }

    public POIManager getPoiManager() {
        return poiManager;
    }

    public GossipSystem getGossipSystem() {
        return gossipSystem;
    }

    public TradingSystem getTradingSystem() {
        return tradingSystem;
    }
    public AdvancedAgentSystem getAdvancedSystem() {
        return advancedSystem;
    }



    public void createResourceGossip(Item item, BlockPos location) {
        gossipSystem.createResourceGossip(this, item, location);
    }

    public void createDangerGossip(String danger, BlockPos location) {
        gossipSystem.createDangerGossip(this, danger, location);
    }

    public void createReputationGossip(AgentEntity subject, boolean positive, String reason) {
        gossipSystem.createReputationGossip(this, subject, positive, reason);
    }



    /**
     * Directly forces the agent into a new state, bypassing normal FSM logic.
     * Used for external commands like the Task Paper.
     * @param newState The state to transition to.
     */
    public void forceState(AgentState newState) {
        MASONRY.LOGGER.info("{} is being forced from state {} to state {} by an external command.", 
            this.getName().getString(), this.currentState, newState);
        transitionToState(newState);
    }
    /**
     * Gets the base name of the agent without any state information
     */
    public String getBaseName() {
        String customName = this.getCustomName() != null ? this.getCustomName().getString() : "Agent";

        // Extract base name by removing profession suffixes
        if (customName.contains("(")) {
            return customName.substring(0, customName.indexOf("(")).trim();
        }

        return customName;
    }

    /**
     * Finds the nearest ally that might need help
     */
    private LivingEntity findNearestAllyNeedingHelp() {
        double closestDistSqr = Double.MAX_VALUE;
        LivingEntity closestAlly = null;

        for (LivingEntity entity : this.level().getEntitiesOfClass(
                LivingEntity.class,
                this.getBoundingBox().inflate(this.getAttributeValue(Attributes.FOLLOW_RANGE)),
                e -> e instanceof AgentEntity && e != this)) {

            // Check if this ally needs help
            boolean needsHelp = false;
            if (entity instanceof AgentEntity allyAgent) {
                needsHelp = allyAgent.getMemory().isDangerNearby() ||
                        allyAgent.getCurrentState() == AgentState.FLEE;
            }

            if (!needsHelp) {
                continue;
            }

            double distSqr = this.distanceToSqr(entity);
            if (distSqr < closestDistSqr) {
                closestDistSqr = distSqr;
                closestAlly = entity;
            }
        }

        return closestAlly;
    }



    /**
     * Sends a random chat message based on the agent's current state
     */
    private void sendRandomChatMessage() {
        // Don't chat if the agent is in certain states that already handle chat
        if (currentState == AgentState.GREET_AGENT ||
                currentState == AgentState.CHAT_WITH_AGENT) {
            return;
        }

        // Get the agent's clean name for the message
        String agentName = getBaseName();
        Component agentNameComponent = Component.literal(agentName);

        // Get a chat message for the current state
        Component formattedMessage = AgentChatter.getFormattedChatMessage(currentState, agentNameComponent);

        // If we got a message, broadcast it
        if (formattedMessage != null && this.level().getServer() != null) {
            this.level().getServer().getPlayerList().broadcastSystemMessage(formattedMessage, false);
            MASONRY.LOGGER.debug("{} sent a random chat message", agentName);
        }
    }

    // Enhanced sendChatMessage method
    public void sendChatMessage(Component message) {
        if (level().isClientSide) return;

        // Check chat cooldown
        if (level().getGameTime() < nextChatTicks) {
            return;
        }

        // Set next chat time with some randomization
        int baseInterval = MIN_CHAT_INTERVAL_TICKS;
        int variance = MAX_CHAT_INTERVAL_TICKS - MIN_CHAT_INTERVAL_TICKS;
        nextChatTicks = (int) (level().getGameTime() + baseInterval + random.nextInt(variance));

        // Send to all players in the area
        level().players().forEach(player -> {
            if (player.distanceToSqr(this) < 1024) { // 32 block radius
                player.sendSystemMessage(message);
            }
        });

        MASONRY.LOGGER.debug("Agent {} sent chat: {}", getBaseName(), message.getString());
    }

    // Overloaded method for state-based automatic chatting
    public void sendChatMessage(String message) {
        sendChatMessage(Component.literal("<" + getBaseName() + "> " + message));
    }

    // Method specifically for resource-related communication
    public void sendResourceMessage(String messageType, Item item, String targetName) {
        Component message = AgentChatter.getResourceChatMessage(this, messageType, item, targetName);
        sendChatMessage(message);
    }
    @Override
    public void tick() {
        super.tick();
        memory.updateTimers();

        // Only process AI if cooldown allows
        if (memory.canChangeState() || memory.getTicksInCurrentState() > 20) {
            // Update perception
            updatePerceptions();

            // Update systems
            gossipSystem.tick(this);
//            scheduleSystem.tick(this);
            professionSystem.tick(this);
            tradingSystem.tick(this);

            // Handle current state (only if not in cooldown)
            if (stateBehaviors.containsKey(currentState)) {
                stateBehaviors.get(currentState).handle(this);
            }

            // Increment state ticks
            memory.incrementTicksInState();
        }

        // Always update display name and navigation
        updateDisplayName();



        // Make sure name is always visible
        this.setCustomNameVisible(true);

        // Update all timers in memory
        this.memory.updateTimers();


        // Handle random chatting
        if (!this.level().isClientSide && this.level().getServer() != null) {
            coordinationSystem.tick(this);

            // Advanced system integration
            List<AgentEntity> nearbyAgents = this.level().getEntitiesOfClass(
                    AgentEntity.class, this.getBoundingBox().inflate(64.0));

            advancedSystem.tick(this.level(), nearbyAgents);


            if (nextChatTicks > 0) {
                nextChatTicks--;
            } else {
                // Time to chat!
                sendRandomChatMessage();
                resetChatTimer();
            }
        }


    }


    /**
     * Handles the transition to a new state
     */
    private void transitionToState(AgentState newState) {
        if (currentState == newState) {
            return; // No transition needed
        }

        ;



        if (newState == AgentState.IDLE) {
            this.getNavigation().stop();
            this.memory.clearTargetEntity();
            this.memory.clearTargetLocation();
        }

        // Certain transitions might need special handling
        if (currentState == AgentState.GREET_AGENT && newState != AgentState.CHAT_WITH_AGENT) {
            // If we were greeting but not moving to chatting, clear the target entity
            this.memory.clearTargetEntity();
        }

        // Set the new state
        AgentState previousState = currentState;
        this.currentState = newState;

        // Update the display name with state information
        updateDisplayName();

        // Broadcast chat message for new state using AgentChatter, but not for common states like IDLE or WANDER
        if (!this.level().isClientSide && this.level().getServer() != null &&
                newState != AgentState.IDLE && newState != AgentState.WANDER) {
            Component agentNameComponent = Component.literal(this.getBaseName());
            Component formattedMessage = AgentChatter.getFormattedChatMessage(newState, agentNameComponent);

            if (formattedMessage != null) {
                MinecraftServer server = this.level().getServer();
                server.getPlayerList().broadcastSystemMessage(formattedMessage, false);
            }
        }

        // Reset state-specific timers and update goals
        memory.resetTicksInState();
        memory.updateLastStateChangeTime((int) this.level().getGameTime());
        updateGoalsForState(newState);

    }
    
    /**
     * Updates the entity's goals based on its current state
     */
    private void updateGoalsForState(AgentState state) {
        // Clear non-essential goals or task-specific goals from previous states if necessary.
        // The problematic removeGoal(new RandomStrollGoal(...)) has been removed.
        
        // Add state-specific goals
        switch (state) {
            case WANDER:
                // Now relies on the RandomStrollGoal added in registerGoals().
                // If WANDER state needs a *different* stroll behavior or priority, adjust here.
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
        AgentGoals.checkForDanger(this, perceptionRadius, memory);
        AgentGoals.checkForAllies(this, perceptionRadius, memory);
        AgentGoals.checkForResources(this, perceptionRadius, memory);
        // Simulate hunger increasing over time
        if (random.nextFloat() < 0.01f) {
            memory.setHungerLevel(Math.min(100, memory.getHungerLevel() + 1));
        }
    }
    
    /**
     * Determine if an entity is considered dangerous
     */
    public boolean isEntityDangerous(LivingEntity entity) {
        // Check if entity is hostile mob
        if (entity.getType() == EntityType.ZOMBIE ||
                entity.getType() == EntityType.SKELETON) {
            return true;
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

    // All handle<StateName>State methods have been removed and their logic
    // will be moved to individual IAgentStateHandler classes.
    @Override
    public boolean hurt(DamageSource pSource, float pAmount) {
        boolean wasHurt = super.hurt(pSource, pAmount);
        if (wasHurt && !this.level().isClientSide()) {
            if ((this.level().getGameTime() - this.lastDamageChatTime) > DAMAGE_CHAT_COOLDOWN_TICKS) {
                Component chatMessage = AgentChatter.getFormattedDamageTakenMessage(this.getName(), pSource);
                MinecraftServer server = this.level().getServer();
                if (server != null) {
                    server.getPlayerList().broadcastSystemMessage(chatMessage, false);
                }
                this.lastDamageChatTime = this.level().getGameTime();
            }
        }
        return wasHurt;
    }
    /**
     * Generates a descriptive text for the current state to display below the agent's name
     * @return A Component with state description text
     */

}