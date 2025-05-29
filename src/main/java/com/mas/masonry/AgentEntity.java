package com.mas.masonry;

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

import java.util.Random;
import java.util.EnumMap;
import java.util.List;
import java.util.Optional;
import com.mas.masonry.agent.states.IAgentStateHandler;
import com.mas.masonry.agent.states.IdleStateHandler;
import com.mas.masonry.agent.states.WanderStateHandler;
import com.mas.masonry.agent.states.SeekResourceStateHandler;
import com.mas.masonry.agent.states.FleeStateHandler;
import com.mas.masonry.agent.states.AttackStateHandler;
import com.mas.masonry.agent.states.HelpAllyStateHandler;
import com.mas.masonry.agent.states.FindTargetBlockStateHandler;
import com.mas.masonry.agent.states.MoveToTargetBlockStateHandler;
import com.mas.masonry.agent.states.HarvestBlockStateHandler;
import com.mas.masonry.agent.states.GreetAgentStateHandler;
import com.mas.masonry.agent.states.ChatWithAgentStateHandler;
import com.mas.masonry.agent.states.PlaceConstructionBlockStateHandler;

/**
 * Base entity class for intelligent agents that use Finite State Machine (FSM)
 * for decision-making and behavior control.
 */
public class AgentEntity extends PathfinderMob implements InventoryCarrier {
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



    // Fields for block targeting and harvesting
    // targetBlockType and targetBlockPos are declared further down
    private int findBlockScanRadius = 8; // How far out to scan for the target block
    private int findBlockAttempts = 0; // Counter for attempts to find the block
    private static final int MAX_FIND_BLOCK_ATTEMPTS = 5; // Max attempts before giving up

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
        REACT_TO_TIME_OF_DAY
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
        
        // Entity/environment awareness
        private boolean dangerNearby = false;
        private boolean resourceNearby = false;
        private boolean allyNearby = false;
        
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
        public void setTargetEntity(LivingEntity entity) { this.targetEntity = Optional.ofNullable(entity); }
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
    private final EnumMap<AgentState, IAgentStateHandler> stateBehaviors;
    
    // Perception radius 
    private final double perceptionRadius = 16.0;
    private double attackRange = 2.0D;
    private Random random; // Added for random behaviors
    
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

        // Initialize inventory
        this.inventoryCapability = LazyOptional.of(() -> new InvWrapper(this.inventory));

        // Assign a random name and make it visible
        String agentName = MASONRY.getRandomAgentName();
        this.setCustomName(Component.literal(agentName));
        this.setCustomNameVisible(true);

        // Initialize default target block type
        this.targetBlockType = Blocks.OAK_LOG;

        // Initialize Random instance
        this.random = new Random();
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
    public void setCurrentState(AgentState state) { this.currentState = state; }

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
        stateBehaviors.put(AgentState.IDLE, new IdleStateHandler());
        stateBehaviors.put(AgentState.WANDER, new WanderStateHandler());
        stateBehaviors.put(AgentState.SEEK_RESOURCE, new SeekResourceStateHandler());
        stateBehaviors.put(AgentState.FLEE, new FleeStateHandler());
        stateBehaviors.put(AgentState.ATTACK, new AttackStateHandler());
        stateBehaviors.put(AgentState.HELP_ALLY, new HelpAllyStateHandler());
        stateBehaviors.put(AgentState.FIND_TARGET_BLOCK, new FindTargetBlockStateHandler());
        stateBehaviors.put(AgentState.MOVE_TO_TARGET_BLOCK, new MoveToTargetBlockStateHandler());
        stateBehaviors.put(AgentState.HARVEST_BLOCK, new HarvestBlockStateHandler());
        stateBehaviors.put(AgentState.PLACE_CONSTRUCTION_BLOCK, new PlaceConstructionBlockStateHandler());

        // New states for social interaction & communication
        stateBehaviors.put(AgentState.GREET_AGENT, new GreetAgentStateHandler());
        stateBehaviors.put(AgentState.CHAT_WITH_AGENT, new ChatWithAgentStateHandler());
        // TODO: Instantiate and put other new state handlers here as they are created
        // stateBehaviors.put(AgentState.SHARE_RESOURCE_LOCATION, new ShareResourceLocationStateHandler());
        // stateBehaviors.put(AgentState.REQUEST_ITEM_FROM_AGENT, new RequestItemFromAgentStateHandler());
        // stateBehaviors.put(AgentState.GIVE_ITEM_TO_AGENT, new GiveItemToAgentStateHandler());
        // stateBehaviors.put(AgentState.FOLLOW_AGENT, new FollowAgentStateHandler());
        // stateBehaviors.put(AgentState.WARN_AGENT_OF_DANGER, new WarnAgentOfDangerStateHandler());

