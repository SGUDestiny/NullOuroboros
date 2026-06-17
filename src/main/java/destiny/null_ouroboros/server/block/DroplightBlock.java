package destiny.null_ouroboros.server.block;

import destiny.null_ouroboros.server.entity.FallingDroplightBlockEntity;
import destiny.null_ouroboros.server.registry.SoundRegistry;
import destiny.null_ouroboros.server.util.ModUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class DroplightBlock extends FallingBlock {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public static final VoxelShape UP = ModUtil.buildShape(
            Block.box(0, 11, 0, 16, 16, 16)
    );
    public static final VoxelShape DOWN = ModUtil.buildShape(
            Block.box(0, 0, 0, 16, 5, 16)
    );
    public static final VoxelShape NORTH = ModUtil.buildShape(
            Block.box(0, 0, 0, 16, 16, 5)
    );
    public static final VoxelShape SOUTH = ModUtil.buildShape(
            Block.box(0, 0, 11, 16, 16, 16)
    );
    public static final VoxelShape WEST = ModUtil.buildShape(
            Block.box(0, 0, 0, 5, 16, 16)
    );
    public static final VoxelShape EAST = ModUtil.buildShape(
            Block.box(11, 0, 0, 16, 16, 16)
    );

    public DroplightBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.defaultBlockState().setValue(FACING, Direction.UP).setValue(POWERED, false).setValue(LIT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, POWERED, LIT);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getClickedFace().getOpposite()).setValue(POWERED, false).setValue(LIT, false);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        if (!level.isClientSide && oldState.getBlock() != this) {
            checkAndFlip(state, (ServerLevel) level, pos);
        }
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        if (!level.isClientSide) {
            checkAndFlip(state, (ServerLevel) level, pos);
        }
    }

    private void checkAndFlip(BlockState state, ServerLevel level, BlockPos pos) {
        boolean hasPower = level.hasNeighborSignal(pos);
        boolean wasPowered = state.getValue(POWERED);

        if (hasPower != wasPowered) {
            BlockState newState = state;

            if (!wasPowered) {
                newState = newState.cycle(LIT);

                boolean lit = newState.getValue(LIT);
                level.playSound(null, pos, lit ? SoundRegistry.DROPLIGHT_ON.get() : SoundRegistry.DROPLIGHT_OFF.get(), SoundSource.BLOCKS, 0.8f, 1f);
            }

            level.setBlock(pos, newState.setValue(POWERED, hasPower), 3);
        }
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter getter, BlockPos pos, CollisionContext context) {
        Direction facing = state.getValue(FACING);

        switch (facing) {
            case UP -> {
                return UP;
            }
            case DOWN -> {
                return DOWN;
            }
            case NORTH -> {
                return NORTH;
            }
            case SOUTH -> {
                return SOUTH;
            }
            case WEST -> {
                return WEST;
            }
            case EAST -> {
                return EAST;
            }
        }

        return UP;
    }

    public void tick(BlockState state, ServerLevel serverLevel, BlockPos pos, RandomSource randomSource) {
        Direction facing = state.getValue(FACING);

        if (!canSupportCenter(serverLevel, pos.relative(facing), facing.getOpposite()) && isFree(serverLevel.getBlockState(pos.below())) && pos.getY() >= serverLevel.getMinBuildHeight()) {
            FallingDroplightBlockEntity entity = FallingDroplightBlockEntity.fall(serverLevel, pos, state);
            this.falling(entity);
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource randomSource) {
    }
}
