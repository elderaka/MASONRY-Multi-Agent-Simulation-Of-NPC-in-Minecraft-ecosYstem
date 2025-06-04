
package com.mas.masonry.agent.systems;

import com.mas.masonry.AgentEntity;
import net.minecraft.world.level.Level;

/**
 * Manages an agent's daily schedule based on Minecraft time
 * Minecraft day cycle: 0-24000 ticks (20 minutes real time)
 * Day: 0-12000, Night: 12000-24000
 */
public class ScheduleSystem {
    // Time constants (in ticks)
    private static final int DAWN = 0;          // 6:00 AM
    private static final int WORK_START = 2000; // 7:00 AM
    private static final int MORNING_BREAK = 4000; // 8:00 AM
    private static final int NOON = 6000;       // 12:00 PM
    private static final int AFTERNOON_WORK = 8000; // 2:00 PM
    private static final int WORK_END = 10000;  // 4:00 PM
    private static final int EVENING_SOCIAL = 11000; // 5:30 PM
    private static final int DUSK = 12000;      // 6:00 PM
    private static final int NIGHT_START = 13000; // 6:30 PM
    private static final int SLEEP_TIME = 14000; // 8:00 PM
    private static final int MIDNIGHT = 18000;  // 12:00 AM
    
    private AgentActivity currentActivity = AgentActivity.IDLE;
    private AgentActivity previousActivity = AgentActivity.IDLE;
    private int activityStartTime = 0;
    
    // Individual schedule preferences (adds personality)
    private int workStartOffset = 0;    // -1000 to +1000 ticks (early bird vs late starter)
    private int socialPreference = 50;  // 0-100, how much they like social time
    private int workEthic = 50;         // 0-100, how dedicated to work they are
    
    public enum AgentActivity {
        SLEEP,          // Nighttime rest
        WAKE_UP,        // Just waking up, brief transition
        IDLE,           // No specific activity
        WORK,           // At job site performing profession tasks
        SOCIALIZE,      // At meeting point or with other agents
        BREAK,          // Short rest during work hours
        EVENING_FREE,   // Free time before sleep
        PANIC           // Override for danger situations
    }
    
    public ScheduleSystem() {
        // Randomize personality traits
        this.workStartOffset = (int) ((Math.random() - 0.5) * 2000); // -1000 to +1000
        this.socialPreference = (int) (Math.random() * 100);
        this.workEthic = (int) (Math.random() * 100);
    }
    
    public AgentActivity getCurrentActivity() {
        return currentActivity;
    }
    
    public AgentActivity getPreviousActivity() {
        return previousActivity;
    }
    
    public int getTimeInCurrentActivity() {
        return activityStartTime;
    }
    
    public AgentActivity determineActivity(Level level, AgentEntity agent) {
        long worldTime = level.getDayTime();
        long dayTime = worldTime % 24000;
        
        // Check for panic override (danger nearby)
        if (agent.getMemory().isDangerNearby()) {
            updateActivity(AgentActivity.PANIC);
            return AgentActivity.PANIC;
        }
        
        AgentActivity newActivity = calculateScheduledActivity(dayTime, agent);
        
        // Update activity if it changed
        if (newActivity != currentActivity) {
            updateActivity(newActivity);
        }
        
        return currentActivity;
    }
    
