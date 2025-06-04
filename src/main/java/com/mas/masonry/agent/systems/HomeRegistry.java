package com.mas.masonry.agent.systems;

import net.minecraft.core.BlockPos;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Global registry to track which beds/homes are claimed by agents
 * Prevents multiple agents from claiming the same home
 */
public class HomeRegistry {
    private static HomeRegistry instance;
    
    // Thread-safe set of claimed positions
    private final Set<BlockPos> claimedPositions = ConcurrentHashMap.newKeySet();
    
    private HomeRegistry() {}
    
    public static HomeRegistry getInstance() {
        if (instance == null) {
            instance = new HomeRegistry();
        }
        return instance;
    }
    
    public boolean isPositionClaimed(BlockPos pos) {
        return claimedPositions.contains(pos);
    }
    
    public void claimPosition(BlockPos pos) {
        claimedPositions.add(pos);
    }
    
    public void releasePosition(BlockPos pos) {
        claimedPositions.remove(pos);
    }
    
    public void clearAll() {
        claimedPositions.clear();
    }
    
    public int getClaimedCount() {
        return claimedPositions.size();
    }
}