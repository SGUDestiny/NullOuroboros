package destiny.null_ouroboros;

import com.mojang.logging.LogUtils;
import destiny.null_ouroboros.client.render.blockentity.ElectromagneticAssemblyBlockEntityRenderer;
import destiny.null_ouroboros.client.render.blockentity.MechanicalSirenBlockEntityRenderer;
import destiny.null_ouroboros.client.render.blockentity.StrobelightBlockEntityRenderer;
import destiny.null_ouroboros.client.render.blockentity.TemporalSurgeDetectorBlockEntityRenderer;
import destiny.null_ouroboros.client.render.dimension.VergeOfRealityDimensionEffects;
import destiny.null_ouroboros.client.render.entity.BurrowBeaconEntityRenderer;
import destiny.null_ouroboros.client.render.model.*;
import destiny.null_ouroboros.client.render.particle.AshParticle;
import destiny.null_ouroboros.client.screen.DustyComputerScreen;
import destiny.null_ouroboros.server.registry.*;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.renderer.entity.FallingBlockRenderer;
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
        ParticleTypeRegistry.PARTICLE_TYPES.register(modEventBus);
        PacketHandlerRegistry.register();
        BlockEntityRegistry.BLOCK_ENTITIES.register(modEventBus);
        SoundRegistry.SOUNDS.register(modEventBus);
        EntityRegistry.ENTITY_TYPES.register(modEventBus);
        MenuRegistry.MENUS.register(modEventBus);

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
            event.registerLayerDefinition(ElectromagneticAssemblyBlockModel.LAYER_LOCATION, ElectromagneticAssemblyBlockModel::createBodyLayer);
        }

        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            event.enqueueWork(() -> {
                        EntityRenderers.register(EntityRegistry.FALLING_DROPLIGHT.get(), FallingBlockRenderer::new);
                        EntityRenderers.register(EntityRegistry.FALLING_ASH_PILE.get(), FallingBlockRenderer::new);
                        EntityRenderers.register(EntityRegistry.BURROW_BEACON.get(), BurrowBeaconEntityRenderer::new);
                        MenuScreens.register(MenuRegistry.DUSTY_COMPUTER_MENU.get(), DustyComputerScreen::new);
            });

            try {
                Field maxSourcesField = SoundSource.class.getDeclaredField("maxSources");
                maxSourcesField.setAccessible(true);

                Field modifiersField = Field.class.getDeclaredField("modifiers");
                modifiersField.setAccessible(true);
                modifiersField.setInt(maxSourcesField, maxSourcesField.getModifiers() & ~Modifier.FINAL);

                maxSourcesField.setInt(SoundSource.AMBIENT, 128);
                maxSourcesField.setInt(SoundSource.WEATHER, 128);
            } catch (Exception e) {
                NullOuroboros.LOGGER.warn("Could not increase per-category sound limits; streaming limit is handled via LibraryMixin.", e);
            }
        }

        @SubscribeEvent
        public static void registerDimensionEffects(RegisterDimensionSpecialEffectsEvent event) {
            VergeOfRealityDimensionEffects vergeOfRealityDimensionEffects = new VergeOfRealityDimensionEffects();

            event.register(VergeOfRealityDimensionEffects.VERGE_OF_REALITY_DIMENSION_EFFECTS, vergeOfRealityDimensionEffects);
        }

        @SubscribeEvent
        public static void registerParticleProvider(RegisterParticleProvidersEvent event) {
            event.registerSpriteSet(ParticleTypeRegistry.ASH.get(), AshParticle.Provider::new);
        }

        @SubscribeEvent
        public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerBlockEntityRenderer(BlockEntityRegistry.STROBELIGHT_BLOCK_ENTITY.get(), StrobelightBlockEntityRenderer::new);
            event.registerBlockEntityRenderer(BlockEntityRegistry.MECHANICAL_SIREN_BLOCK_ENTITY.get(), MechanicalSirenBlockEntityRenderer::new);
            event.registerBlockEntityRenderer(BlockEntityRegistry.TEMPORAL_SURGE_DETECTOR_BLOCK_ENTITY.get(), TemporalSurgeDetectorBlockEntityRenderer::new);
            event.registerBlockEntityRenderer(BlockEntityRegistry.ELECTROMAGNETIC_ASSEMBLY_BLOCK_ENTITY.get(), ElectromagneticAssemblyBlockEntityRenderer::new);
        }
    }
}
