package destiny.null_ouroboros;

import com.mojang.logging.LogUtils;
import destiny.null_ouroboros.client.render.dimension.VergeOfRealityDimensionEffects;
import destiny.null_ouroboros.client.render.particle.AshParticle;
import destiny.null_ouroboros.server.registry.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.api.distmarker.Dist;
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

@Mod(NullOuroboros.MODID)
public class NullOuroboros {
    public static final String MODID = "null_ouroboros";
    private static final Logger LOGGER = LogUtils.getLogger();

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

        context.registerConfig(ModConfig.Type.COMMON, Config.SPEC);

        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
    }

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
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
    }
}
