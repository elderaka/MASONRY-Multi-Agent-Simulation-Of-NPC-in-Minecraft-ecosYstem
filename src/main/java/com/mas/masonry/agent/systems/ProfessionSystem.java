
package com.mas.masonry.agent.systems;

import com.mas.masonry.AgentEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import java.util.*;

/**
 * Manages an agent's profession, including tasks, experience, and work behavior
 */
public class ProfessionSystem {
    private Profession currentProfession = Profession.NONE;
    private int professionLevel = 0; // 0-5 like villagers
    private int experience = 0;
    private int workProductivity = 50; // 0-100, affects work speed and quality
    
    // Work-related timers
    private int ticksSinceLastWork = 0;
    private int totalWorkTicks = 0;
    private int consecutiveWorkDays = 0;
    
    // Experience requirements for each level
    private static final int[] EXP_REQUIREMENTS = {0, 10, 70, 150, 250, 350};
    
    public enum Profession {
        NONE(new Block[0], new Item[0], "Unemployed"),
        FARMER(
            new Block[]{Blocks.COMPOSTER, Blocks.FARMLAND, Blocks.WHEAT, Blocks.CARROTS, Blocks.POTATOES},
            new Item[]{Items.WHEAT_SEEDS, Items.CARROT, Items.POTATO, Items.BONE_MEAL, Items.BREAD},
            "Farmer"
        ),
        MINER(
            new Block[]{Blocks.FURNACE, Blocks.BLAST_FURNACE, Blocks.STONE, Blocks.COAL_ORE, Blocks.IRON_ORE},
            new Item[]{Items.IRON_PICKAXE, Items.STONE_PICKAXE, Items.COAL, Items.IRON_INGOT, Items.STONE},
            "Miner"
        ),
        BUILDER(
            new Block[]{Blocks.CRAFTING_TABLE, Blocks.STONECUTTER, Blocks.COBBLESTONE, Blocks.OAK_PLANKS},
            new Item[]{Items.COBBLESTONE, Items.OAK_PLANKS, Items.STONE_BRICKS, Items.OAK_LOG},
            "Builder"
        ),
        LUMBERJACK(
            new Block[]{Blocks.OAK_LOG, Blocks.BIRCH_LOG, Blocks.SPRUCE_LOG, Blocks.JUNGLE_LOG},
            new Item[]{Items.IRON_AXE, Items.STONE_AXE, Items.OAK_LOG, Items.OAK_PLANKS, Items.STICK},
            "Lumberjack"
        ),
        LIBRARIAN(
            new Block[]{Blocks.LECTERN, Blocks.BOOKSHELF, Blocks.ENCHANTING_TABLE},
            new Item[]{Items.BOOK, Items.PAPER, Items.FEATHER, Items.INK_SAC, Items.BOOKSHELF},
            "Librarian"
        ),
        TOOLSMITH(
            new Block[]{Blocks.SMITHING_TABLE, Blocks.ANVIL, Blocks.FURNACE},
            new Item[]{Items.IRON_INGOT, Items.DIAMOND, Items.IRON_PICKAXE, Items.IRON_SWORD, Items.IRON_AXE},
            "Toolsmith"
        );
        
        private final Block[] workBlocks;
        private final Item[] professionItems;
        private final String displayName;
        
        Profession(Block[] workBlocks, Item[] professionItems, String displayName) {
            this.workBlocks = workBlocks;
            this.professionItems = professionItems;
            this.displayName = displayName;
        }
        
        public Block[] getWorkBlocks() { return workBlocks; }
        public Item[] getProfessionItems() { return professionItems; }
        public String getDisplayName() { return displayName; }
        public String getName() {
            return displayName;
        }



        public boolean canWorkWith(Block block) {
            for (Block workBlock : workBlocks) {
                if (workBlock == block) return true;
            }
            return false;
        }
    }
    
    public Profession getCurrentProfession() {
        return currentProfession;
    }
    
