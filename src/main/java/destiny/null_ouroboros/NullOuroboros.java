package destiny.null_ouroboros;

import com.mojang.logging.LogUtils;
import destiny.null_ouroboros.client.render.animation.HeavyRevolverPlayerAnimation;
import destiny.null_ouroboros.client.render.animation.RespiratorAnimation;
import destiny.null_ouroboros.client.render.blockentity.*;
import destiny.null_ouroboros.client.render.dimension.VergeOfRealityDimensionEffects;
import destiny.null_ouroboros.client.render.entity.*;
import destiny.null_ouroboros.client.render.model.*;
import destiny.null_ouroboros.client.render.entity.steel_leviathan.BurrowMissileGeoRenderer;
import destiny.null_ouroboros.client.render.entity.steel_leviathan.SteelLeviathanPartGeoRenderer;
import destiny.null_ouroboros.client.render.particle.AshParticle;
import destiny.null_ouroboros.client.render.particle.BloodParticle;
import destiny.null_ouroboros.client.render.particle.TintedSmokeParticle;
import destiny.null_ouroboros.client.render.player_anim.PlayerAnimationRegistry;
import destiny.null_ouroboros.client.screen.DustyComputerScreen;
import destiny.null_ouroboros.client.screen.FuseBoxScreen;
import destiny.null_ouroboros.common.player_anim.HeavyRevolverPlayerAnims;
import destiny.null_ouroboros.common.player_anim.RespiratorPlayerAnims;
import destiny.null_ouroboros.server.registry.*;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.renderer.entity.FallingBlockRenderer;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterDimensionSpecialEffectsEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

@Mod(NullOuroboros.MODID)
public class NullOuroboros {
    public static final String MODID = "null_ouroboros";
    public static final Logger LOGGER = LogUtils.getLogger();

