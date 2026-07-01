package destiny.null_ouroboros.server.registry;

import destiny.null_ouroboros.NullOuroboros;
import destiny.null_ouroboros.server.entity.BurrowBeaconEntity;
import destiny.null_ouroboros.server.entity.FallingAshPileBlockEntity;
import destiny.null_ouroboros.server.entity.FallingDroplightBlockEntity;
import destiny.null_ouroboros.server.entity.DusterbikeEntity;
import destiny.null_ouroboros.server.entity.DusterbikeKeyEntity;
import destiny.null_ouroboros.server.entity.DusterbikeWheelEntity;
import destiny.null_ouroboros.server.entity.RedstickEndEntity;
import destiny.null_ouroboros.server.entity.RedstickEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class EntityRegistry {
    public static DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, NullOuroboros.MODID);

    public static final RegistryObject<EntityType<FallingDroplightBlockEntity>> FALLING_DROPLIGHT =
            ENTITY_TYPES.register("falling_droplight",
                    () -> EntityType.Builder.of(FallingDroplightBlockEntity::new, MobCategory.MISC)
                            .sized(1f, 1f)
                            .build(ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, "falling_droplight").toString()));

    public static final RegistryObject<EntityType<FallingAshPileBlockEntity>> FALLING_ASH_PILE =
            ENTITY_TYPES.register("falling_ash_pile",
                    () -> EntityType.Builder.of(FallingAshPileBlockEntity::new, MobCategory.MISC)
                            .sized(0.98f, 0.98f)
                            .build(ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, "falling_ash_pile").toString()));

    public static final RegistryObject<EntityType<BurrowBeaconEntity>> BURROW_BEACON =
            ENTITY_TYPES.register("burrow_beacon",
                    () -> EntityType.Builder.of(BurrowBeaconEntity::new, MobCategory.MISC)
                            .build(ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, "burrow_beacon").toString()));

    public static final RegistryObject<EntityType<RedstickEntity>> REDSTICK =
            ENTITY_TYPES.register("redstick",
                    () -> EntityType.Builder.<RedstickEntity>of(RedstickEntity::new, MobCategory.MISC)
                            .sized(0.5F, 0.5F)
                            .clientTrackingRange(64)
                            .updateInterval(1)
                            .build(ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, "redstick").toString()));

    public static final RegistryObject<EntityType<RedstickEndEntity>> REDSTICK_END =
            ENTITY_TYPES.register("redstick_end",
                    () -> EntityType.Builder.<RedstickEndEntity>of(RedstickEndEntity::new, MobCategory.MISC)
                            .sized(0.125F, 0.125F)
                            .clientTrackingRange(64)
                            .updateInterval(1)
                            .build(ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, "redstick_end").toString()));

    public static final RegistryObject<EntityType<DusterbikeEntity>> DUSTERBIKE =
            ENTITY_TYPES.register("dusterbike",
                    () -> EntityType.Builder.<DusterbikeEntity>of(DusterbikeEntity::new, MobCategory.MISC)
                            .sized(1.5F, 1.0F)
                            .clientTrackingRange(64)
                            .updateInterval(1)
                            .build(ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, "dusterbike").toString()));

    public static final RegistryObject<EntityType<DusterbikeWheelEntity>> DUSTERBIKE_WHEEL =
            ENTITY_TYPES.register("dusterbike_wheel",
                    () -> EntityType.Builder.<DusterbikeWheelEntity>of(DusterbikeWheelEntity::new, MobCategory.MISC)
                            .sized(1.0F, 1.0F)
                            .clientTrackingRange(64)
                            .updateInterval(1)
                            .build(ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, "dusterbike_wheel").toString()));

    public static final RegistryObject<EntityType<DusterbikeKeyEntity>> DUSTERBIKE_KEY =
            ENTITY_TYPES.register("dusterbike_key",
                    () -> EntityType.Builder.<DusterbikeKeyEntity>of(DusterbikeKeyEntity::new, MobCategory.MISC)
                            .sized(0.125F, 0.25F)
                            .clientTrackingRange(64)
                            .updateInterval(1)
                            .build(ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, "dusterbike_key").toString()));

    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }
}