    private AgentActivity calculateScheduledActivity(long dayTime, AgentEntity agent) {
        // Apply personal work start offset
        long adjustedWorkStart = WORK_START + workStartOffset;
        long adjustedWorkEnd = WORK_END + workStartOffset;
        
        // Clamp to reasonable bounds
        adjustedWorkStart = Math.max(DAWN, Math.min(NOON, adjustedWorkStart));
        adjustedWorkEnd = Math.max(NOON, Math.min(DUSK, adjustedWorkEnd));
        
        // Determine activity based on time
        if (dayTime >= SLEEP_TIME || dayTime < DAWN) {
            return AgentActivity.SLEEP;
        } else if (dayTime >= DAWN && dayTime < DAWN + 400) {
            return AgentActivity.WAKE_UP;
        } else if (dayTime >= adjustedWorkStart && dayTime < adjustedWorkStart + 1000) {
            // Work period with breaks
            if (hasWorkBreak(dayTime, adjustedWorkStart)) {
                return AgentActivity.BREAK;
            }
            return AgentActivity.WORK;
        } else if (dayTime >= MORNING_BREAK && dayTime < MORNING_BREAK + 600) {
            return AgentActivity.BREAK;
        } else if (dayTime >= NOON - 600 && dayTime < NOON + 600) {
            // Lunch break / social time
            if (socialPreference > 40) {
                return AgentActivity.SOCIALIZE;
            } else {
                return AgentActivity.BREAK;
            }
        } else if (dayTime >= adjustedWorkStart && dayTime < adjustedWorkEnd) {
            // Main work period
            if (workEthic > 30 && agent.getProfessionSystem().hasProfession()) {
                return AgentActivity.WORK;
            } else {
                return AgentActivity.IDLE;
            }
        } else if (dayTime >= EVENING_SOCIAL && dayTime < SLEEP_TIME) {
            // Evening social time
            if (socialPreference > 50) {
                return AgentActivity.SOCIALIZE;
            } else {
                return AgentActivity.EVENING_FREE;
            }
        } else {
            return AgentActivity.IDLE;
        }
    }
    
    private boolean hasWorkBreak(long currentTime, long workStart) {
        long workElapsed = currentTime - workStart;
        // Take a break every 2000 ticks (10 minutes) for 200 ticks (1 minute)
        return (workElapsed % 2000) < 200;
    }
    
    private void updateActivity(AgentActivity newActivity) {
        if (newActivity != currentActivity) {
            previousActivity = currentActivity;
            currentActivity = newActivity;
            activityStartTime = 0;
        } else {
            activityStartTime++;
        }
    }
    
    public boolean shouldWork(Level level, AgentEntity agent) {
        AgentActivity activity = determineActivity(level, agent);
        return activity == AgentActivity.WORK && agent.getProfessionSystem().hasProfession();
    }
    
    public boolean shouldSocialize(Level level, AgentEntity agent) {
        AgentActivity activity = determineActivity(level, agent);
        return activity == AgentActivity.SOCIALIZE && socialPreference > 40;
    }
    
    public boolean shouldSleep(Level level) {
        long dayTime = level.getDayTime() % 24000;
        return dayTime >= SLEEP_TIME || dayTime < DAWN;
    }
    
    public boolean shouldWakeUp(Level level) {
        long dayTime = level.getDayTime() % 24000;
        return dayTime >= DAWN && dayTime < DAWN + 400;
    }
    
    public boolean isWorkTime(Level level) {
        long dayTime = level.getDayTime() % 24000;
        long adjustedWorkStart = WORK_START + workStartOffset;
        long adjustedWorkEnd = WORK_END + workStartOffset;
        
        adjustedWorkStart = Math.max(DAWN, Math.min(NOON, adjustedWorkStart));
        adjustedWorkEnd = Math.max(NOON, Math.min(DUSK, adjustedWorkEnd));
        
        return dayTime >= adjustedWorkStart && dayTime < adjustedWorkEnd;
    }
    
    public boolean isSocialTime(Level level) {
        long dayTime = level.getDayTime() % 24000;
        return (dayTime >= NOON - 600 && dayTime < NOON + 600) || 
               (dayTime >= EVENING_SOCIAL && dayTime < SLEEP_TIME);
    }
    
    public String getTimeOfDayDescription(Level level) {
        long dayTime = level.getDayTime() % 24000;
        
        if (dayTime < DAWN) return "Late Night";
        else if (dayTime < WORK_START) return "Early Morning";
        else if (dayTime < NOON) return "Morning";
        else if (dayTime < WORK_END) return "Afternoon";
        else if (dayTime < DUSK) return "Evening";
        else if (dayTime < SLEEP_TIME) return "Dusk";
        else return "Night";
    }
    
    // Getters for personality traits
    public int getWorkStartOffset() { return workStartOffset; }
    public int getSocialPreference() { return socialPreference; }
    public int getWorkEthic() { return workEthic; }
    
    // Setters for personality traits (can be used for character development)
    public void adjustSocialPreference(int change) {
        socialPreference = Math.max(0, Math.min(100, socialPreference + change));
    }
    
    public void adjustWorkEthic(int change) {
        workEthic = Math.max(0, Math.min(100, workEthic + change));
    }
}