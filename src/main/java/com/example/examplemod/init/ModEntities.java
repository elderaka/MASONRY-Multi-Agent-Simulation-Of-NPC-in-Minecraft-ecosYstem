package com.example.examplemod.init;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.entity.EntityAgent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Handles registration of all custom entities
 */
public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = 
        DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, ExampleMod.MOD_ID);

    public static final RegistryObject<EntityType<EntityAgent>> AGENT = ENTITIES.register("agent",
        () -> EntityType.Builder.of(EntityAgent::new, MobCategory.CREATURE)
            .sized(0.6f, 1.8f) // Size of the entity (width, height)
            .build("agent"));
} 