package destiny.null_ouroboros.server.registry;

import destiny.null_ouroboros.NullOuroboros;
import destiny.null_ouroboros.server.block.*;
import destiny.null_ouroboros.server.worldgen.grower.ScorchedTreeGrower;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

import static destiny.null_ouroboros.server.block.DroplightBlock.LIT;
import static destiny.null_ouroboros.server.block.DroplightBlock.POWERED;

public class BlockRegistry {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, NullOuroboros.MODID);

    public static final BlockBehaviour.Properties SCORCHED_PROPERTIES = BlockBehaviour.Properties.copy(Blocks.OAK_PLANKS).mapColor(MapColor.COLOR_GRAY).sound(SoundType.WOOD);
    public static final BlockBehaviour.Properties SANGUINE_PROPERTIES = BlockBehaviour.Properties.copy(Blocks.OAK_PLANKS).mapColor(MapColor.COLOR_RED).sound(SoundType.WOOD);
    public static final BlockBehaviour.Properties BLACKMETAL_PROPERTIES = BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).mapColor(MapColor.COLOR_GRAY).sound(SoundType.LANTERN).requiresCorrectToolForDrops();
    public static final BlockBehaviour.Properties BLACKMETAL_SUPPORT_PROPERTIES = BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).mapColor(MapColor.COLOR_GRAY).sound(SoundType.LANTERN).requiresCorrectToolForDrops().noOcclusion();

    public static final BlockSetType SCORCHED_BLOCKSET = new BlockSetType("scorched", true, SoundType.WOOD, SoundEvents.WOODEN_DOOR_CLOSE, SoundEvents.WOODEN_DOOR_OPEN, SoundEvents.WOODEN_TRAPDOOR_CLOSE, SoundEvents.WOODEN_TRAPDOOR_OPEN, SoundEvents.WOODEN_PRESSURE_PLATE_CLICK_OFF, SoundEvents.WOODEN_PRESSURE_PLATE_CLICK_ON, SoundEvents.WOODEN_BUTTON_CLICK_OFF, SoundEvents.WOODEN_BUTTON_CLICK_ON);
    public static final BlockSetType SANGUINE_BLOCKSET = new BlockSetType("sanguine", true, SoundType.WOOD, SoundEvents.WOODEN_DOOR_CLOSE, SoundEvents.WOODEN_DOOR_OPEN, SoundEvents.WOODEN_TRAPDOOR_CLOSE, SoundEvents.WOODEN_TRAPDOOR_OPEN, SoundEvents.WOODEN_PRESSURE_PLATE_CLICK_OFF, SoundEvents.WOODEN_PRESSURE_PLATE_CLICK_ON, SoundEvents.WOODEN_BUTTON_CLICK_OFF, SoundEvents.WOODEN_BUTTON_CLICK_ON);
    public static final BlockSetType BLACKMETAL_BLOCKSET = new BlockSetType("blackmetal", false, SoundType.LANTERN, SoundEvents.IRON_DOOR_CLOSE, SoundEvents.IRON_DOOR_OPEN, SoundEvents.IRON_TRAPDOOR_CLOSE, SoundEvents.IRON_TRAPDOOR_OPEN, SoundEvents.STONE_PRESSURE_PLATE_CLICK_OFF, SoundEvents.STONE_PRESSURE_PLATE_CLICK_ON, SoundEvents.STONE_BUTTON_CLICK_OFF, SoundEvents.STONE_BUTTON_CLICK_ON);

    public static final RegistryObject<Block> ASH_PILE = registerBlock("ash_pile",
            () -> new AshPileBlock(BlockBehaviour.Properties.copy(Blocks.SAND)
                    .mapColor(MapColor.COLOR_LIGHT_GRAY).noOcclusion().replaceable()));
    public static final RegistryObject<Block> ASH_BLOCK = registerBlock("ash_block",
            () -> new AshBlock(BlockBehaviour.Properties.copy(Blocks.SAND)
                    .mapColor(MapColor.COLOR_LIGHT_GRAY)));
    public static final RegistryObject<Block> TRAMPLED_ASH = registerBlock("trampled_ash",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE)
                    .mapColor(MapColor.COLOR_GRAY)));

    public static final RegistryObject<Block> VEINED_ASH_BLOCK = registerBlock("veined_ash_block",
            () -> new AshBlock(BlockBehaviour.Properties.copy(Blocks.SAND)
                    .mapColor(MapColor.COLOR_RED).sound(SoundType.STEM)));
    public static final RegistryObject<Block> VEINED_TRAMPLED_ASH = registerBlock("veined_trampled_ash",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE)
                    .mapColor(MapColor.COLOR_RED).sound(SoundType.STEM)));

    //Scorched misc
    public static final RegistryObject<Block> SCORCHED_SAPLING = registerBlock("scorched_sapling",
            () -> new ScorchedSaplingBlock(new ScorchedTreeGrower(), BlockBehaviour.Properties.copy(Blocks.OAK_SAPLING)
                    .mapColor(MapColor.COLOR_GRAY)));

    //Scorched wood
    public static final RegistryObject<Block> SCORCHED_LOG = registerBlock("scorched_log", () -> new ScorchedLogBlock(SCORCHED_PROPERTIES));
    public static final RegistryObject<Block> SCORCHED_PLANKS = registerBlock("scorched_planks",
            () -> new Block(SCORCHED_PROPERTIES));
    public static final RegistryObject<Block> SCORCHED_STAIRS = registerBlock("scorched_stairs",
            () -> new StairBlock(BlockRegistry.SCORCHED_PLANKS.get().defaultBlockState(), SCORCHED_PROPERTIES));
    public static final RegistryObject<Block> SCORCHED_SLAB = registerBlock("scorched_slab",
            () -> new SlabBlock(SCORCHED_PROPERTIES));
    public static final RegistryObject<Block> SCORCHED_DOOR = registerBlock("scorched_door",
            () -> new DoorBlock(BlockBehaviour.Properties.copy(Blocks.OAK_PLANKS).mapColor(MapColor.COLOR_RED).noOcclusion(), BlockSetType.CHERRY));
    public static final RegistryObject<Block> SCORCHED_TRAPDOOR = registerBlock("scorched_trapdoor",
            () -> new TrapDoorBlock(BlockBehaviour.Properties.copy(Blocks.OAK_PLANKS).mapColor(MapColor.COLOR_RED)
                    .noOcclusion(), BlockSetType.CHERRY));
    public static final RegistryObject<Block> SCORCHED_FENCE = registerBlock("scorched_fence", () -> new FenceBlock(SCORCHED_PROPERTIES));
    public static final RegistryObject<Block> SCORCHED_FENCE_GATE = registerBlock("scorched_fence_gate",
            () -> new FenceGateBlock(SCORCHED_PROPERTIES, WoodType.CHERRY));
    public static final RegistryObject<Block> SCORCHED_BUTTON = registerBlock("scorched_button",
            () -> new ButtonBlock(BlockBehaviour.Properties.copy(Blocks.OAK_PLANKS).mapColor(MapColor.COLOR_RED)
                    .noCollission(), SCORCHED_BLOCKSET, 30, true));
    public static final RegistryObject<Block> SCORCHED_PRESSURE_PLATE = registerBlock("scorched_pressure_plate",
            () -> new PressurePlateBlock(PressurePlateBlock.Sensitivity.EVERYTHING, BlockBehaviour.Properties.copy(Blocks.OAK_PLANKS)
                    .mapColor(MapColor.COLOR_RED).noCollission(), SCORCHED_BLOCKSET));

    //Sanguine wood
    public static final RegistryObject<Block> SANGUINE_LOG = registerBlock("sanguine_log", () -> new RotatedPillarBlock(SANGUINE_PROPERTIES));
    public static final RegistryObject<Block> SANGUINE_PLANKS = registerBlock("sanguine_planks",
            () -> new Block(SANGUINE_PROPERTIES));
    public static final RegistryObject<Block> SANGUINE_STAIRS = registerBlock("sanguine_stairs",
            () -> new StairBlock(BlockRegistry.SANGUINE_PLANKS.get().defaultBlockState(), SANGUINE_PROPERTIES));
    public static final RegistryObject<Block> SANGUINE_SLAB = registerBlock("sanguine_slab",
            () -> new SlabBlock(SANGUINE_PROPERTIES));
    public static final RegistryObject<Block> SANGUINE_DOOR = registerBlock("sanguine_door",
            () -> new DoorBlock(BlockBehaviour.Properties.copy(Blocks.OAK_PLANKS).mapColor(MapColor.COLOR_RED).noOcclusion(), BlockSetType.CHERRY));
    public static final RegistryObject<Block> SANGUINE_TRAPDOOR = registerBlock("sanguine_trapdoor",
            () -> new TrapDoorBlock(BlockBehaviour.Properties.copy(Blocks.OAK_PLANKS).mapColor(MapColor.COLOR_RED)
                    .noOcclusion(), BlockSetType.CHERRY));
    public static final RegistryObject<Block> SANGUINE_FENCE = registerBlock("sanguine_fence", () -> new FenceBlock(SANGUINE_PROPERTIES));
    public static final RegistryObject<Block> SANGUINE_FENCE_GATE = registerBlock("sanguine_fence_gate",
            () -> new FenceGateBlock(SANGUINE_PROPERTIES, WoodType.CHERRY));
    public static final RegistryObject<Block> SANGUINE_BUTTON = registerBlock("sanguine_button",
            () -> new ButtonBlock(BlockBehaviour.Properties.copy(Blocks.OAK_PLANKS).mapColor(MapColor.COLOR_RED)
                    .noCollission(), SANGUINE_BLOCKSET, 30, true));
    public static final RegistryObject<Block> SANGUINE_PRESSURE_PLATE = registerBlock("sanguine_pressure_plate",
            () -> new PressurePlateBlock(PressurePlateBlock.Sensitivity.EVERYTHING, BlockBehaviour.Properties.copy(Blocks.OAK_PLANKS)
                    .mapColor(MapColor.COLOR_RED).noCollission(), SANGUINE_BLOCKSET));

    //Blackmetal plate
    public static final RegistryObject<Block> BLACKMETAL_PLATE = registerBlock("blackmetal_plate", () -> new Block(BLACKMETAL_PROPERTIES));
    public static final RegistryObject<Block> BLACKMETAL_PLATE_STAIRS = registerBlock("blackmetal_plate_stairs",
            () -> new StairBlock(BlockRegistry.BLACKMETAL_PLATE.get().defaultBlockState(), BLACKMETAL_PROPERTIES));
    public static final RegistryObject<Block> BLACKMETAL_PLATE_SLAB = registerBlock("blackmetal_plate_slab",
            () -> new SlabBlock(BLACKMETAL_PROPERTIES));
    public static final RegistryObject<Block> BLACKMETAL_PLATE_WALL = registerBlock("blackmetal_plate_wall",
            () -> new WallBlock(BLACKMETAL_PROPERTIES));
    public static final RegistryObject<Block> BLACKMETAL_PLATE_BUTTON = registerBlock("blackmetal_plate_button",
            () -> new ButtonBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).mapColor(MapColor.COLOR_GRAY).requiresCorrectToolForDrops().noCollission(), BLACKMETAL_BLOCKSET, 30, false));
    public static final RegistryObject<Block> BLACKMETAL_PLATE_PRESSURE_PLATE = registerBlock("blackmetal_plate_pressure_plate",
            () -> new PressurePlateBlock(PressurePlateBlock.Sensitivity.EVERYTHING, BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)
                    .mapColor(MapColor.COLOR_GRAY).requiresCorrectToolForDrops().noCollission(), BLACKMETAL_BLOCKSET));

    //Blackmetal tiles
    public static final RegistryObject<Block> BLACKMETAL_TILES = registerBlock("blackmetal_tiles", () -> new Block(BLACKMETAL_PROPERTIES));
    public static final RegistryObject<Block> BLACKMETAL_TILE_STAIRS = registerBlock("blackmetal_tile_stairs",
            () -> new StairBlock(BlockRegistry.BLACKMETAL_TILES.get().defaultBlockState(), BLACKMETAL_PROPERTIES));
    public static final RegistryObject<Block> BLACKMETAL_TILE_SLAB = registerBlock("blackmetal_tile_slab",
            () -> new SlabBlock(BLACKMETAL_PROPERTIES));
    public static final RegistryObject<Block> BLACKMETAL_TILE_WALL = registerBlock("blackmetal_tile_wall",
            () -> new WallBlock(BLACKMETAL_PROPERTIES));
    public static final RegistryObject<Block> BLACKMETAL_TILE_BUTTON = registerBlock("blackmetal_tile_button",
            () -> new ButtonBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).mapColor(MapColor.COLOR_GRAY).requiresCorrectToolForDrops()
                    .noCollission(), BLACKMETAL_BLOCKSET, 30, false));
    public static final RegistryObject<Block> BLACKMETAL_TILE_PRESSURE_PLATE = registerBlock("blackmetal_tile_pressure_plate",
            () -> new PressurePlateBlock(PressurePlateBlock.Sensitivity.EVERYTHING, BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)
                    .mapColor(MapColor.COLOR_GRAY).requiresCorrectToolForDrops().noCollission(), BLACKMETAL_BLOCKSET));

    //Blackmetal support
    public static final RegistryObject<Block> BLACKMETAL_SUPPORT = registerBlock("blackmetal_support",
            () -> new Block(BLACKMETAL_SUPPORT_PROPERTIES));
    public static final RegistryObject<Block> BLACKMETAL_SUPPORT_STAIRS = registerBlock("blackmetal_support_stairs",
            () -> new StairBlock(BlockRegistry.BLACKMETAL_TILES.get().defaultBlockState(), BLACKMETAL_SUPPORT_PROPERTIES));
    public static final RegistryObject<Block> BLACKMETAL_SUPPORT_SLAB = registerBlock("blackmetal_support_slab",
            () -> new SlabBlock(BLACKMETAL_SUPPORT_PROPERTIES));
    public static final RegistryObject<Block> BLACKMETAL_SUPPORT_WALL = registerBlock("blackmetal_support_wall",
            () -> new WallBlock(BLACKMETAL_SUPPORT_PROPERTIES));
    public static final RegistryObject<Block> BLACKMETAL_SUPPORT_BUTTON = registerBlock("blackmetal_support_button",
            () -> new ButtonBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).mapColor(MapColor.COLOR_GRAY).requiresCorrectToolForDrops()
                    .noCollission(), BLACKMETAL_BLOCKSET, 30, false));
    public static final RegistryObject<Block> BLACKMETAL_SUPPORT_PRESSURE_PLATE = registerBlock("blackmetal_support_pressure_plate",
            () -> new PressurePlateBlock(PressurePlateBlock.Sensitivity.EVERYTHING, BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)
                    .mapColor(MapColor.COLOR_GRAY).requiresCorrectToolForDrops().noCollission(), BLACKMETAL_BLOCKSET));

    public static final RegistryObject<Block> BLACKMETAL_TRUSS = registerBlock("blackmetal_truss",
            () -> new BlackmetalTrussBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)
                    .mapColor(MapColor.COLOR_GRAY).sound(SoundType.LANTERN).noOcclusion()));

    public static final RegistryObject<Block> DROPLIGHT = registerBlock("droplight",
            () -> new DroplightBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)
                    .mapColor(MapColor.COLOR_GRAY).sound(SoundType.LANTERN).noOcclusion().lightLevel(state -> state.getValue(LIT) ? 15 : 0)));
    public static final RegistryObject<Block> BROKEN_DROPLIGHT = registerBlock("broken_droplight",
            () -> new BrokenDroplightBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)
                    .mapColor(MapColor.COLOR_GRAY).sound(SoundType.LANTERN).noOcclusion().lightLevel(state -> state.getValue(LIT) ? 10 : 0)
                    .randomTicks()));
    public static final RegistryObject<Block> STROBELIGHT = registerBlock("strobelight",
            () -> new StrobelightBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)
                    .mapColor(MapColor.COLOR_GRAY).sound(SoundType.LANTERN).noOcclusion().lightLevel(state -> state.getValue(LIT) ? 8 : 0)));

    public static final RegistryObject<Block> MANIFOLDING_LABEL = registerBlock("manifolding_label",
            () -> new ManifoldingLabelBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).sound(SoundType.LANTERN)
                    .strength(0.0F, 6.0F).instabreak().noParticlesOnBreak().noOcclusion()));

    public static final RegistryObject<Block> MECHANICAL_SIREN = registerBlock("mechanical_siren",
            () -> new MechanicalSirenBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)
                    .mapColor(MapColor.COLOR_GRAY).sound(SoundType.LANTERN).noOcclusion()));
    public static final RegistryObject<Block> TEMPORAL_SURGE_DETECTOR = registerBlock("temporal_surge_detector",
            () -> new TemporalSurgeDetectorBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)
                    .mapColor(MapColor.COLOR_GRAY).sound(SoundType.LANTERN).noOcclusion()));

    public static final RegistryObject<Block> DUSTY_COMPUTER = registerBlock("dusty_computer",
            () -> new DustyComputerBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)
                    .mapColor(MapColor.COLOR_GRAY).sound(SoundType.LANTERN).noOcclusion().lightLevel(state -> state.getValue(POWERED) ? 6 : 0)));
    public static final RegistryObject<Block> ELECTROMAGNETIC_ASSEMBLY = registerBlock("electromagnetic_assembly",
            () -> new ElectromagneticAssemblyBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)
                    .mapColor(MapColor.COLOR_RED).sound(SoundType.LANTERN).noOcclusion()));

    public static final RegistryObject<Block> STOP_SIGN = BLOCKS.register("stop_sign",
            () -> new StopSignBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)
                    .mapColor(MapColor.COLOR_RED).sound(SoundType.LANTERN).noOcclusion()));

    private static <T extends Block> RegistryObject<T> registerBlock(String name, Supplier<T> block) {
        RegistryObject<T> toReturn = BLOCKS.register(name, block);
        registerBlockItem(name, toReturn);
        return toReturn;
    }

    private static <T extends Block> RegistryObject<Item> registerBlockItem(String name, Supplier<T> block) {
        return ItemRegistry.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }
}
