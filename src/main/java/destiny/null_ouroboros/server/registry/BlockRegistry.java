package destiny.null_ouroboros.server.registry;

import destiny.null_ouroboros.NullOuroboros;
import destiny.null_ouroboros.server.block.AshPileBlock;
import destiny.null_ouroboros.server.block.ScorchedSaplingBlock;
import destiny.null_ouroboros.server.worldgen.grower.ScorchedTreeGrower;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

public class BlockRegistry {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, NullOuroboros.MODID);

    public static final RegistryObject<Block> ASH_PILE = registerBlock("ash_pile",
            () -> new AshPileBlock(BlockBehaviour.Properties.copy(Blocks.SAND)
                    .mapColor(MapColor.COLOR_LIGHT_GRAY).noOcclusion()));
    public static final RegistryObject<Block> ASH_BLOCK = registerBlock("ash_block",
            () -> new FallingBlock(BlockBehaviour.Properties.copy(Blocks.SAND)
                    .mapColor(MapColor.COLOR_LIGHT_GRAY)));
    public static final RegistryObject<Block> TRAMPLED_ASH = registerBlock("trampled_ash",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE)
                    .mapColor(MapColor.COLOR_GRAY)));

    public static final RegistryObject<Block> SCORCHED_LOG = registerBlock("scorched_log",
            () -> new RotatedPillarBlock(BlockBehaviour.Properties.copy(Blocks.OAK_LOG)
                    .mapColor(MapColor.COLOR_GRAY)));
    public static final RegistryObject<Block> SCORCHED_SAPLING = registerBlock("scorched_sapling",
            () -> new ScorchedSaplingBlock(new ScorchedTreeGrower(), BlockBehaviour.Properties.copy(Blocks.OAK_SAPLING)
                    .mapColor(MapColor.COLOR_GRAY)));

    private static <T extends Block> RegistryObject<T> registerBlock(String name, Supplier<T> block) {
        RegistryObject<T> toReturn = BLOCKS.register(name, block);
        registerBlockItem(name, toReturn);
        return toReturn;
    }

    private static <T extends Block> RegistryObject<Item> registerBlockItem(String name, Supplier<T> block) {
        return ItemRegistry.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }
}
