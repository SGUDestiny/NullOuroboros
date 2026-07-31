package destiny.null_ouroboros.server.registry;

import destiny.null_ouroboros.NullOuroboros;
import destiny.null_ouroboros.server.block.entity.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class BlockEntityRegistry {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, NullOuroboros.MODID);

    public static final RegistryObject<BlockEntityType<StrobelightBlockEntity>> STROBELIGHT_BLOCK_ENTITY = BLOCK_ENTITIES.register("strobelight", () -> BlockEntityType.Builder.of(StrobelightBlockEntity::new, BlockRegistry.STROBELIGHT.get()).build(null));
    public static final RegistryObject<BlockEntityType<MechanicalSirenBlockEntity>> MECHANICAL_SIREN_BLOCK_ENTITY = BLOCK_ENTITIES.register("mechanical_siren", () -> BlockEntityType.Builder.of(MechanicalSirenBlockEntity::new, BlockRegistry.MECHANICAL_SIREN.get()).build(null));
    public static final RegistryObject<BlockEntityType<TemporalSurgeDetectorBlockEntity>> TEMPORAL_SURGE_DETECTOR_BLOCK_ENTITY = BLOCK_ENTITIES.register("temporal_surge_detector", () -> BlockEntityType.Builder.of(TemporalSurgeDetectorBlockEntity::new, BlockRegistry.TEMPORAL_SURGE_DETECTOR.get()).build(null));
    public static final RegistryObject<BlockEntityType<DustyComputerBlockEntity>> DUSTY_COMPUTER_BLOCK_ENTITY = BLOCK_ENTITIES.register("dusty_computer", () -> BlockEntityType.Builder.of(DustyComputerBlockEntity::new, BlockRegistry.DUSTY_COMPUTER.get()).build(null));
    public static final RegistryObject<BlockEntityType<ElectromagneticAssemblyBlockEntity>> ELECTROMAGNETIC_ASSEMBLY_BLOCK_ENTITY = BLOCK_ENTITIES.register("electromagnetic_assembly", () -> BlockEntityType.Builder.of(ElectromagneticAssemblyBlockEntity::new, BlockRegistry.ELECTROMAGNETIC_ASSEMBLY.get()).build(null));
    public static final RegistryObject<BlockEntityType<FuseBoxBlockEntity>> FUSE_BOX_BLOCK_ENTITY = BLOCK_ENTITIES.register("fuse_box", () -> BlockEntityType.Builder.of(FuseBoxBlockEntity::new, BlockRegistry.FUSE_BOX.get()).build(null));
    public static final RegistryObject<BlockEntityType<BulkheadBlockEntity>> BULKHEAD_BLOCK_ENTITY = BLOCK_ENTITIES.register("bulkhead", () -> BlockEntityType.Builder.of(BulkheadBlockEntity::new, BlockRegistry.BULKHEAD.get()).build(null));
    public static final RegistryObject<BlockEntityType<GarageDoorBlockEntity>> GARAGE_DOOR_BLOCK_ENTITY = BLOCK_ENTITIES.register("garage_door", () -> BlockEntityType.Builder.of(GarageDoorBlockEntity::new, BlockRegistry.GARAGE_DOOR.get()).build(null));
    public static final RegistryObject<BlockEntityType<IntakeFanBlockEntity>> INTAKE_FAN_BLOCK_ENTITY = BLOCK_ENTITIES.register("intake_fan", () -> BlockEntityType.Builder.of(IntakeFanBlockEntity::new, BlockRegistry.INTAKE_FAN.get()).build(null));
    public static final RegistryObject<BlockEntityType<OutputVentBlockEntity>> OUTPUT_VENT_BLOCK_ENTITY = BLOCK_ENTITIES.register("output_vent", () -> BlockEntityType.Builder.of(OutputVentBlockEntity::new, BlockRegistry.OUTPUT_VENT.get()).build(null));
    public static final RegistryObject<BlockEntityType<VentilationShaftBlockEntity>> VENTILATION_SHAFT_BLOCK_ENTITY = BLOCK_ENTITIES.register("ventilation_shaft", () -> BlockEntityType.Builder.of(VentilationShaftBlockEntity::new, BlockRegistry.VENTILATION_SHAFT.get()).build(null));
    public static final RegistryObject<BlockEntityType<VentilationRouterBlockEntity>> VENTILATION_ROUTER_BLOCK_ENTITY = BLOCK_ENTITIES.register("ventilation_router", () -> BlockEntityType.Builder.of(VentilationRouterBlockEntity::new, BlockRegistry.VENTILATION_ROUTER.get()).build(null));
    public static final RegistryObject<BlockEntityType<AbandonedDusterbikeSpawnerBlockEntity>> ABANDONED_DUSTERBIKE_SPAWNER_BLOCK_ENTITY = BLOCK_ENTITIES.register("abandoned_dusterbike_spawner", () -> BlockEntityType.Builder.of(AbandonedDusterbikeSpawnerBlockEntity::new, BlockRegistry.ABANDONED_DUSTERBIKE_SPAWNER.get()).build(null));
}