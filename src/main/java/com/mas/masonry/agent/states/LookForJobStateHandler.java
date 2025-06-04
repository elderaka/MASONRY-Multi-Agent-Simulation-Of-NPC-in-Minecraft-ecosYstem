package com.mas.masonry.agent.states;

import com.mas.masonry.AgentEntity;
import com.mas.masonry.MASONRY;
import com.mas.masonry.agent.systems.POIManager;
import com.mas.masonry.agent.systems.ProfessionSystem;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;

public class LookForJobStateHandler implements IAgentStateHandler {
    
    private static final int SEARCH_RADIUS = 24;
    private static final int MAX_SEARCH_TICKS = 2400;
    
    @Override
    public void handle(AgentEntity agent) {
        if (agent.getMemory().getTicksInCurrentState() == 0) {
            MASONRY.LOGGER.debug("{} is looking for a job", agent.getBaseName());
        }

        if (agent.getMemory().getTicksInCurrentState() < 60) {
            return;
        }


        Level level = agent.level();
        BlockPos agentPos = agent.blockPosition();
        
        // If agent has no profession, try to find one
        // If agent already has a profession, look for work site
        if (agent.getProfessionSystem().hasProfession()) {
            BlockPos jobSite = findJobSiteForProfession(agent, level, agentPos);
            if (jobSite != null) {
                agent.getPoiManager().registerPOI(
                        POIManager.POIType.JOB_SITE,
                        jobSite,
                        level
                );
                MASONRY.LOGGER.debug("{} found job site at {}", agent.getBaseName(), jobSite);
                agent.setCurrentState(AgentEntity.AgentState.GO_TO_WORK);
                return;
            }
        } else {
            // Try to find a profession
            ProfessionSystem.Profession newProfession = determineProfessionFromEnvironment(agent, level, agentPos);
            if (newProfession != ProfessionSystem.Profession.NONE) {
                if (agent.getProfessionSystem().setProfession(newProfession, agent)) {
                    MASONRY.LOGGER.debug("{} became a {}", agent.getBaseName(), newProfession.getDisplayName());
                    // Stay in LOOK_FOR_JOB to find work site for new profession
                    return;
                }
            }
        }

        // Search for opportunities
        if (!agent.getNavigation().isInProgress() && agent.getMemory().getTicksInCurrentState() % 100 == 0) {
            BlockPos randomTarget = agentPos.offset(
                    agent.getRandom().nextInt(SEARCH_RADIUS * 2) - SEARCH_RADIUS,
                    0,
                    agent.getRandom().nextInt(SEARCH_RADIUS * 2) - SEARCH_RADIUS
            );
            agent.getNavigation().moveTo(randomTarget.getX(), randomTarget.getY(), randomTarget.getZ(), 1.0);
        }

        // Give up after searching too long - transition to different state
        if (agent.getMemory().getTicksInCurrentState() > MAX_SEARCH_TICKS) {
            if (agent.getProfessionSystem().hasProfession()) {
                // Has profession but no job site - maybe try socializing
                agent.setCurrentState(AgentEntity.AgentState.WANDER);
            } else {
                // No profession found - become idle or wander
                agent.setCurrentState(AgentEntity.AgentState.IDLE);
            }
        }
    }
    
    private ProfessionSystem.Profession determineProfessionFromEnvironment(AgentEntity agent, Level level, BlockPos center) {
        // Count different work blocks nearby to determine best profession
        int farmBlocks = 0;
        int miningBlocks = 0;
        int buildingBlocks = 0;
        int lumberjackBlocks = 0;
        
        for (int x = -SEARCH_RADIUS; x <= SEARCH_RADIUS; x++) {
            for (int y = -4; y <= 4; y++) {
                for (int z = -SEARCH_RADIUS; z <= SEARCH_RADIUS; z++) {
                    BlockPos checkPos = center.offset(x, y, z);
                    BlockState blockState = level.getBlockState(checkPos);
                    Block block = blockState.getBlock();
                    
                    if (block == Blocks.FARMLAND || block instanceof CropBlock || block == Blocks.COMPOSTER) {
                        farmBlocks++;
                    } else if (block == Blocks.FURNACE || block == Blocks.STONE || 
                               block == Blocks.COAL_ORE || block == Blocks.IRON_ORE) {
                        miningBlocks++;
                    } else if (block == Blocks.CRAFTING_TABLE || block == Blocks.STONECUTTER) {
                        buildingBlocks++;
                    } else if (block instanceof RotatedPillarBlock && (
                               block == Blocks.OAK_LOG || block == Blocks.BIRCH_LOG)) {
                        lumberjackBlocks++;
                    }
                }
            }
        }
        
        // Choose profession based on most available work
        int maxBlocks = Math.max(Math.max(farmBlocks, miningBlocks), Math.max(buildingBlocks, lumberjackBlocks));
        
        if (maxBlocks == 0) {
            // No specific work environment, randomly choose
            ProfessionSystem.Profession[] professions = {
                ProfessionSystem.Profession.FARMER,
                ProfessionSystem.Profession.MINER,
                ProfessionSystem.Profession.BUILDER,
                ProfessionSystem.Profession.LUMBERJACK
            };
            return professions[agent.getRandom().nextInt(professions.length)];
        }
        
        if (farmBlocks == maxBlocks) return ProfessionSystem.Profession.FARMER;
        if (miningBlocks == maxBlocks) return ProfessionSystem.Profession.MINER;
        if (buildingBlocks == maxBlocks) return ProfessionSystem.Profession.BUILDER;
        if (lumberjackBlocks == maxBlocks) return ProfessionSystem.Profession.LUMBERJACK;
        
        return ProfessionSystem.Profession.NONE;
    }
    
    private BlockPos findJobSiteForProfession(AgentEntity agent, Level level, BlockPos center) {
        ProfessionSystem.Profession profession = agent.getProfessionSystem().getCurrentProfession();
        
        for (int x = -SEARCH_RADIUS; x <= SEARCH_RADIUS; x++) {
            for (int y = -4; y <= 4; y++) {
                for (int z = -SEARCH_RADIUS; z <= SEARCH_RADIUS; z++) {
                    BlockPos checkPos = center.offset(x, y, z);
                    Block block = level.getBlockState(checkPos).getBlock();
                    
                    if (profession.canWorkWith(block)) {
                        return checkPos;
                    }
                }
            }
        }
        
        return null;
    }
}