    public NullOuroboros(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();

        ItemRegistry.ITEMS.register(modEventBus);
        BlockRegistry.BLOCKS.register(modEventBus);
        CreativeTabRegistry.DEF_REG.register(modEventBus);
        FeatureRegistry.FEATURES.register(modEventBus);
        FeatureRegistry.TRUNKS.register(modEventBus);
        FeatureRegistry.TREE_DECORATORS.register(modEventBus);
        PlacementRegistry.PLACEMENT_MODIFIERS.register(modEventBus);
        ParticleTypeRegistry.PARTICLE_TYPES.register(modEventBus);
        PacketHandlerRegistry.register();
        RecipeRegistry.RECIPE_SERIALIZERS.register(modEventBus);
        BlockEntityRegistry.BLOCK_ENTITIES.register(modEventBus);
        SoundRegistry.SOUNDS.register(modEventBus);
        EntityRegistry.ENTITY_TYPES.register(modEventBus);
        MenuRegistry.MENUS.register(modEventBus);
        FluidRegistry.register(modEventBus);
        FluidTypeRegistry.register(modEventBus);
        HeavyRevolverPlayerAnims.registerMeta();
        RespiratorPlayerAnims.registerMeta();

        context.registerConfig(ModConfig.Type.COMMON, Config.SPEC);

        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
    }

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {

        @SubscribeEvent
        public static void bakeModels(EntityRenderersEvent.RegisterLayerDefinitions event) {
            event.registerLayerDefinition(StrobelightBlockModel.LAYER_LOCATION, StrobelightBlockModel::createBodyLayer);
            event.registerLayerDefinition(MechanicalSirenBlockModel.LAYER_LOCATION, MechanicalSirenBlockModel::createBodyLayer);
            event.registerLayerDefinition(TemporalSurgeDetectorBlockModel.LAYER_LOCATION, TemporalSurgeDetectorBlockModel::createBodyLayer);
            event.registerLayerDefinition(BurrowBeaconEntityModel.LAYER_LOCATION, BurrowBeaconEntityModel::createBodyLayer);
            event.registerLayerDefinition(RedstickEntityModel.LAYER_LOCATION, RedstickEntityModel::createBodyLayer);
            event.registerLayerDefinition(RedstickEndEntityModel.LAYER_LOCATION, RedstickEndEntityModel::createBodyLayer);
            event.registerLayerDefinition(ElectromagneticAssemblyBlockModel.LAYER_LOCATION, ElectromagneticAssemblyBlockModel::createBodyLayer);
            event.registerLayerDefinition(LiquidatorArmorModel.LAYER_LOCATION, LiquidatorArmorModel::createBodyLayer);
            event.registerLayerDefinition(RespiratorModel.LAYER_LOCATION, RespiratorModel::createBodyLayer);
        }

        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            event.enqueueWork(() -> {
                PlayerAnimationRegistry.register(HeavyRevolverPlayerAnims.HOLD_ID, HeavyRevolverPlayerAnimation.revolver_hold);
                PlayerAnimationRegistry.register(HeavyRevolverPlayerAnims.SHOOT_ID, HeavyRevolverPlayerAnimation.revolver_shoot);
                PlayerAnimationRegistry.register(HeavyRevolverPlayerAnims.COCK_ID, HeavyRevolverPlayerAnimation.revolver_cock);
                PlayerAnimationRegistry.register(HeavyRevolverPlayerAnims.DECOCK_ID, HeavyRevolverPlayerAnimation.revolver_decock);
                PlayerAnimationRegistry.register(HeavyRevolverPlayerAnims.CYLINDER_OUT_ID, HeavyRevolverPlayerAnimation.revolver_cylinder_out);
                PlayerAnimationRegistry.register(HeavyRevolverPlayerAnims.CYLINDER_IN_ID, HeavyRevolverPlayerAnimation.revolver_cylinder_in);
                PlayerAnimationRegistry.register(HeavyRevolverPlayerAnims.CYLINDER_EJECT_ID, HeavyRevolverPlayerAnimation.revolver_cylinder_eject);
                PlayerAnimationRegistry.register(HeavyRevolverPlayerAnims.CYLINDER_TAKE_ID, HeavyRevolverPlayerAnimation.revolver_cylinder_take);
                PlayerAnimationRegistry.register(HeavyRevolverPlayerAnims.CYLINDER_PUT_ID, HeavyRevolverPlayerAnimation.revolver_cylinder_put);
                PlayerAnimationRegistry.register(HeavyRevolverPlayerAnims.CYLINDER_ROTATE_ID, HeavyRevolverPlayerAnimation.revolver_cylinder_rotate);
                PlayerAnimationRegistry.register(HeavyRevolverPlayerAnims.CYLINDER_SPEEDLOADER_ID, HeavyRevolverPlayerAnimation.revolver_cylinder_speedloader);
                PlayerAnimationRegistry.register(RespiratorPlayerAnims.FILTER_REMOVE_RIGHT_ID, RespiratorAnimation.filter_remove_right);
                PlayerAnimationRegistry.register(RespiratorPlayerAnims.FILTER_PUT_RIGHT_ID, RespiratorAnimation.filter_put_right);
                PlayerAnimationRegistry.register(RespiratorPlayerAnims.FILTER_REMOVE_LEFT_ID, RespiratorAnimation.filter_remove_left);
                PlayerAnimationRegistry.register(RespiratorPlayerAnims.FILTER_PUT_LEFT_ID, RespiratorAnimation.filter_put_left);
                EntityRenderers.register(EntityRegistry.FALLING_DROPLIGHT.get(), FallingBlockRenderer::new);
                EntityRenderers.register(EntityRegistry.FALLING_ASH_PILE.get(), FallingBlockRenderer::new);
                EntityRenderers.register(EntityRegistry.BURROW_BEACON.get(), BurrowBeaconEntityRenderer::new);
                EntityRenderers.register(EntityRegistry.REDSTICK.get(), RedstickEntityRenderer::new);
                EntityRenderers.register(EntityRegistry.REDSTICK_END.get(), RedstickEndEntityRenderer::new);
                EntityRenderers.register(EntityRegistry.DUSTERBIKE.get(), DusterbikeGeoRenderer::new);
                EntityRenderers.register(EntityRegistry.DUSTERBIKE_WHEEL.get(), DusterbikeWheelEntityRenderer::new);
                EntityRenderers.register(EntityRegistry.DUSTERBIKE_KEY.get(), InvisibleEntityRenderer::new);
                EntityRenderers.register(EntityRegistry.DUSTERBIKE_PART_INTERACTION.get(), InvisibleEntityRenderer::new);
                EntityRenderers.register(EntityRegistry.ENGINE_HOIST.get(), EngineHoistGeoRenderer::new);
                EntityRenderers.register(EntityRegistry.ENGINE.get(), EngineGeoRenderer::new);
                EntityRenderers.register(EntityRegistry.ENGINE_KEY.get(), InvisibleEntityRenderer::new);
                EntityRenderers.register(EntityRegistry.HOIST_PART_INTERACTION.get(), InvisibleEntityRenderer::new);
                EntityRenderers.register(EntityRegistry.STEEL_LEVIATHAN_HEAD.get(), ctx -> new SteelLeviathanPartGeoRenderer(ctx, "head"));
                EntityRenderers.register(EntityRegistry.STEEL_LEVIATHAN_SEGMENT.get(), ctx -> new SteelLeviathanPartGeoRenderer(ctx, "segment"));
                EntityRenderers.register(EntityRegistry.STEEL_LEVIATHAN_TAIL.get(), ctx -> new SteelLeviathanPartGeoRenderer(ctx, "tail"));
                EntityRenderers.register(EntityRegistry.STEEL_LEVIATHAN_HEATSINK.get(), InvisibleEntityRenderer::new);
                EntityRenderers.register(EntityRegistry.BURROW_MISSILE.get(), BurrowMissileGeoRenderer::new);
                EntityRenderers.register(EntityRegistry.BURROW_MISSILE_DRILL.get(), InvisibleEntityRenderer::new);
                EntityRenderers.register(EntityRegistry.BULLET.get(), BulletEntityRenderer::new);
                EntityRenderers.register(EntityRegistry.CARTRIDGE.get(), CartridgeGeoRenderer::new);
                MenuScreens.register(MenuRegistry.DUSTY_COMPUTER_MENU.get(), DustyComputerScreen::new);
                MenuScreens.register(MenuRegistry.FUSE_BOX_MENU.get(), FuseBoxScreen::new);
                ItemProperties.register(ItemRegistry.BIKE_KEY.get(), ResourceLocation.fromNamespaceAndPath(MODID, "is_colored"),
                        (stack, level, entity, seed) -> stack.getTag() != null && stack.getTag().contains("display") ? 1f : 0f);
                ItemProperties.register(ItemRegistry.SPRAY_CAN.get(), ResourceLocation.fromNamespaceAndPath(MODID, "is_colored"),
                        (stack, level, entity, seed) -> stack.getTag() != null && stack.getTag().contains("display") ? 1f : 0f);
            });
        }

