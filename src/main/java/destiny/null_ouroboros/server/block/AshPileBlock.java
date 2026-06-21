package destiny.null_ouroboros.server.block;

import destiny.null_ouroboros.server.registry.BlockRegistry;
import destiny.null_ouroboros.server.registry.ParticleTypeRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.ParticleUtils;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
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
            Block.box(0, 0, 0,
            16, 2, 16),
            Block.box(0, 0, 0,
                    16, 4, 16),
            Block.box(0, 0, 0,
                    16, 6, 16),
            Block.box(0, 0, 0,
                    16, 8, 16),
            Block.box(0, 0, 0,
                    16, 10, 16),
            Block.box(0, 0, 0,
                    16, 12, 16),
            Block.box(0, 0, 0,
                    16, 14, 16),
            Shapes.block()};
    public static final int HEIGHT_IMPASSABLE = 5;

    public AshPileBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.defaultBlockState().setValue(LAYERS, 1));
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource randomSource) {
        if (isFree(level.getBlockState(pos.below())) && pos.getY() >= level.getMinBuildHeight()) {
            FallingBlockEntity $$4 = FallingBlockEntity.fall(level, pos, state);
            this.falling($$4);
        }
    }

    public static boolean isFree(BlockState state) {
        return state.isAir() || state.is(BlockTags.FIRE) || state.liquid() || (state.canBeReplaced() && !state.is(BlockRegistry.ASH_PILE.get()));
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource randomSource) {
        if (randomSource.nextFloat() > 0.95f && level.getBlockState(pos.above()).isAir()) {
            ParticleUtils.spawnParticleBelow(level, pos.above(), level.random, ParticleTypeRegistry.ASH.get());
        }
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
        return SHAPE_BY_LAYER[state.getValue(LAYERS)];
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
    public boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
        int layers = state.getValue(LAYERS);
        if (context.getItemInHand().is(this.asItem()) && layers < MAX_HEIGHT) {
            if (context.replacingClickedOnBlock()) {
                return context.getClickedFace() == Direction.UP;
            } else {
                return false;
            }
        } else {
            return layers < MAX_HEIGHT;
        }
    }

    @Override
    public boolean isCollisionShapeFullBlock(BlockState state, BlockGetter getter, BlockPos pos) {
        return getter.getBlockState(pos).getValue(LAYERS) == 8;
    }

    @Override
    public boolean isOcclusionShapeFullBlock(BlockState state, BlockGetter getter, BlockPos pos) {
        return getter.getBlockState(pos).getValue(LAYERS) == 8;
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