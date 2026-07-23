package destiny.null_ouroboros.server.registry;

import destiny.null_ouroboros.NullOuroboros;
import destiny.null_ouroboros.server.item.JerrycanItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class CreativeTabRegistry {
    public static final DeferredRegister<CreativeModeTab> DEF_REG  = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, NullOuroboros.MODID);

    public static final RegistryObject<CreativeModeTab> MAIN = DEF_REG.register("main", () -> CreativeModeTab.builder()
            .icon(() -> BlockRegistry.ASH_BLOCK.get().asItem().getDefaultInstance())
            .title(Component.translatable("itemGroup.null_ouroboros.main"))
            .displayItems((parameters, output) -> {
                output.accept(BlockRegistry.ASH_PILE.get());
                output.accept(BlockRegistry.ASH_BLOCK.get());
                output.accept(BlockRegistry.TRAMPLED_ASH.get());

                output.accept(BlockRegistry.VEINED_ASH_BLOCK.get());
                output.accept(BlockRegistry.VEINED_TRAMPLED_ASH.get());

                output.accept(BlockRegistry.SCORCHED_SAPLING.get());

                output.accept(BlockRegistry.SCORCHED_LOG.get());
                output.accept(BlockRegistry.SCORCHED_PLANKS.get());
                output.accept(BlockRegistry.SCORCHED_STAIRS.get());
                output.accept(BlockRegistry.SCORCHED_SLAB.get());
                output.accept(BlockRegistry.SCORCHED_FENCE.get());
                output.accept(BlockRegistry.SCORCHED_FENCE_GATE.get());
                output.accept(BlockRegistry.SCORCHED_DOOR.get());
                output.accept(BlockRegistry.SCORCHED_TRAPDOOR.get());
                output.accept(BlockRegistry.SCORCHED_BUTTON.get());
                output.accept(BlockRegistry.SCORCHED_PRESSURE_PLATE.get());

                output.accept(BlockRegistry.SANGUINE_LOG.get());
                output.accept(BlockRegistry.SANGUINE_PLANKS.get());
                output.accept(BlockRegistry.SANGUINE_STAIRS.get());
                output.accept(BlockRegistry.SANGUINE_SLAB.get());
                output.accept(BlockRegistry.SANGUINE_FENCE.get());
                output.accept(BlockRegistry.SANGUINE_FENCE_GATE.get());
                output.accept(BlockRegistry.SANGUINE_DOOR.get());
                output.accept(BlockRegistry.SANGUINE_TRAPDOOR.get());
                output.accept(BlockRegistry.SANGUINE_BUTTON.get());
                output.accept(BlockRegistry.SANGUINE_PRESSURE_PLATE.get());

                output.accept(BlockRegistry.BLACKMETAL_PLATE.get());
                output.accept(BlockRegistry.BLACKMETAL_PLATE_STAIRS.get());
                output.accept(BlockRegistry.BLACKMETAL_PLATE_SLAB.get());
                output.accept(BlockRegistry.BLACKMETAL_PLATE_WALL.get());
                output.accept(BlockRegistry.BLACKMETAL_PLATE_BUTTON.get());
                output.accept(BlockRegistry.BLACKMETAL_PLATE_PRESSURE_PLATE.get());
                output.accept(BlockRegistry.BLACKMETAL_TILES.get());
                output.accept(BlockRegistry.BLACKMETAL_TILE_STAIRS.get());
                output.accept(BlockRegistry.BLACKMETAL_TILE_SLAB.get());
                output.accept(BlockRegistry.BLACKMETAL_TILE_WALL.get());
                output.accept(BlockRegistry.BLACKMETAL_TILE_BUTTON.get());
                output.accept(BlockRegistry.BLACKMETAL_TILE_PRESSURE_PLATE.get());
                output.accept(BlockRegistry.BLACKMETAL_SUPPORT.get());
                output.accept(BlockRegistry.BLACKMETAL_SUPPORT_STAIRS.get());
                output.accept(BlockRegistry.BLACKMETAL_SUPPORT_SLAB.get());
                output.accept(BlockRegistry.BLACKMETAL_SUPPORT_WALL.get());
                output.accept(BlockRegistry.BLACKMETAL_SUPPORT_BUTTON.get());
                output.accept(BlockRegistry.BLACKMETAL_SUPPORT_PRESSURE_PLATE.get());

                output.accept(BlockRegistry.BLACKMETAL_TRUSS.get());

                output.accept(BlockRegistry.DROPLIGHT.get());
                output.accept(BlockRegistry.BROKEN_DROPLIGHT.get());
                output.accept(BlockRegistry.STROBELIGHT.get());
                output.accept(BlockRegistry.MECHANICAL_SIREN.get());
                output.accept(BlockRegistry.TEMPORAL_SURGE_DETECTOR.get());

                output.accept(BlockRegistry.DUSTY_COMPUTER.get());
                output.accept(BlockRegistry.ELECTROMAGNETIC_ASSEMBLY.get());

                output.accept(BlockRegistry.MANIFOLDING_LABEL.get());
                output.accept(ItemRegistry.STOP_SIGN.get());

                output.accept(ItemRegistry.BURROW_BEACON.get());
                output.accept(ItemRegistry.REDSTICK.get());

                output.accept(ItemRegistry.BLACKMETAL_PANEL.get());
                output.accept(ItemRegistry.DATA_VEINS.get());
                output.accept(ItemRegistry.DRILL_BIT.get());
                output.accept(ItemRegistry.DRILL_SHARD.get());
                output.accept(ItemRegistry.THRUSTER_NOZZLE.get());
                output.accept(ItemRegistry.THRUSTER_SHARD.get());

                output.accept(ItemRegistry.DISKETTE.get());

                output.accept(ItemRegistry.WRENCH.get());
                output.accept(JerrycanItem.createFilled());
                output.accept(ItemRegistry.SPRAY_CAN.get());

                output.accept(ItemRegistry.ENGINE_HOIST.get());
                output.accept(ItemRegistry.DUSTERBIKE_FRAME_BUILT.get());
                output.accept(ItemRegistry.DUSTERBIKE_FRAME.get());

                output.accept(ItemRegistry.BIKE_KEY.get());
                output.accept(ItemRegistry.ENGINE_BASE.get());
                output.accept(ItemRegistry.PISTON.get());
                output.accept(ItemRegistry.SPARK_PLUG.get());
                output.accept(ItemRegistry.BIKE_BATTERY.get());
                output.accept(ItemRegistry.BIKE_WHEEL.get());
                output.accept(ItemRegistry.HEADLIGHT.get());

                output.accept(ItemRegistry.HEAVY_REVOLVER.get());
                output.accept(ItemRegistry.REVOLVER_CARTRIDGE_HP.get());
                output.accept(ItemRegistry.REVOLVER_CARTRIDGE_AP.get());
                output.accept(ItemRegistry.REVOLVER_CARTRIDGE_IC.get());
                output.accept(ItemRegistry.REVOLVER_CARTRIDGE_OP.get());
                output.accept(ItemRegistry.REVOLVER_EMPTY_CASING.get());

                output.accept(ItemRegistry.BINARY_SWORD.get());
                output.accept(ItemRegistry.BINARY_SHARD.get());

                output.accept(ItemRegistry.RAKE.get());
                output.accept(ItemRegistry.LIQUIDATOR_GAS_MASK.get());
                output.accept(ItemRegistry.LIQUIDATOR_CHESTPLATE.get());
                output.accept(ItemRegistry.LIQUIDATOR_LEGGINGS.get());
                output.accept(ItemRegistry.LIQUIDATOR_BOOTS.get());
            })
            .build()
    );
}