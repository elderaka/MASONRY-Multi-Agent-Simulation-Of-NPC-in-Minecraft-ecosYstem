package com.mas.masonry.agent.systems;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import com.mas.masonry.AgentEntity;

/**
 * Manages an agent's home location and attachment to it
 */
public class HomeLocationSystem {
    private BlockPos homeLocation;
    private int homeAttachment = 0; // 0-100, how attached agent is to this home
    private boolean isHomeClaimed = false;
    private long timeSpentAtHome = 0;
    private int ticksSinceLastHomeVisit = 0;
    
    // Constants
    private static final int MAX_HOME_ATTACHMENT = 100;
    private static final int HOME_ATTACHMENT_DECAY_RATE = 1; // Per day
    private static final int HOME_VISIT_ATTACHMENT_GAIN = 5;
    private static final int MAX_TICKS_AWAY_FROM_HOME = 24000; // 1 day
    
    public boolean hasHome() {
        return homeLocation != null && isHomeClaimed;
    }
    
    public BlockPos getHomeLocation() {
        return homeLocation;
    }
    
    public int getHomeAttachment() {
        return homeAttachment;
    }
    
    public boolean shouldReturnHome(AgentEntity agent) {
        if (!hasHome()) return false;
        
        Level level = agent.level();
        long dayTime = level.getDayTime() % 24000;
        
        // Return home if:
        // 1. Night time (18000-6000 ticks)
        // 2. Low health
        // 3. Danger nearby
        // 4. Been away too long
        // 5. Rain and no shelter
        
        boolean isNight = dayTime >= 18000 || dayTime <= 6000;
        boolean lowHealth = agent.getMemory().getHealthPercent() < 30;
        boolean dangerNearby = agent.getMemory().isDangerNearby();
        boolean awayTooLong = ticksSinceLastHomeVisit > MAX_TICKS_AWAY_FROM_HOME;
        boolean rainAndNoShelter = level.isRaining() && !agent.isUnderWater() && level.canSeeSky(agent.blockPosition());
        
        return isNight || lowHealth || dangerNearby || awayTooLong || rainAndNoShelter;
    }
    
    public boolean canClaimHome(BlockPos pos, Level level) {
        if (pos == null) return false;
        
        // Check if the position has a valid bed
        BlockState blockState = level.getBlockState(pos);
        if (!(blockState.getBlock() instanceof BedBlock)) {
            return false;
        }
        
        // Check if it's already claimed by another agent
        // This would need integration with a global home registry
        return !HomeRegistry.getInstance().isPositionClaimed(pos);
    }
    
    public boolean claimHome(BlockPos pos, Level level) {
        if (!canClaimHome(pos, level)) {
            return false;
        }
        
        // Release previous home if any
        if (hasHome()) {
            abandonHome();
        }
        
        this.homeLocation = pos;
        this.isHomeClaimed = true;
        this.homeAttachment = 25; // Start with moderate attachment
        this.ticksSinceLastHomeVisit = 0;
        
        // Register with global registry
        HomeRegistry.getInstance().claimPosition(pos);
        
        return true;
    }
    
    public void abandonHome() {
        if (hasHome()) {
            // Unregister from global registry
            HomeRegistry.getInstance().releasePosition(homeLocation);
            
            this.isHomeClaimed = false;
            this.homeAttachment = 0;
            this.homeLocation = null;
        }
    }
    
    public void tick(AgentEntity agent) {
        if (!hasHome()) return;
        
        BlockPos agentPos = agent.blockPosition();
        
        // Check if agent is at home (within 3 blocks)
        if (agentPos.closerThan(homeLocation, 3.0)) {
            ticksSinceLastHomeVisit = 0;
            timeSpentAtHome++;
            
            // Increase attachment when spending time at home
            if (timeSpentAtHome % 1200 == 0) { // Every minute
                increaseAttachment(HOME_VISIT_ATTACHMENT_GAIN);
            }
        } else {
            ticksSinceLastHomeVisit++;
            timeSpentAtHome = 0;
        }
        
        // Decay attachment over time if away from home
        if (ticksSinceLastHomeVisit > 12000) { // Half day away
            if (agent.level().getDayTime() % 24000 == 0) { // Once per day
                decreaseAttachment(HOME_ATTACHMENT_DECAY_RATE);
            }
        }
        
        // Abandon home if attachment gets too low
        if (homeAttachment <= 0) {
            abandonHome();
        }
        
        // Validate home still exists
        if (hasHome() && !isValidHome(agent.level())) {
            abandonHome();
        }
    }
    
    private boolean isValidHome(Level level) {
        if (homeLocation == null) return false;
        
        BlockState blockState = level.getBlockState(homeLocation);
        return blockState.getBlock() instanceof BedBlock;
    }
    
    private void increaseAttachment(int amount) {
        homeAttachment = Math.min(MAX_HOME_ATTACHMENT, homeAttachment + amount);
    }
    
    private void decreaseAttachment(int amount) {
        homeAttachment = Math.max(0, homeAttachment - amount);
    }
    
    public double getDistanceToHome(BlockPos currentPos) {
        if (!hasHome()) return Double.MAX_VALUE;
        return Math.sqrt(currentPos.distSqr(homeLocation));
    }
    
    public boolean isAtHome(BlockPos currentPos) {
        if (!hasHome()) return false;
        return currentPos.closerThan(homeLocation, 3.0);
    }
}