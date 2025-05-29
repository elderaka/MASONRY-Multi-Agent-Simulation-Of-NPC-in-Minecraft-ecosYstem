package com.mas.masonry.items;

import com.mas.masonry.AgentEntity;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionHand;
import net.minecraft.network.chat.Component;

public class TaskPaperItem extends Item {

    public TaskPaperItem(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack pStack, Player pPlayer, LivingEntity pInteractionTarget, InteractionHand pUsedHand) {
        if (pInteractionTarget instanceof AgentEntity agent) {
            if (!pPlayer.level().isClientSide()) {
                // Force the agent into PLACE_CONSTRUCTION_BLOCK state
                agent.forceState(AgentEntity.AgentState.PLACE_CONSTRUCTION_BLOCK);
                pPlayer.sendSystemMessage(Component.literal("Tasked " + agent.getName().getString() + " to build."));
                
                // Consume the item
                if (!pPlayer.getAbilities().instabuild) {
                    pStack.shrink(1);
                }
            }
            return InteractionResult.sidedSuccess(pPlayer.level().isClientSide());
        }
        return InteractionResult.PASS;
    }
}
