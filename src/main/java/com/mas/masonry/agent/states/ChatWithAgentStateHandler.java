package com.mas.masonry.agent.states;

import com.mas.masonry.AgentEntity;
import com.mas.masonry.MASONRY;
import com.mas.masonry.AgentChatter;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.MinecraftServer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

public class ChatWithAgentStateHandler implements IAgentStateHandler {

    // How long agents will chat together before going back to idle
    // Increasing from 600 to 1200 ticks (60 seconds)
    private static final int MIN_CHAT_TIME_TICKS = 400; // 20 seconds
    private static final int MAX_CHAT_TIME_TICKS = 1200; // 60 seconds

    // Conversation timing constants
    private static final int INITIAL_CHAT_DELAY = 20; // 1 second before firs
    // t message
    private static final int CHAT_RESPONSE_DELAY = 20; // 1 second between messages
    private static final int MIN_MESSAGES_PER_CONVERSATION = 2;
    private static final int MAX_MESSAGES_PER_CONVERSATION = 5;

    // How close agents need to be to maintain a conversation
    // Decreasing from 16.0 to 1.5 (1.22 blocks squared)
    private static final double MAX_CHAT_DISTANCE_SQR = 1.5;

    // Conversation state tracking
    private int conversationId = -1;
    private int messagesExchanged = 0;
    private int targetMessageCount = 0;
    private int messageTimer = 0;
    private boolean isInitiator = false;
    private boolean waitingForResponse = false;

    @Override
    public void handle(AgentEntity agent) {
        // Initialize conversation when entering the state
        if (agent.getMemory().getTicksInCurrentState() == 0) {
            MASONRY.LOGGER.debug("{} is in CHAT_WITH_AGENT state.", agent.getBaseName());

            // Generate a random ID for this conversation
            conversationId = agent.getRandom().nextInt(10000);

            // Determine if this agent is the initiator of the conversation
            LivingEntity target = agent.getMemory().getTargetEntity().orElse(null);
            if (target instanceof AgentEntity targetAgent) {
                // If the target is already in CHAT_WITH_AGENT state, this agent is responding
                isInitiator = targetAgent.getCurrentState() != AgentEntity.AgentState.CHAT_WITH_AGENT;
            } else {
                isInitiator = true;
            }

            // Set up initial conversation parameters
            messagesExchanged = 0;
            targetMessageCount = agent.getRandom().nextInt(
                    MAX_MESSAGES_PER_CONVERSATION - MIN_MESSAGES_PER_CONVERSATION + 1)
                    + MIN_MESSAGES_PER_CONVERSATION;

            // Set initial message timer
            messageTimer = isInitiator ? INITIAL_CHAT_DELAY : CHAT_RESPONSE_DELAY * 2;
            waitingForResponse = false;

            MASONRY.LOGGER.debug("{} conversation setup: id={}, initiator={}, targetMessages={}",
                    agent.getBaseName(), conversationId, isInitiator, targetMessageCount);
        }

        // Get the chat target
        Optional<LivingEntity> targetOptional = agent.getMemory().getTargetEntity();

        if (targetOptional.isEmpty()) {
            MASONRY.LOGGER.debug("{} has no chat target, returning to IDLE.", agent.getBaseName());
            agent.setCurrentState(AgentEntity.AgentState.IDLE);
            return;
        }

        LivingEntity target = targetOptional.get();

        // Check if target is valid and still an agent
        if (!(target instanceof AgentEntity) || !target.isAlive() || target == agent) {
            MASONRY.LOGGER.debug("{} has invalid chat target, returning to IDLE.", agent.getBaseName());
            agent.setCurrentState(AgentEntity.AgentState.IDLE);
            agent.getMemory().clearTargetEntity();
            return;
        }

        AgentEntity targetAgent = (AgentEntity)target;

        // Check if target is too far away
        double distanceSqr = agent.distanceToSqr(target);

        // If agents are too far apart, try to move closer
        if (distanceSqr > MAX_CHAT_DISTANCE_SQR) {
            // Try to move closer to the target
            agent.getNavigation().moveTo(target, 1.0D);

            // Log movement occasionally
            if (agent.getMemory().getTicksInCurrentState() % 40 == 0) {
                MASONRY.LOGGER.debug("{} moving closer to {} for conversation, distance: {}",
                        agent.getBaseName(),
                        targetAgent.getBaseName(),
                        Math.sqrt(distanceSqr));
            }

            // Don't exchange messages until they're close enough
            return;
        } else {
            // Close enough, stop moving
            agent.getNavigation().stop();
        }

        // Look at the target while chatting
        lookAtEntity(agent, target);

        // Handle conversation flow

        // Decrement message timer
        if (messageTimer > 0) {
            messageTimer--;
        }

        // Time to send a message?
        if (messageTimer == 0 && !waitingForResponse) {
            // Check if target is still in chat state
            if (targetAgent.getCurrentState() != AgentEntity.AgentState.CHAT_WITH_AGENT) {
                MASONRY.LOGGER.debug("{} target is no longer in chat state, ending conversation.",
                        agent.getBaseName());
                agent.setCurrentState(AgentEntity.AgentState.IDLE);
                agent.getMemory().clearTargetEntity();
                return;
            }

            // Send a message
            sendChatMessage(agent, targetAgent);
            messagesExchanged++;

            // Set waiting for response
            waitingForResponse = true;

            // Set response timer for target
            messageTimer = CHAT_RESPONSE_DELAY;

            // Check if we've reached the target message count
            if (messagesExchanged >= targetMessageCount) {
                MASONRY.LOGGER.debug("{} reached target message count, will end conversation after response.",
                        agent.getBaseName());

                // Conversation will end after one more exchange
                if (!isInitiator) {
                    // If we're not the initiator and have sent our message, end conversation
                    MASONRY.LOGGER.debug("{} ending conversation as responder.", agent.getBaseName());
                    endConversation(agent, targetAgent);
                }
            }
        }

        // End conversation if it's gone on too long
        if (agent.getMemory().getTicksInCurrentState() >= MAX_CHAT_TIME_TICKS) {
            MASONRY.LOGGER.debug("{} conversation timeout, ending.", agent.getBaseName());
            endConversation(agent, targetAgent);
        }
    }