    public boolean hasProfession() {
        return currentProfession != Profession.NONE;
    }
    
    public int getProfessionLevel() {
        return professionLevel;
    }
    
    public int getExperience() {
        return experience;
    }
    
    public int getWorkProductivity() {
        return workProductivity;
    }
    
    public String getProfessionDisplayName() {
        if (professionLevel > 0) {
            return getProfessionLevelName() + " " + currentProfession.getDisplayName();
        }
        return currentProfession.getDisplayName();
    }
    
    private String getProfessionLevelName() {
        return switch (professionLevel) {
            case 0 -> "Novice";
            case 1 -> "Apprentice";
            case 2 -> "Journeyman";
            case 3 -> "Expert";
            case 4 -> "Master";
            default -> "Novice";
        };
    }
    
    public boolean setProfession(Profession profession, AgentEntity agent) {
        if (profession == currentProfession) return true;
        
        // Check if agent has the basic requirements for this profession
        if (hasRequirementsForProfession(profession, agent)) {
            this.currentProfession = profession;
            this.professionLevel = 0;
            this.experience = 0;
            this.workProductivity = 30; // Start with basic productivity
            
            // Give basic starting items if needed
            giveStartingItems(profession, agent);
            
            return true;
        }
        
        return false;
    }
    
    private boolean hasRequirementsForProfession(Profession profession, AgentEntity agent) {
        // For now, any agent can take any profession
        // Later we might add requirements like having certain items or being in a village
        return profession != Profession.NONE;
    }
    
    private void giveStartingItems(Profession profession, AgentEntity agent) {
        // Give basic starting tools based on profession
        switch (profession) {
            case FARMER -> {
                addItemToInventory(agent, new ItemStack(Items.WOODEN_HOE));
                addItemToInventory(agent, new ItemStack(Items.WHEAT_SEEDS, 10));
            }
            case MINER -> {
                addItemToInventory(agent, new ItemStack(Items.STONE_PICKAXE));
                addItemToInventory(agent, new ItemStack(Items.TORCH, 16));
            }
            case BUILDER -> {
                addItemToInventory(agent, new ItemStack(Items.STONE_AXE));
                addItemToInventory(agent, new ItemStack(Items.COBBLESTONE, 32));
            }
            case LUMBERJACK -> {
                addItemToInventory(agent, new ItemStack(Items.STONE_AXE));
            }
            case LIBRARIAN -> {
                addItemToInventory(agent, new ItemStack(Items.BOOK, 3));
                addItemToInventory(agent, new ItemStack(Items.PAPER, 10));
            }
            case TOOLSMITH -> {
                addItemToInventory(agent, new ItemStack(Items.IRON_INGOT, 5));
            }
        }
    }
    
    private void addItemToInventory(AgentEntity agent, ItemStack itemStack) {
        // Try to add to agent's inventory
        for (int i = 0; i < agent.getInventory().getContainerSize(); i++) {
            ItemStack slotStack = agent.getInventory().getItem(i);
            if (slotStack.isEmpty()) {
                agent.getInventory().setItem(i, itemStack);
                break;
            }
        }
    }
    
