package destiny.null_ouroboros.server.registry;

import destiny.null_ouroboros.NullOuroboros;
import destiny.null_ouroboros.common.steel_leviathan.SteelLeviathanConstants;
import destiny.null_ouroboros.server.entity.EngineKeyEntity;
import destiny.null_ouroboros.server.entity.*;
import destiny.null_ouroboros.server.entity.steel_leviathan.*;
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

    public static final RegistryObject<EntityType<DusterbikePartInteractionEntity>> DUSTERBIKE_PART_INTERACTION =
            ENTITY_TYPES.register("dusterbike_part_interaction",
                    () -> EntityType.Builder.<DusterbikePartInteractionEntity>of(DusterbikePartInteractionEntity::new, MobCategory.MISC)
                            .sized(0.5F, 0.5F)
                            .clientTrackingRange(64)
                            .updateInterval(1)
                            .build(ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, "dusterbike_part_interaction").toString()));

    public static final RegistryObject<EntityType<EngineHoistEntity>> ENGINE_HOIST =
            ENTITY_TYPES.register("engine_hoist",
                    () -> EntityType.Builder.<EngineHoistEntity>of(EngineHoistEntity::new, MobCategory.MISC)
                            .build(ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, "engine_hoist").toString()));
    public static final RegistryObject<EntityType<HoistPartInteractionEntity>> HOIST_PART_INTERACTION =
            ENTITY_TYPES.register("hoist_part_interaction",
                    () -> EntityType.Builder.<HoistPartInteractionEntity>of(HoistPartInteractionEntity::new, MobCategory.MISC)
                            .build(ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, "hoist_part_interaction").toString()));

    public static final RegistryObject<EntityType<EngineEntity>> ENGINE =
            ENTITY_TYPES.register("engine",
                    () -> EntityType.Builder.<EngineEntity>of(EngineEntity::new, MobCategory.MISC)
                            .build(ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, "engine").toString()));
    public static final RegistryObject<EntityType<EngineKeyEntity>> ENGINE_KEY =
            ENTITY_TYPES.register("engine_key",
                    () -> EntityType.Builder.<EngineKeyEntity>of(EngineKeyEntity::new, MobCategory.MISC)
                            .build(ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, "engine_key").toString()));

    public static final RegistryObject<EntityType<SteelLeviathanHeadEntity>> STEEL_LEVIATHAN_HEAD =
            ENTITY_TYPES.register("steel_leviathan_head",
                    () -> EntityType.Builder.<SteelLeviathanHeadEntity>of(SteelLeviathanHeadEntity::new, MobCategory.MONSTER)
                            .sized(5.0F, 5.0F)
                            .clientTrackingRange(128)
                            .updateInterval(1)
                            .fireImmune()
                            .build(ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, "steel_leviathan_head").toString()));

    public static final RegistryObject<EntityType<SteelLeviathanSegmentEntity>> STEEL_LEVIATHAN_SEGMENT =
            ENTITY_TYPES.register("steel_leviathan_segment",
                    () -> EntityType.Builder.<SteelLeviathanSegmentEntity>of(SteelLeviathanSegmentEntity::new, MobCategory.MISC)
                            .sized(SteelLeviathanConstants.SEGMENT_WIDTH, SteelLeviathanConstants.SEGMENT_HEIGHT)
                            .clientTrackingRange(128)
                            .updateInterval(1)
                            .fireImmune()
                            .build(ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, "steel_leviathan_segment").toString()));

    public static final RegistryObject<EntityType<SteelLeviathanTailEntity>> STEEL_LEVIATHAN_TAIL =
            ENTITY_TYPES.register("steel_leviathan_tail",
                    () -> EntityType.Builder.<SteelLeviathanTailEntity>of(SteelLeviathanTailEntity::new, MobCategory.MISC)
                            .sized(5.0F, 5.0F)
                            .clientTrackingRange(128)
                            .updateInterval(1)
                            .fireImmune()
                            .build(ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, "steel_leviathan_tail").toString()));

    public static final RegistryObject<EntityType<SteelLeviathanHeatsinkHitboxEntity>> STEEL_LEVIATHAN_HEATSINK =
            ENTITY_TYPES.register("steel_leviathan_heatsink",
                    () -> EntityType.Builder.<SteelLeviathanHeatsinkHitboxEntity>of(SteelLeviathanHeatsinkHitboxEntity::new, MobCategory.MISC)
                            .sized(1.5F, 1.5F)
                            .clientTrackingRange(64)
                            .updateInterval(1)
                            .build(ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, "steel_leviathan_heatsink").toString()));

    public static final RegistryObject<EntityType<BurrowMissileEntity>> BURROW_MISSILE =
            ENTITY_TYPES.register("burrow_missile",
                    () -> EntityType.Builder.<BurrowMissileEntity>of(BurrowMissileEntity::new, MobCategory.MISC)
                            .sized(0.75F, 0.75F)
                            .clientTrackingRange(64)
                            .updateInterval(1)
                            .build(ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, "burrow_missile").toString()));

    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }
}
