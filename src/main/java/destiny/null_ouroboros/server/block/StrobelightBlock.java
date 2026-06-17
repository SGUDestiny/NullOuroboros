package destiny.null_ouroboros.server.block;

import destiny.null_ouroboros.server.block.entity.StrobelightBlockEntity;
import destiny.null_ouroboros.server.registry.BlockEntityRegistry;
import destiny.null_ouroboros.server.util.ModUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class StrobelightBlock extends BaseEntityBlock {
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

    public StrobelightBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.defaultBlockState().setValue(FACING, Direction.UP).setValue(POWERED, false).setValue(LIT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, POWERED, LIT);
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

    @Override
    public RenderShape getRenderShape(BlockState p_49232_) {
        return RenderShape.INVISIBLE;
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
            }

            level.setBlock(pos, newState.setValue(POWERED, hasPower), 3);
        }
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new StrobelightBlockEntity(blockPos, blockState);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, BlockEntityRegistry.STROBELIGHT_BLOCK_ENTITY.get(), StrobelightBlockEntity::tick);
    }
}
