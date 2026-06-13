package destiny.null_ouroboros.server.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

public class AshPileBlock extends FallingBlock {
    public static final int MAX_HEIGHT = 8;
    public static final IntegerProperty LAYERS = BlockStateProperties.LAYERS;
    protected static final VoxelShape[] SHAPE_BY_LAYER = new VoxelShape[]{
            Shapes.empty(),
            Block.box(0.0F, 0.0F, 0.0F,
            16.0F, 2.0F, 16.0F),
            Block.box(0.0F, 0.0F, 0.0F,
                    16.0F, 4.0F, 16.0F),
            Block.box(0.0F, 0.0F, 0.0F,
                    16.0F, 6.0F, 16.0F),
            Block.box(0.0F, 0.0F, 0.0F,
                    16.0F, 8.0F, 16.0F),
            Block.box(0.0F, 0.0F, 0.0F,
                    16.0F, 10.0F, 16.0F),
            Block.box(0.0F, 0.0F, 0.0F,
                    16.0F, 12.0F, 16.0F),
            Block.box(0.0F, 0.0F, 0.0F,
                    16.0F, 14.0F, 16.0F),
            Block.box(0.0F, 0.0F, 0.0F,
                    16.0F, 16.0F, 16.0F)};
    public static final int HEIGHT_IMPASSABLE = 5;

    public AshPileBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.defaultBlockState().setValue(LAYERS, 1));
    }

    @Override
    public boolean isPathfindable(BlockState state, BlockGetter getter, BlockPos pos, PathComputationType pathType) {
        switch (pathType) {
            case LAND -> {
                return state.getValue(LAYERS) < HEIGHT_IMPASSABLE;
            }
            case WATER -> {
                return false;
            }
            case AIR -> {
                return false;
            }
            default -> {
                return false;
            }
        }
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter getter, BlockPos pos, CollisionContext context) {
        return SHAPE_BY_LAYER[state.getValue(LAYERS)];
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter getter, BlockPos pos, CollisionContext context) {
        return SHAPE_BY_LAYER[state.getValue(LAYERS) - 1];
    }

    @Override
    public VoxelShape getBlockSupportShape(BlockState state, BlockGetter getter, BlockPos pos) {
        return SHAPE_BY_LAYER[state.getValue(LAYERS)];
    }

    @Override
    public VoxelShape getVisualShape(BlockState state, BlockGetter getter, BlockPos pos, CollisionContext context) {
        return SHAPE_BY_LAYER[state.getValue(LAYERS)];
    }

    @Override
    public boolean useShapeForLightOcclusion(BlockState p_56630_) {
        return false;
    }

    @Override
    public VoxelShape getOcclusionShape(BlockState p_60578_, BlockGetter p_60579_, BlockPos p_60580_) {
        return Shapes.empty();
    }

    @Override
    public BlockState updateShape(BlockState newState, Direction direction, BlockState oldState, LevelAccessor accessor, BlockPos newPos, BlockPos oldPos) {
        return !newState.canSurvive(accessor, newPos) ? Blocks.AIR.defaultBlockState() : super.updateShape(newState, direction, oldState, accessor, newPos, oldPos);
    }

    @Override
    public boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
        int layers = state.getValue(LAYERS);
        if (context.getItemInHand().is(this.asItem()) && layers < MAX_HEIGHT) {
            if (context.replacingClickedOnBlock()) {
                return context.getClickedFace() == Direction.UP;
            } else {
                return true;
            }
        } else {
            return layers == 1;
        }
    }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = context.getLevel().getBlockState(context.getClickedPos());

        if (state.is(this)) {
            int layers = state.getValue(LAYERS);
            return state.setValue(LAYERS, Math.min(MAX_HEIGHT, layers + 1));
        } else {
            return super.getStateForPlacement(context);
        }
    }

    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LAYERS);
    }
}
