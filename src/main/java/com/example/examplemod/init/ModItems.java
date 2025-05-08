package com.example.examplemod.init;

import com.example.examplemod.ExampleMod;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Handles registration of all custom items
 */
public class ModItems {
    public static final DeferredRegister<Item> ITEMS = 
        DeferredRegister.create(ForgeRegistries.ITEMS, ExampleMod.MOD_ID);

    // Spawn egg for the Agent entity
    public static final RegistryObject<Item> AGENT_SPAWN_EGG = ITEMS.register("agent_spawn_egg",
        () -> new SpawnEggItem(
            ModEntities.AGENT.get(),
            0x4A4A4A, // Primary color (dark gray)
            0x8B8B8B, // Secondary color (light gray)
            new Item.Properties()
        ));
} 