        // New states for work & task management
        // stateBehaviors.put(AgentState.LOOK_FOR_TASK, new LookForTaskStateHandler());
        // stateBehaviors.put(AgentState.TRAVEL_TO_TASK_LOCATION, new TravelToTaskLocationStateHandler());
        // stateBehaviors.put(AgentState.PERFORM_TASK, new PerformTaskStateHandler());
        // stateBehaviors.put(AgentState.RETURN_TO_BASE, new ReturnToBaseStateHandler());
        // stateBehaviors.put(AgentState.DEPOSIT_RESOURCES, new DepositResourcesStateHandler());
        // stateBehaviors.put(AgentState.RETRIEVE_ITEM, new RetrieveItemStateHandler());
        // stateBehaviors.put(AgentState.CRAFT_ITEM, new CraftItemStateHandler());

        // New states for needs & reactions
        // stateBehaviors.put(AgentState.SEEK_SHELTER, new SeekShelterStateHandler());
        // stateBehaviors.put(AgentState.SEEK_REST, new SeekRestStateHandler());
        // stateBehaviors.put(AgentState.SEEK_HEALING_ITEM, new SeekHealingItemStateHandler());
        // stateBehaviors.put(AgentState.REACT_TO_WEATHER, new ReactToWeatherStateHandler());
        // stateBehaviors.put(AgentState.REACT_TO_TIME_OF_DAY, new ReactToTimeOfDayStateHandler());
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
        // this.goalSelector.addGoal(7, new RandomStrollGoal(this, 1.0D));
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
        
