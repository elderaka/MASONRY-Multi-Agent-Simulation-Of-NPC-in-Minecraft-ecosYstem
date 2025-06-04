package com.mas.masonry.agent.systems;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages Points of Interest (POI) for an agent
 */
public class POIManager {
    // Different types of POIs the agent can remember
    private BlockPos jobSite;
    private BlockPos meetingPoint;
    private final List<BlockPos> resourceLocations = new ArrayList<>();
    private final List<BlockPos> dangerLocations = new ArrayList<>();
    
    // Track when POIs were last visited/used
    private final Map<BlockPos, Long> lastVisitTimes = new HashMap<>();
    private final Map<BlockPos, Integer> poiValues = new HashMap<>(); // How valuable this POI is
    
    // Constants
    private static final int MAX_RESOURCE_LOCATIONS = 10;
    private static final int MAX_DANGER_LOCATIONS = 5;
    private static final long POI_EXPIRY_TIME = 48000; // 2 Minecraft days
    private static final int DEFAULT_POI_VALUE = 50;
    
    public enum POIType {
        JOB_SITE, MEETING_POINT, RESOURCE, DANGER
    }
    
    public void registerPOI(POIType type, BlockPos pos, Level level) {
        if (pos == null) return;
        
        long currentTime = level.getGameTime();
        
        switch(type) {
            case JOB_SITE -> {
                if (isValidJobSite(pos, level)) {
                    this.jobSite = pos;
                    lastVisitTimes.put(pos, currentTime);
                    poiValues.put(pos, DEFAULT_POI_VALUE);
                }
            }
            case MEETING_POINT -> {
                this.meetingPoint = pos;
                lastVisitTimes.put(pos, currentTime);
                poiValues.put(pos, DEFAULT_POI_VALUE);
            }
            case RESOURCE -> {
                if (!resourceLocations.contains(pos)) {
                    resourceLocations.add(pos);
                    lastVisitTimes.put(pos, currentTime);
                    poiValues.put(pos, calculateResourceValue(pos, level));
                    
                    // Remove oldest if we have too many
                    if (resourceLocations.size() > MAX_RESOURCE_LOCATIONS) {
                        BlockPos oldest = getOldestPOI(resourceLocations);
                        resourceLocations.remove(oldest);
                        lastVisitTimes.remove(oldest);
                        poiValues.remove(oldest);
                    }
                }
            }
            case DANGER -> {
                if (!dangerLocations.contains(pos)) {
                    dangerLocations.add(pos);
                    lastVisitTimes.put(pos, currentTime);
                    poiValues.put(pos, 100); // Danger locations are high priority
                    
                    // Remove oldest if we have too many
                    if (dangerLocations.size() > MAX_DANGER_LOCATIONS) {
                        BlockPos oldest = getOldestPOI(dangerLocations);
                        dangerLocations.remove(oldest);
                        lastVisitTimes.remove(oldest);
                        poiValues.remove(oldest);
                    }
                }
            }
        }
    }
    
    public BlockPos getNearestPOI(POIType type, BlockPos currentPos) {
        List<BlockPos> poiList;
        
        switch(type) {
            case JOB_SITE -> {
                return jobSite;
            }
            case MEETING_POINT -> {
                return meetingPoint;
            }
            case RESOURCE -> poiList = resourceLocations;
            case DANGER -> poiList = dangerLocations;
            default -> {
                return null;
            }
        }
        
        BlockPos nearest = null;
        double nearestDistSqr = Double.MAX_VALUE;
        
        for (BlockPos poi : poiList) {
            double distSqr = currentPos.distSqr(poi);
            if (distSqr < nearestDistSqr) {
                nearestDistSqr = distSqr;
                nearest = poi;
            }
        }
        
        return nearest;
    }
    
    public boolean shouldVisitPOI(POIType type, long currentTime) {
        BlockPos poi = null;
        
        switch(type) {
            case JOB_SITE -> poi = jobSite;
            case MEETING_POINT -> poi = meetingPoint;
            default -> {
                return false;
            }
        }
        
        if (poi == null) return false;
        
        Long lastVisit = lastVisitTimes.get(poi);
        if (lastVisit == null) return true;
        
        // Visit job site during work hours, meeting point during social hours
        long timeSinceVisit = currentTime - lastVisit;
        
        return switch (type) {
            case JOB_SITE -> timeSinceVisit > 2400; // Visit every 2 minutes of game time
            case MEETING_POINT -> timeSinceVisit > 6000; // Visit every 5 minutes
            default -> false;
        };
    }
    
    public void updatePOIValue(BlockPos pos, int newValue) {
        if (poiValues.containsKey(pos)) {
            poiValues.put(pos, Math.max(0, Math.min(100, newValue)));
        }
    }
    
