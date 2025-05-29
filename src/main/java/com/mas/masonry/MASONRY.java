package com.mas.masonry;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;

import java.util.List;
import java.util.Arrays;
import java.util.Random;

import com.mas.masonry.AgentEntity; // Assuming AgentEntity is in this package
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import com.mas.masonry.items.TaskPaperItem;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(MASONRY.MODID)
@EventBusSubscriber(modid = MASONRY.MODID, bus = EventBusSubscriber.Bus.MOD)
public class MASONRY
{
    // Define mod id in a common place for everything to reference
    public static final String MODID = "masonry";
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();
    // Create a Deferred Register to hold Blocks which will all be registered under the "examplemod" namespace
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
    // Create a Deferred Register to hold Items which will all be registered under the "examplemod" namespace
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    // Create a Deferred Register to hold CreativeModeTabs which will all be registered under the "examplemod" namespace
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);
    // Create a Deferred Register to hold EntityTypes
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, MODID);

    // List of names for AgentEntities
    private static final List<String> AGENT_NAMES = Arrays.asList(
            "Monika", "Mita", "Flowey", "Sans", "Deadpool", 
            "GLaDOS", "Stanley", "PsychoMantis", "TheScribe", "Dimentio"
    );
    private static final Random RANDOM = new Random();

    // --- Blueprint Definitions ---
    public static class BlueprintBlock {
        public final BlockPos relativePos; // Relative to the blueprint's origin
        public final Block blockType;      // Type of block to place

        public BlueprintBlock(BlockPos relativePos, Block blockType) {
            this.relativePos = relativePos;
            this.blockType = blockType;
        }
    }

    public static final List<BlueprintBlock> SIMPLE_WALL_BLUEPRINT = List.of(
        new BlueprintBlock(new BlockPos(0, 0, 0), Blocks.OAK_PLANKS),
        new BlueprintBlock(new BlockPos(1, 0, 0), Blocks.OAK_PLANKS),
        new BlueprintBlock(new BlockPos(2, 0, 0), Blocks.OAK_PLANKS)
    );
    // --- End Blueprint Definitions ---

    // Creates a new Block with the id "examplemod:example_block", combining the namespace and path
    public static final RegistryObject<Block> EXAMPLE_BLOCK = BLOCKS.register("example_block", () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.STONE)));
    // Creates a new BlockItem with the id "examplemod:example_block", combining the namespace and path
    public static final RegistryObject<Item> EXAMPLE_BLOCK_ITEM = ITEMS.register("example_block", () -> new BlockItem(EXAMPLE_BLOCK.get(), new Item.Properties()));

    // Creates a new food item with the id "examplemod:example_id", nutrition 1 and saturation 2
    public static final RegistryObject<Item> EXAMPLE_ITEM = ITEMS.register("example_item", () -> new Item(new Item.Properties().food(new FoodProperties.Builder()
            .alwaysEdible().nutrition(1).saturationModifier(2f).build())));

    // Register Agent Entity Type
    public static final RegistryObject<EntityType<AgentEntity>> AGENT_ENTITY = ENTITY_TYPES.register("agent",
            () -> EntityType.Builder.of(AgentEntity::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.8F) // Standard player-like size, adjust if needed
                    .build(ResourceLocation.fromNamespaceAndPath(MODID, "agent").toString()));

    // Register Agent Spawn Egg
    // Placeholder colors: 0x6B4226 (brown), 0xA0A0A0 (grey)
    public static final RegistryObject<Item> AGENT_SPAWN_EGG = ITEMS.register("agent_spawn_egg",
            () -> new ForgeSpawnEggItem(AGENT_ENTITY, 0x6B4226, 0xA0A0A0, new Item.Properties()));

    // Register Task Paper Item
    public static final RegistryObject<Item> TASK_PAPER = ITEMS.register("task_paper",
            () -> new TaskPaperItem(new Item.Properties()));

    // Register Creative Mode Tab
    public static final RegistryObject<CreativeModeTab> MASONRY_TAB = CREATIVE_MODE_TABS.register("masonry_tab",
            () -> CreativeModeTab.builder().icon(() -> new ItemStack(AGENT_SPAWN_EGG.get()))
                    .title(Component.translatable("creativetab.masonry_tab"))
                    .displayItems((parameters, output) -> {
                        output.accept(AGENT_SPAWN_EGG.get()); // Add spawn egg
                        output.accept(TASK_PAPER.get()); // Add task paper
                        // Add other mod items here for the tab
                        output.accept(EXAMPLE_BLOCK_ITEM.get());
                        output.accept(EXAMPLE_ITEM.get());
                    })
                    .build());

    // Creates a creative tab with the id "examplemod:example_tab" for the example item, that is placed after the combat tab
    public static final RegistryObject<CreativeModeTab> EXAMPLE_TAB = CREATIVE_MODE_TABS.register("example_tab", () -> CreativeModeTab.builder()
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .icon(() -> EXAMPLE_ITEM.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(EXAMPLE_ITEM.get()); // Add the example item to the tab. For your own tabs, this method is preferred over the event
            }).build());

    public MASONRY(FMLJavaModLoadingContext context)
    {
        IEventBus modEventBus = context.getModEventBus();

        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        // Register the Deferred Register to the mod event bus so blocks get registered
        BLOCKS.register(modEventBus);
        // Register the Deferred Register to the mod event bus so items get registered
        ITEMS.register(modEventBus);
        // Register the Deferred Register to the mod event bus so tabs get registered
        CREATIVE_MODE_TABS.register(modEventBus);
        ENTITY_TYPES.register(modEventBus);

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);

        // Register the item to a creative tab
        modEventBus.addListener(this::addCreative);

        // Register our mod's ForgeConfigSpec so that Forge can create and load the config file for us
        context.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event)
    {
        // Some common setup code
        LOGGER.info("HELLO FROM COMMON SETUP");

        if (Config.logDirtBlock)
            LOGGER.info("DIRT BLOCK >> {}", ForgeRegistries.BLOCKS.getKey(Blocks.DIRT));

        LOGGER.info(Config.magicNumberIntroduction + Config.magicNumber);

        Config.items.forEach((item) -> LOGGER.info("ITEM >> {}", item.toString()));
    }

    // Add the example block item to the building blocks tab
    private void addCreative(BuildCreativeModeTabContentsEvent event)
    {
        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS)
            event.accept(EXAMPLE_BLOCK_ITEM);
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event)
    {
        // Do something when the server starts
        LOGGER.info("HELLO from server starting");
    }

    // You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents
    {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event)
        {
            // Some client setup code
            LOGGER.info("HELLO FROM CLIENT SETUP");
            LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());

            // Register AgentEntity renderer
            EntityRenderers.register(MASONRY.AGENT_ENTITY.get(), (EntityRendererProvider.Context renderContext) -> {
                return new HumanoidMobRenderer<AgentEntity, HumanoidModel<AgentEntity>>(renderContext, new HumanoidModel<>(renderContext.bakeLayer(ModelLayers.PLAYER)), 0.5F) {
                    private static final ResourceLocation AGENT_TEXTURE = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/steve.png"); // Default Steve texture

                    @Override
                    public ResourceLocation getTextureLocation(AgentEntity entity) {
                        return AGENT_TEXTURE;
                    }
                };
            });
            // Note: This makes your agent look like Steve. To use a custom texture:
            // 1. Create your texture file (e.g., assets/masonry/textures/entity/agent.png).
            // 2. Update AGENT_TEXTURE to: ResourceLocation.fromNamespaceAndPath(MASONRY.MODID, "textures/entity/agent.png");
            // To use a custom model, you'd also need to create a model class, register its ModelLayerLocation, and use it here.
        }
    }

    @SubscribeEvent
    public static void onRegisterEntityAttributes(EntityAttributeCreationEvent event) {
        event.put(AGENT_ENTITY.get(), AgentEntity.createAttributes().build());
    }

    public static String getRandomAgentName() {
        if (AGENT_NAMES.isEmpty()) {
            return "Agent"; // Fallback name
        }
        return AGENT_NAMES.get(RANDOM.nextInt(AGENT_NAMES.size()));
    }
}