        // Increment the ticks in current state
        memory.incrementTicksInState();
    }
    
    /**
     * FSM tick method to handle current state's behavior and transitions
     */
    private void tickAI() {
        // Execute behavior for current state
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
        // Default is to stay in current state
        AgentState nextState = currentState;
        
        switch (currentState) {
            case IDLE:
                if (memory.isDangerNearby() && memory.getFearLevel() > 50) {
                    nextState = AgentState.FLEE;
                } else if (memory.isResourceNearby() && memory.getHungerLevel() > 30) {
                    nextState = AgentState.SEEK_RESOURCE;
                } else if (this.getTarget() == null) { // Only consider helping if not already targeting something
                    // Check if any nearby allies are fleeing
                    List<AgentEntity> nearbyFleeingAllies = this.level().getEntitiesOfClass(AgentEntity.class, 
                        this.getBoundingBox().inflate(16.0D), // Check in a 16-block radius
                        ally -> ally != this && ally.isAlive() && ally.getCurrentState() == AgentState.FLEE);
                    
                    if (!nearbyFleeingAllies.isEmpty()) {
                        // If an ally is fleeing, decide to help.
                        // The HelpAllyGoal or HelpAllyStateHandler would then need to determine how to help.
                        // For example, it could set memory.setTargetEntity(nearbyFleeingAllies.get(0)); 
                        // to move towards or defend the first detected fleeing ally.
                        nextState = AgentState.HELP_ALLY;
                    }
                } else if (this.constructionOrigin != null && 
                         !MASONRY.SIMPLE_WALL_BLUEPRINT.isEmpty() &&
                         this.currentBlueprintIndex < MASONRY.SIMPLE_WALL_BLUEPRINT.size()) {
                    nextState = AgentState.PLACE_CONSTRUCTION_BLOCK;
                } else if (memory.isAllyNearby() && memory.getSocialMeter() > 30 && this.getTarget() == null && random.nextFloat() < 0.02f) { // Chance to greet if sociable, ally nearby, and no current target
                    nextState = AgentState.GREET_AGENT;
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
               } else if (this.getTarget() != null && this.getTarget().isAlive()) { // If it has a target (presumably from helping) and target is alive
                   nextState = AgentState.ATTACK;
               } else if (!memory.isAllyNearby() || memory.getTicksInCurrentState() > 200) { // No ally detected or helped for too long (approx 10s)
                   nextState = AgentState.IDLE;
               }
               break;

           case FIND_TARGET_BLOCK:
               // Transitions from FIND_TARGET_BLOCK are typically handled by its state handler
               // (e.g., to MOVE_TO_TARGET_BLOCK if found, or IDLE/WANDER if not found after attempts/timeout).
               // High-priority interrupts like FLEE can still occur based on general conditions.
               if (memory.isDangerNearby() && memory.getFearLevel() > 60) { // Example flee condition
                   nextState = AgentState.FLEE;
               }
               break;

           case MOVE_TO_TARGET_BLOCK:
               // Transitions from MOVE_TO_TARGET_BLOCK are typically handled by its state handler
               // (e.g., to HARVEST_BLOCK or PLACE_CONSTRUCTION_BLOCK on arrival, or FIND_TARGET_BLOCK/IDLE on failure/timeout).
               if (memory.isDangerNearby() && memory.getFearLevel() > 60) { // Example flee condition
                   nextState = AgentState.FLEE;
               }
               break;

           case HARVEST_BLOCK:
               // Transitions from HARVEST_BLOCK are typically handled by its state handler
               // (e.g., to IDLE, FIND_TARGET_BLOCK for more, or a storage-related state when full).
               if (memory.isDangerNearby() && memory.getFearLevel() > 60) { // Example flee condition
                   nextState = AgentState.FLEE;
               }
               break;

           case PLACE_CONSTRUCTION_BLOCK:
               // Transitions from PLACE_CONSTRUCTION_BLOCK are primarily handled by its state handler.
               // It might transition to IDLE, MOVE_TO_TARGET_BLOCK (for next spot), or FIND_TARGET_BLOCK (for materials).
               // A high-priority interrupt for danger:
               if (memory.isDangerNearby() && memory.getFearLevel() > 80) { // High threshold to interrupt construction
                   nextState = AgentState.FLEE;
               }
               break;

           // New states - primarily rely on their handlers for transitions, but can be interrupted by Flee.
           case GREET_AGENT:
           case CHAT_WITH_AGENT:
           case SHARE_RESOURCE_LOCATION:
           case REQUEST_ITEM_FROM_AGENT:
           case GIVE_ITEM_TO_AGENT:
           case FOLLOW_AGENT:
           case WARN_AGENT_OF_DANGER:
           case LOOK_FOR_TASK:
           case TRAVEL_TO_TASK_LOCATION:
           case PERFORM_TASK:
           case RETURN_TO_BASE:
           case DEPOSIT_RESOURCES:
           case RETRIEVE_ITEM:
           case CRAFT_ITEM:
           case SEEK_SHELTER:
           case SEEK_REST:
           case SEEK_HEALING_ITEM:
           case REACT_TO_WEATHER:
           case REACT_TO_TIME_OF_DAY:
               if (memory.isDangerNearby() && memory.getFearLevel() > 70) { // Generic flee condition for new states
                   nextState = AgentState.FLEE;
               }
               // Otherwise, these states manage their own lifecycle via their handlers.
               break;

            default:
                // If current state is unknown or unhandled in this switch, default to IDLE to prevent getting stuck.
                // However, all AgentState enum members should ideally be covered.
                MASONRY.LOGGER.warn("Unhandled state in determineNextState switch: {}. Defaulting to IDLE.", currentState);
                nextState = AgentState.IDLE;
                break;
        }
        
        return nextState;
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
     * Handles the transition to a new state
     */
    private void transitionToState(AgentState newState) {
        String oldStateDebug = this.getName().getString() + " transitioning from " + currentState + " to " + newState;
        // System.out.println(oldStateDebug); // Keep for debugging if needed, or use LOGGER

        // Broadcast chat message for new state using AgentChatter, but not for common states like IDLE or WANDER
        if (!this.level().isClientSide && this.level().getServer() != null && 
            newState != AgentState.IDLE && newState != AgentState.WANDER) {
            Component agentNameComponent = this.getCustomName() != null ? this.getCustomName() : Component.literal(MASONRY.getRandomAgentName());
            Component formattedMessage = AgentChatter.getFormattedChatMessage(newState, agentNameComponent);
            
            if (formattedMessage != null) {
                MinecraftServer server = this.level().getServer();
                server.getPlayerList().broadcastSystemMessage(formattedMessage, false);
            }
        }

        // Original transition logic continues here
        System.out.println(oldStateDebug); // You can move this or use a logger if preferred
        
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
}