
package com.mas.masonry.agent.states;

import com.mas.masonry.AgentEntity;
import com.mas.masonry.MASONRY;
import com.mas.masonry.agent.systems.POIManager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.Blocks;

public class GoToMeetingStateHandler implements IAgentStateHandler {
    
    private static final double CLOSE_ENOUGH_TO_MEETING = 4.0;
    private static final int MAX_TRAVEL_TO_MEETING_TICKS = 1200;
    private static final int MEETING_SEARCH_RADIUS = 32;
    
    @Override
    public void handle(AgentEntity agent) {
        if (agent.getMemory().getTicksInCurrentState() == 0) {
            MASONRY.LOGGER.debug("{} is going to a meeting", agent.getBaseName());
        }
        
        BlockPos meetingPoint = agent.getPoiManager().getMeetingPoint();
        
        // If no meeting point registered, look for one
        if (meetingPoint == null) {
            meetingPoint = findNearbyMeetingPoint(agent);
            if (meetingPoint != null) {
                agent.getPoiManager().registerPOI(
                    POIManager.POIType.MEETING_POINT,
                    meetingPoint, 
                    agent.level()
                );
            } else {
                // No meeting point found, just socialize in place
                agent.setCurrentState(AgentEntity.AgentState.CHAT_WITH_AGENT);
                return;
            }
        }
        
        // Check if still social time
        if (!agent.getScheduleSystem().isSocialTime(agent.level())) {
            agent.setCurrentState(AgentEntity.AgentState.IDLE);
            return;
        }
        
        double distanceToMeeting = agent.blockPosition().distSqr(meetingPoint);
        
        if (distanceToMeeting > CLOSE_ENOUGH_TO_MEETING * CLOSE_ENOUGH_TO_MEETING) {
            // Navigate to meeting point
            agent.getNavigation().moveTo(meetingPoint.getX(), meetingPoint.getY(), meetingPoint.getZ(), 1.0);
            
            // Give up if taking too long
            if (agent.getMemory().getTicksInCurrentState() > MAX_TRAVEL_TO_MEETING_TICKS) {
                agent.setCurrentState(AgentEntity.AgentState.IDLE);
            }
        } else {
            // Reached meeting point!
            MASONRY.LOGGER.debug("{} reached meeting point", agent.getBaseName());
            agent.setCurrentState(AgentEntity.AgentState.SOCIALIZE_AT_MEETING);
        }
    }
    
    private BlockPos findNearbyMeetingPoint(AgentEntity agent) {
        BlockPos center = agent.blockPosition();
        
        // Look for bells first (primary meeting points)
        for (int x = -MEETING_SEARCH_RADIUS; x <= MEETING_SEARCH_RADIUS; x++) {
            for (int y = -4; y <= 4; y++) {
                for (int z = -MEETING_SEARCH_RADIUS; z <= MEETING_SEARCH_RADIUS; z++) {
                    BlockPos checkPos = center.offset(x, y, z);
                    if (agent.level().getBlockState(checkPos).getBlock() instanceof BellBlock) {
                        return checkPos;
                    }
                }
            }
        }
        
        // If no bell found, look for campfires
        for (int x = -MEETING_SEARCH_RADIUS/2; x <= MEETING_SEARCH_RADIUS/2; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -MEETING_SEARCH_RADIUS/2; z <= MEETING_SEARCH_RADIUS/2; z++) {
                    BlockPos checkPos = center.offset(x, y, z);
                    if (agent.level().getBlockState(checkPos).getBlock() == Blocks.CAMPFIRE) {
                        return checkPos;
                    }
                }
            }
        }
        
        return null;
    }
}