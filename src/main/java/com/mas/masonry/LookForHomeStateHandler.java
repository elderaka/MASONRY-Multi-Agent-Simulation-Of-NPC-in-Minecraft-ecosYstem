package com.mas.masonry.agent.states;

import com.mas.masonry.AgentEntity;
import com.mas.masonry.MASONRY;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;

public class LookForHomeStateHandler implements IAgentStateHandler {
    
    private static final int SEARCH_RADIUS = 32;
    private static final int MAX_SEARCH_TICKS = 2400; // 2 minutes max search time
    
    @Override
    public void handle(AgentEntity agent) {
        if (agent.getMemory().getTicksInCurrentState() == 0) {
            MASONRY.LOGGER.debug("{} is looking for a home", agent.getBaseName());
        }
        
        Level level = agent.level();
        BlockPos agentPos = agent.blockPosition();
        
        // Search for nearby beds
        BlockPos foundBed = findNearbyBed(agent, agentPos, level);
        
        if (foundBed != null) {
            // Try to claim the bed
            if (agent.getHomeLocationSystem().claimHome(foundBed, level)) {
                MASONRY.LOGGER.debug("{} found and claimed a home at {}", agent.getBaseName(), foundBed);
                agent.setCurrentState(AgentEntity.AgentState.RETURN_HOME);
                return;
            }
        }
        
        // If no bed found, keep wandering to search
        if (!agent.getNavigation().isInProgress()) {
            // Pick a random direction to continue searching
            BlockPos randomTarget = agentPos.offset(
                agent.getRandom().nextInt(SEARCH_RADIUS * 2) - SEARCH_RADIUS,
                0,
                agent.getRandom().nextInt(SEARCH_RADIUS * 2) - SEARCH_RADIUS
            );
            
            agent.getNavigation().moveTo(randomTarget.getX(), randomTarget.getY(), randomTarget.getZ(), 1.0);
        }
        
        // Give up after searching for too long
        if (agent.getMemory().getTicksInCurrentState() > MAX_SEARCH_TICKS) {
            MASONRY.LOGGER.debug("{} gave up looking for home", agent.getBaseName());
            agent.setCurrentState(AgentEntity.AgentState.WANDER);
        }
    }
    
    private BlockPos findNearbyBed(AgentEntity agent, BlockPos center, Level level) {
        for (int x = -SEARCH_RADIUS; x <= SEARCH_RADIUS; x++) {
            for (int y = -4; y <= 4; y++) {
                for (int z = -SEARCH_RADIUS; z <= SEARCH_RADIUS; z++) {
                    BlockPos checkPos = center.offset(x, y, z);
                    BlockState blockState = level.getBlockState(checkPos);
                    
                    if (blockState.getBlock() instanceof BedBlock) {
                        // Check if this bed can be claimed
                        if (agent.getHomeLocationSystem().canClaimHome(checkPos, level)) {
                            return checkPos;
                        }
                    }
                }
            }
        }
        return null;
    }
}