    /**
     * Sends a chat message from one agent to another
     */
    private void sendChatMessage(AgentEntity speaker, AgentEntity listener) {
        // Create a chat message based on whether this agent is initiating or responding
        Component chatMessage = AgentChatter.getConversationMessage(speaker, listener,
                isInitiator ? (messagesExchanged % 2 == 0) : (messagesExchanged % 2 != 0));

        // Send the message to all players
        if (speaker.level() instanceof ServerLevel && !speaker.level().isClientSide) {
            MinecraftServer server = speaker.level().getServer();
            if (server != null) {
                server.getPlayerList().broadcastSystemMessage(chatMessage, false);
            }
        }
    }

    /**
     * Ends the conversation gracefully
     */
    private void endConversation(AgentEntity agent, AgentEntity targetAgent) {
        // Set both agents back to idle
        agent.setCurrentState(AgentEntity.AgentState.IDLE);
        agent.getMemory().clearTargetEntity();

        // If target is still in chat state, set them to idle too
        if (targetAgent.getCurrentState() == AgentEntity.AgentState.CHAT_WITH_AGENT) {
            targetAgent.setCurrentState(AgentEntity.AgentState.IDLE);
            targetAgent.getMemory().clearTargetEntity();
        }

        // Update social meters
        agent.getMemory().increaseSocialMeter(2);
        targetAgent.getMemory().increaseSocialMeter(2);

        // Set cooldown to prevent immediate re-greeting
        agent.getMemory().resetSocialCooldown();
        targetAgent.getMemory().resetSocialCooldown();
    }

    /**
     * Makes the agent look at another entity
     */
    private void lookAtEntity(AgentEntity agent, LivingEntity target) {
        Vec3 targetPos = new Vec3(
                target.getX(),
                target.getEyeY(),
                target.getZ()
        );

        Vec3 agentPos = new Vec3(
                agent.getX(),
                agent.getEyeY(),
                agent.getZ()
        );

        Vec3 direction = targetPos.subtract(agentPos).normalize();
        double horizontalDistance = Math.sqrt(direction.x * direction.x + direction.z * direction.z);

        float yaw = (float) (Math.atan2(direction.z, direction.x) * 180.0 / Math.PI) - 90.0F;
        float pitch = (float) -(Math.atan2(direction.y, horizontalDistance) * 180.0 / Math.PI);

        agent.setYRot(yaw);
        agent.setXRot(pitch);
        agent.setYHeadRot(yaw);
    }
}