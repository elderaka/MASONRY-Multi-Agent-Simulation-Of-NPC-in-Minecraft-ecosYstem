package com.mas.masonry.items;

import com.mas.masonry.AgentEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class TaskPaperItem extends Item {

    public TaskPaperItem(Properties pProperties) {
        super(pProperties);
    }

    // Using item damage value as a simple way to store the task type
    private TaskType getCurrentTask(ItemStack stack) {
        int damageValue = stack.getDamageValue();
        return TaskType.fromOrdinal(damageValue);
    }

    private void setCurrentTask(ItemStack stack, TaskType taskType) {
        stack.setDamageValue(taskType.ordinal());
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level pLevel, Player pPlayer, InteractionHand pUsedHand) {
        ItemStack stack = pPlayer.getItemInHand(pUsedHand);
        if (pPlayer.isShiftKeyDown()) {
            if (!pLevel.isClientSide()) {
                TaskType currentTask = getCurrentTask(stack);
                TaskType nextTask = currentTask.next();
                setCurrentTask(stack, nextTask);
                pPlayer.sendSystemMessage(Component.translatable("item.masonry.task_paper.task_selected", nextTask.getDisplayComponent()));
            }
            return InteractionResultHolder.sidedSuccess(stack, pLevel.isClientSide());
        }
        return InteractionResultHolder.pass(stack);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack pStack, Player pPlayer, LivingEntity pInteractionTarget, InteractionHand pUsedHand) {
        if (pInteractionTarget instanceof AgentEntity agent) {
            if (!pPlayer.level().isClientSide()) {
                TaskType selectedTask = getCurrentTask(pStack);
                agent.forceState(selectedTask.getAgentState());
                pPlayer.sendSystemMessage(Component.translatable("item.masonry.task_paper.task_assigned", 
                                                                agent.getName(), 
                                                                selectedTask.getDisplayComponent()));
                
                if (!pPlayer.getAbilities().instabuild) {
                    pStack.shrink(1);
                }
            }
            return InteractionResult.sidedSuccess(pPlayer.level().isClientSide());
        }
        return InteractionResult.PASS;
    }

    @Override
    public Component getName(ItemStack pStack) {
        TaskType currentTask = getCurrentTask(pStack);
        return Component.translatable(this.getDescriptionId(pStack), currentTask.getDisplayComponent());
    }
    
    // Make sure the item is damageable so we can
    public boolean isDamageable(ItemStack stack) {
        return true;
    }

    public int getMaxDamage(ItemStack stack) {
        return TaskType.values().length - 1;
    }
}