        @SubscribeEvent
        public static void registerDimensionEffects(RegisterDimensionSpecialEffectsEvent event) {
            VergeOfRealityDimensionEffects vergeOfRealityDimensionEffects = new VergeOfRealityDimensionEffects();

            event.register(VergeOfRealityDimensionEffects.VERGE_OF_REALITY_DIMENSION_EFFECTS, vergeOfRealityDimensionEffects);
        }

        @SubscribeEvent
        public static void registerParticleProvider(RegisterParticleProvidersEvent event) {
            event.registerSpriteSet(ParticleTypeRegistry.ASH.get(), AshParticle.Provider::new);
            event.registerSpriteSet(ParticleTypeRegistry.TINTED_SMOKE.get(), TintedSmokeParticle.Provider::new);
            event.registerSpriteSet(ParticleTypeRegistry.BLOOD.get(), BloodParticle.Provider::new);
        }

        @SubscribeEvent
        public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerBlockEntityRenderer(BlockEntityRegistry.STROBELIGHT_BLOCK_ENTITY.get(), StrobelightBlockEntityRenderer::new);
            event.registerBlockEntityRenderer(BlockEntityRegistry.MECHANICAL_SIREN_BLOCK_ENTITY.get(), MechanicalSirenBlockEntityRenderer::new);
            event.registerBlockEntityRenderer(BlockEntityRegistry.TEMPORAL_SURGE_DETECTOR_BLOCK_ENTITY.get(), TemporalSurgeDetectorBlockEntityRenderer::new);
            event.registerBlockEntityRenderer(BlockEntityRegistry.ELECTROMAGNETIC_ASSEMBLY_BLOCK_ENTITY.get(), ElectromagneticAssemblyBlockEntityRenderer::new);
            event.registerBlockEntityRenderer(BlockEntityRegistry.FUSE_BOX_BLOCK_ENTITY.get(), context -> new FuseBoxGeoBlockEntityRenderer());
            event.registerBlockEntityRenderer(BlockEntityRegistry.BULKHEAD_BLOCK_ENTITY.get(), context -> new BulkheadGeoBlockEntityRenderer());
            event.registerBlockEntityRenderer(BlockEntityRegistry.VENTILATION_SHAFT_BLOCK_ENTITY.get(), CamouflageBlockEntityRenderer::new);
            event.registerBlockEntityRenderer(BlockEntityRegistry.VENTILATION_ROUTER_BLOCK_ENTITY.get(), CamouflageBlockEntityRenderer::new);
            event.registerBlockEntityRenderer(BlockEntityRegistry.ABANDONED_DUSTERBIKE_SPAWNER_BLOCK_ENTITY.get(), AbandonedDusterbikeSpawnerRenderer::new);
        }
    }
}