    public void performProfessionTask(AgentEntity agent, BlockPos workSite) {
        if (currentProfession == Profession.NONE || workSite == null) return;
        
        Level level = agent.level();
        BlockState blockState = level.getBlockState(workSite);
        Block block = blockState.getBlock();
        
        // Check if this is a valid work block for the profession
        if (!currentProfession.canWorkWith(block)) {
            // Try to find work-related blocks nearby
            BlockPos nearbyWorkBlock = findNearbyWorkBlock(agent, workSite);
            if (nearbyWorkBlock != null) {
                workSite = nearbyWorkBlock;
                blockState = level.getBlockState(workSite);
                block = blockState.getBlock();
            } else {
                return; // No valid work found
            }
        }
        
        // Perform profession-specific tasks
        switch (currentProfession) {
            case FARMER -> performFarmingTask(agent, workSite, block);
            case MINER -> performMiningTask(agent, workSite, block);
            case BUILDER -> performBuildingTask(agent, workSite, block);
            case LUMBERJACK -> performLumberjackTask(agent, workSite, block);
            case LIBRARIAN -> performLibrarianTask(agent, workSite, block);
            case TOOLSMITH -> performToolsmithTask(agent, workSite, block);
        }
        
        // Track work and gain experience
        ticksSinceLastWork = 0;
        totalWorkTicks++;
        
        // Gain experience periodically
        if (totalWorkTicks % (100 - workProductivity) == 0) {
            addExperience(1);
        }
        
        // Improve productivity over time
        if (totalWorkTicks % 1200 == 0) { // Every minute of work
            workProductivity = Math.min(100, workProductivity + 1);
        }
    }
    
    private void performFarmingTask(AgentEntity agent, BlockPos workSite, Block block) {
        Level level = agent.level();
        
        if (block == Blocks.FARMLAND) {
            // Try to plant crops
            BlockPos cropPos = workSite.above();
            if (level.getBlockState(cropPos).isAir()) {
                // Plant seeds if available
                if (hasItemInInventory(agent, Items.WHEAT_SEEDS)) {
                    level.setBlock(cropPos, Blocks.WHEAT.defaultBlockState(), 3);
                    removeItemFromInventory(agent, Items.WHEAT_SEEDS, 1);
                }
            }
        } else if (block instanceof CropBlock cropBlock) {
            // Harvest mature crops
            if (cropBlock.isMaxAge(level.getBlockState(workSite))) {
                level.destroyBlock(workSite, true);
                // Replant
                level.setBlock(workSite, cropBlock.getStateForAge(0), 3);
            }
        } else if (block == Blocks.COMPOSTER) {
            // Use composter to make bone meal
            // Implementation would depend on specific composter logic
        }
    }
    
    private void performMiningTask(AgentEntity agent, BlockPos workSite, Block block) {
        Level level = agent.level();
        
        if (block == Blocks.STONE || block == Blocks.COAL_ORE || block == Blocks.IRON_ORE) {
            // Mine the block if agent has a pickaxe
            if (hasPickaxeInInventory(agent)) {
                level.destroyBlock(workSite, true);
            }
        } else if (block == Blocks.FURNACE) {
            // Smelt ores if available
            // Implementation would involve furnace interaction
        }
    }
    
    private void performBuildingTask(AgentEntity agent, BlockPos workSite, Block block) {
        Level level = agent.level();
        
        if (block == Blocks.CRAFTING_TABLE) {
            // Craft building materials
            // Implementation would involve crafting logic
        } else if (block == Blocks.STONECUTTER) {
            // Cut stone blocks
            // Implementation would involve stonecutter logic
        }
    }
    
    private void performLumberjackTask(AgentEntity agent, BlockPos workSite, Block block) {
        Level level = agent.level();
        
        if (block instanceof RotatedPillarBlock && (
            block == Blocks.OAK_LOG || block == Blocks.BIRCH_LOG || 
            block == Blocks.SPRUCE_LOG || block == Blocks.JUNGLE_LOG)) {
            
            // Chop down the tree if agent has an axe
            if (hasAxeInInventory(agent)) {
                level.destroyBlock(workSite, true);
                // Try to find more logs above
                for (int y = 1; y <= 10; y++) {
                    BlockPos logPos = workSite.above(y);
                    if (level.getBlockState(logPos).getBlock() == block) {
                        level.destroyBlock(logPos, true);
                    } else {
                        break;
                    }
                }
            }
        }
    }
    
    private void performLibrarianTask(AgentEntity agent, BlockPos workSite, Block block) {
        // Organize books, enchant items, etc.
        // Implementation would involve book and enchanting logic
    }
    