    public void removePOI(POIType type, BlockPos pos) {
        switch(type) {
            case JOB_SITE -> {
                if (jobSite != null && jobSite.equals(pos)) {
                    jobSite = null;
                }
            }
            case MEETING_POINT -> {
                if (meetingPoint != null && meetingPoint.equals(pos)) {
                    meetingPoint = null;
                }
            }
            case RESOURCE -> resourceLocations.remove(pos);
            case DANGER -> dangerLocations.remove(pos);
        }
        
        lastVisitTimes.remove(pos);
        poiValues.remove(pos);
    }
    
    public void tick(Level level) {
        long currentTime = level.getGameTime();
        
        // Clean up expired POIs
        cleanupExpiredPOIs(currentTime, level);
        
        // Validate existing POIs
        validatePOIs(level);
    }
    
    private void cleanupExpiredPOIs(long currentTime, Level level) {
        // Remove expired resource locations
        resourceLocations.removeIf(pos -> {
            Long lastVisit = lastVisitTimes.get(pos);
            if (lastVisit != null && (currentTime - lastVisit) > POI_EXPIRY_TIME) {
                lastVisitTimes.remove(pos);
                poiValues.remove(pos);
                return true;
            }
            return false;
        });
        
        // Remove expired danger locations
        dangerLocations.removeIf(pos -> {
            Long lastVisit = lastVisitTimes.get(pos);
            if (lastVisit != null && (currentTime - lastVisit) > POI_EXPIRY_TIME) {
                lastVisitTimes.remove(pos);
                poiValues.remove(pos);
                return true;
            }
            return false;
        });
    }
    
    private void validatePOIs(Level level) {
        // Validate job site
        if (jobSite != null && !isValidJobSite(jobSite, level)) {
            removePOI(POIType.JOB_SITE, jobSite);
        }
        
        // Validate meeting point
        if (meetingPoint != null && !isValidMeetingPoint(meetingPoint, level)) {
            removePOI(POIType.MEETING_POINT, meetingPoint);
        }
    }
    
    private boolean isValidJobSite(BlockPos pos, Level level) {
        BlockState blockState = level.getBlockState(pos);
        Block block = blockState.getBlock();
        
        // Job sites are work-related blocks
        return block instanceof CraftingTableBlock ||
               block instanceof FurnaceBlock ||
               block instanceof AnvilBlock ||
               block instanceof CartographyTableBlock ||
               block instanceof FletchingTableBlock ||
               block instanceof SmithingTableBlock ||
               block instanceof StonecutterBlock ||
               block instanceof LoomBlock ||
               block instanceof ComposterBlock ||
               block instanceof BarrelBlock ||
               block instanceof SmokerBlock ||
               block instanceof BlastFurnaceBlock;
    }
    
    private boolean isValidMeetingPoint(BlockPos pos, Level level) {
        BlockState blockState = level.getBlockState(pos);
        Block block = blockState.getBlock();
        
        // Meeting points are typically bells or open areas
        return block instanceof BellBlock || 
               block == Blocks.AIR || 
               block == Blocks.CAMPFIRE;
    }
    
    private int calculateResourceValue(BlockPos pos, Level level) {
        BlockState blockState = level.getBlockState(pos);
        Block block = blockState.getBlock();
        
        // Calculate value based on block type
        if (block == Blocks.DIAMOND_ORE || block == Blocks.DEEPSLATE_DIAMOND_ORE) {
            return 100;
        } else if (block == Blocks.IRON_ORE || block == Blocks.DEEPSLATE_IRON_ORE) {
            return 80;
        } else if (block == Blocks.GOLD_ORE || block == Blocks.DEEPSLATE_GOLD_ORE) {
            return 70;
        } else if (block == Blocks.COAL_ORE || block == Blocks.DEEPSLATE_COAL_ORE) {
            return 40;
        } else if (block instanceof CropBlock) {
            return 60;
        } else if (block == Blocks.CHEST || block == Blocks.BARREL) {
            return 90;
        }
        
        return DEFAULT_POI_VALUE;
    }
    
    private BlockPos getOldestPOI(List<BlockPos> poiList) {
        BlockPos oldest = null;
        long oldestTime = Long.MAX_VALUE;
        
        for (BlockPos pos : poiList) {
            Long visitTime = lastVisitTimes.get(pos);
            if (visitTime != null && visitTime < oldestTime) {
                oldestTime = visitTime;
                oldest = pos;
            }
        }
        
        return oldest;
    }
    
    // Getters
    public BlockPos getJobSite() { return jobSite; }
    public BlockPos getMeetingPoint() { return meetingPoint; }
    public List<BlockPos> getResourceLocations() { return new ArrayList<>(resourceLocations); }
    public List<BlockPos> getDangerLocations() { return new ArrayList<>(dangerLocations); }
    
    public boolean hasJobSite() { return jobSite != null; }
    public boolean hasMeetingPoint() { return meetingPoint != null; }
    public boolean hasResourceLocations() { return !resourceLocations.isEmpty(); }
}