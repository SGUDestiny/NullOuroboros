package destiny.null_ouroboros.server.ash;

import destiny.null_ouroboros.NullOuroboros;
import destiny.null_ouroboros.server.block.BulkheadBlock;
import destiny.null_ouroboros.server.block.IntakeFanBlock;
import destiny.null_ouroboros.server.block.OutputVentBlock;
import destiny.null_ouroboros.server.block.VentilationRouterBlock;
import destiny.null_ouroboros.server.block.VentilationShaftBlock;
import destiny.null_ouroboros.server.camouflage.Camouflage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FluidState;

public final class AshAirtight {
    public static final TagKey<Block> VERGE_UNSEALABLE = BlockTags.create(
            ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, "verge_unsealable"));

    public static final int ENCLOSURE_CELL_LIMIT = 8192;
    public static final int EXTERIOR_SCAN_LIMIT = 8192;
    public static final int SPREAD_SCAN_LIMIT = 64;

    private AshAirtight() {
    }

    public static boolean isDuctBlock(BlockState state) {
        Block block = state.getBlock();
        return block instanceof VentilationShaftBlock
                || block instanceof VentilationRouterBlock
                || block instanceof IntakeFanBlock
                || block instanceof OutputVentBlock;
    }

    public static boolean isAirCell(BlockGetter level, BlockPos pos) {
        if (level instanceof Level realLevel && !realLevel.isLoaded(pos)) {
            return false;
        }
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) {
            return true;
        }
        if (state.is(VERGE_UNSEALABLE)) {
            return true;
        }
        Block block = state.getBlock();
        if (block instanceof IntakeFanBlock || block instanceof OutputVentBlock) {
            return false;
        }
        if (block instanceof VentilationShaftBlock || block instanceof VentilationRouterBlock) {
            return !Camouflage.isSealingCamouflage(level, pos);
        }
        if (block instanceof BulkheadBlock) {
            return BulkheadBlock.isPassable(state);
        }
        if (block instanceof DoorBlock) {
            return state.hasProperty(DoorBlock.OPEN) && state.getValue(DoorBlock.OPEN);
        }
        if (block instanceof TrapDoorBlock) {
            return state.hasProperty(BlockStateProperties.OPEN) && state.getValue(BlockStateProperties.OPEN);
        }
        FluidState fluid = state.getFluidState();
        if (!fluid.isEmpty() && !state.blocksMotion()) {
            return true;
        }
        return !state.blocksMotion() && state.getCollisionShape(level, pos).isEmpty();
    }

    public static boolean isOpenAirCell(BlockGetter level, BlockPos pos) {
        return isAirCell(level, pos) && !isDuctBlock(level.getBlockState(pos));
    }

    public static boolean canFlow(BlockGetter level, BlockPos from, Direction direction) {
        BlockPos to = from.relative(direction);
        if (!isAirCell(level, from) || !isAirCell(level, to)) {
            return false;
        }
        return true;
    }

    public static boolean isSkyExposed(Level level, BlockPos pos) {
        if (!level.isLoaded(pos)) {
            return true;
        }
        return pos.getY() >= level.getHeight(Heightmap.Types.MOTION_BLOCKING, pos.getX(), pos.getZ());
    }
}
