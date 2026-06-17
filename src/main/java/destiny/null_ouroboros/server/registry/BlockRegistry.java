package destiny.null_ouroboros.server.registry;

import destiny.null_ouroboros.NullOuroboros;
import destiny.null_ouroboros.server.block.*;
import destiny.null_ouroboros.server.worldgen.grower.ScorchedTreeGrower;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

import static destiny.null_ouroboros.server.block.DroplightBlock.LIT;
import static destiny.null_ouroboros.server.block.DroplightBlock.POWERED;

public class BlockRegistry {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, NullOuroboros.MODID);

    public static final RegistryObject<Block> ASH_PILE = registerBlock("ash_pile",
            () -> new AshPileBlock(BlockBehaviour.Properties.copy(Blocks.SAND)
                    .mapColor(MapColor.COLOR_LIGHT_GRAY).noOcclusion().replaceable()));
    public static final RegistryObject<Block> ASH_BLOCK = registerBlock("ash_block",
            () -> new AshBlock(BlockBehaviour.Properties.copy(Blocks.SAND)
                    .mapColor(MapColor.COLOR_LIGHT_GRAY)));
    public static final RegistryObject<Block> TRAMPLED_ASH = registerBlock("trampled_ash",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE)
                    .mapColor(MapColor.COLOR_GRAY)));

    public static final RegistryObject<Block> SCORCHED_LOG = registerBlock("scorched_log",
            () -> new ScorchedLogBlock(BlockBehaviour.Properties.copy(Blocks.OAK_LOG)
                    .mapColor(MapColor.COLOR_GRAY)));
    public static final RegistryObject<Block> SANGUINE_LOG = registerBlock("sanguine_log",
            () -> new RotatedPillarBlock(BlockBehaviour.Properties.copy(Blocks.OAK_LOG)
                    .mapColor(MapColor.COLOR_RED)));
    public static final RegistryObject<Block> SCORCHED_SAPLING = registerBlock("scorched_sapling",
            () -> new ScorchedSaplingBlock(new ScorchedTreeGrower(), BlockBehaviour.Properties.copy(Blocks.OAK_SAPLING)
                    .mapColor(MapColor.COLOR_GRAY)));

    public static final RegistryObject<Block> BLACKMETAL = registerBlock("blackmetal",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)
                    .mapColor(MapColor.COLOR_GRAY).sound(SoundType.LANTERN)));
    public static final RegistryObject<Block> BLACKMETAL_PLATE = registerBlock("blackmetal_plate",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)
                    .mapColor(MapColor.COLOR_GRAY).sound(SoundType.LANTERN)));
    public static final RegistryObject<Block> BLACKMETAL_TILES = registerBlock("blackmetal_tiles",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)
                    .mapColor(MapColor.COLOR_GRAY).sound(SoundType.LANTERN)));
    public static final RegistryObject<Block> BLACKMETAL_SUPPORT = registerBlock("blackmetal_support",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)
                    .mapColor(MapColor.COLOR_GRAY).sound(SoundType.LANTERN).noOcclusion()));
    public static final RegistryObject<Block> BLACKMETAL_TRUSS = registerBlock("blackmetal_truss",
            () -> new BlackmetalTrussBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)
                    .mapColor(MapColor.COLOR_GRAY).sound(SoundType.LANTERN).noOcclusion()));

    public static final RegistryObject<Block> DROPLIGHT = registerBlock("droplight",
            () -> new DroplightBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)
                    .mapColor(MapColor.COLOR_GRAY).sound(SoundType.LANTERN).noOcclusion().lightLevel(state -> state.getValue(LIT) ? 12 : 0)));
    public static final RegistryObject<Block> BROKEN_DROPLIGHT = registerBlock("broken_droplight",
            () -> new BrokenDroplightBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)
                    .mapColor(MapColor.COLOR_GRAY).sound(SoundType.LANTERN).noOcclusion().lightLevel(state -> state.getValue(LIT) ? 10 : 0).randomTicks()));
    public static final RegistryObject<Block> STROBELIGHT = registerBlock("strobelight",
            () -> new StrobelightBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)
                    .mapColor(MapColor.COLOR_GRAY).sound(SoundType.LANTERN).noOcclusion().lightLevel(state -> state.getValue(LIT) ? 8 : 0)));

    public static final RegistryObject<Block> MANIFOLDING_TEXT = registerBlock("manifolding_text",
            () -> new ManifoldingTextBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)
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