    private void performToolsmithTask(AgentEntity agent, BlockPos workSite, Block block) {
        // Craft tools, repair items, etc.
        // Implementation would involve smithing logic
    }
    
    private BlockPos findNearbyWorkBlock(AgentEntity agent, BlockPos center) {
        Level level = agent.level();
        int radius = 8;
        
        for (int x = -radius; x <= radius; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos checkPos = center.offset(x, y, z);
                    Block block = level.getBlockState(checkPos).getBlock();
                    
                    if (currentProfession.canWorkWith(block)) {
                        return checkPos;
                    }
                }
            }
        }
        
        return null;
    }
    
    private boolean hasItemInInventory(AgentEntity agent, Item item) {
        for (int i = 0; i < agent.getInventory().getContainerSize(); i++) {
            ItemStack stack = agent.getInventory().getItem(i);
            if (stack.getItem() == item) {
                return true;
            }
        }
        return false;
    }
    
    private void removeItemFromInventory(AgentEntity agent, Item item, int count) {
        for (int i = 0; i < agent.getInventory().getContainerSize(); i++) {
            ItemStack stack = agent.getInventory().getItem(i);
            if (stack.getItem() == item) {
                stack.shrink(count);
                if (stack.isEmpty()) {
                    agent.getInventory().setItem(i, ItemStack.EMPTY);
                }
                return;
            }
        }
    }
    
    private boolean hasPickaxeInInventory(AgentEntity agent) {
        return hasItemInInventory(agent, Items.WOODEN_PICKAXE) ||
               hasItemInInventory(agent, Items.STONE_PICKAXE) ||
               hasItemInInventory(agent, Items.IRON_PICKAXE) ||
               hasItemInInventory(agent, Items.DIAMOND_PICKAXE);
    }
    
    private boolean hasAxeInInventory(AgentEntity agent) {
        return hasItemInInventory(agent, Items.WOODEN_AXE) ||
               hasItemInInventory(agent, Items.STONE_AXE) ||
               hasItemInInventory(agent, Items.IRON_AXE) ||
               hasItemInInventory(agent, Items.DIAMOND_AXE);
    }
    
    public void addExperience(int amount) {
        this.experience += amount;
        
        // Check for level up
        while (canLevelUp()) {
            levelUp();
        }
    }
    
    private boolean canLevelUp() {
        return professionLevel < 4 && experience >= EXP_REQUIREMENTS[professionLevel + 1];
    }
    
    private void levelUp() {
        if (professionLevel < 4) {
            professionLevel++;
            workProductivity = Math.min(100, workProductivity + 10);
            
            // Could add profession-specific benefits here
            // Like increased work speed, new abilities, etc.
        }
    }
    
    public int getRequiredExperienceForNextLevel() {
        if (professionLevel >= 4) return -1; // Max level
        return EXP_REQUIREMENTS[professionLevel + 1];
    }
    
    public void tick(AgentEntity agent) {
        ticksSinceLastWork++;
        
        // Lose some productivity if not working for a long time
        if (ticksSinceLastWork > 24000) { // 1 day without work
            workProductivity = Math.max(30, workProductivity - 1);
        }
        
        // Track consecutive work days for bonuses
        if (ticksSinceLastWork < 1000) { // Worked recently
            // Consider this a work day
        } else if (ticksSinceLastWork > 24000) {
            consecutiveWorkDays = 0;
        }
    }
    
    public boolean shouldWork(ScheduleSystem.AgentActivity currentActivity) {
        return currentActivity == ScheduleSystem.AgentActivity.WORK && hasProfession();
    }
    
    public double getWorkEfficiency() {
        // Base efficiency affected by level and productivity
        double base = 0.5 + (professionLevel * 0.1) + (workProductivity / 200.0);
        
        // Bonus for consecutive work days
        double consecutiveBonus = Math.min(0.2, consecutiveWorkDays * 0.02);
        
        return Math.min(1.0, base + consecutiveBonus);
    }
}