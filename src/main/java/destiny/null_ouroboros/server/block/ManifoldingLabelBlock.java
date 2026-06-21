package destiny.null_ouroboros.server.block;

import destiny.null_ouroboros.server.util.ModUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class ManifoldingLabelBlock extends HorizontalDirectionalBlock {
    public static final DirectionProperty HORIZONTAL_FACING = BlockStateProperties.HORIZONTAL_FACING;

    public static final VoxelShape SHAPE_NORTH = ModUtil.buildShape(
            Block.box(0, 0, 15, 16, 16, 16)
    );
    public static final VoxelShape SHAPE_SOUTH = ModUtil.buildShape(
            Block.box(0, 0, 0, 16, 16, 1)
    );
    public static final VoxelShape SHAPE_WEST = ModUtil.buildShape(
            Block.box(15, 0, 0, 16, 16, 16)
    );
    public static final VoxelShape SHAPE_EAST = ModUtil.buildShape(
            Block.box(0, 0, 0, 1, 16, 16)
    );

    public ManifoldingLabelBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.defaultBlockState().setValue(HORIZONTAL_FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HORIZONTAL_FACING);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection();

        return this.defaultBlockState().setValue(HORIZONTAL_FACING, facing.getOpposite());
    }

    @Override
    public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        switch (pState.getValue(FACING)) {
            case NORTH:
                return SHAPE_NORTH;
            case SOUTH:
                return SHAPE_SOUTH;
            case EAST:
                return SHAPE_EAST;
            case WEST:
                return SHAPE_WEST;
            default:
                return SHAPE_NORTH;
        }
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter getter, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    public BlockState updateShape(BlockState pState, Direction pDirection, BlockState pNeighborState, LevelAccessor pLevel, BlockPos pPos, BlockPos pNeighborPos) {
        if (!isOnBlock(pLevel, pPos, pState) && pDirection == pState.getValue(FACING).getOpposite()) {
            pLevel.destroyBlock(pPos, true);
        }

        return super.updateShape(pState, pDirection, pNeighborState, pLevel, pPos, pNeighborPos);
    }

    public boolean isOnBlock(LevelAccessor pLevel, BlockPos pPos, BlockState pState) {
        Direction blockDirection = pState.getValue(FACING);
        Block parentBlock = pLevel.getBlockState(pPos.relative(blockDirection.getOpposite())).getBlock();
        return !parentBlock.equals(Blocks.AIR);
    }
}
