package destiny.null_ouroboros.server.block;

import destiny.null_ouroboros.server.ash.AshAirtight;
import destiny.null_ouroboros.server.block.entity.IntakeFanBlockEntity;
import destiny.null_ouroboros.server.registry.BlockEntityRegistry;
import destiny.null_ouroboros.server.registry.CapabilityRegistry;
import destiny.null_ouroboros.server.vent.VentNetworkTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
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
import org.jetbrains.annotations.Nullable;

public class IntakeFanBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

    public IntakeFanBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH).setValue(POWERED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, POWERED);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(POWERED, false);
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        if (level.isClientSide) {
            return;
        }
        boolean signal = level.hasNeighborSignal(pos);
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof IntakeFanBlockEntity fan) {
            if (signal && !fan.wasRedstone()) {
                boolean next = !state.getValue(POWERED);
                level.setBlock(pos, state.setValue(POWERED, next), Block.UPDATE_ALL);
                VentNetworkTracker.markDirty(level);
            }
            fan.setWasRedstone(signal);
        }
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        if (!oldState.is(this)) {
            VentNetworkTracker.addDuct(level, pos);
        }
        VentNetworkTracker.markDirty(level);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            VentNetworkTracker.removeDuct(level, pos);
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (level instanceof Level realLevel) {
            VentNetworkTracker.markDirty(realLevel);
        }
        return state;
    }

    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return false;
    }

    @Override
    public int getLightBlock(BlockState state, BlockGetter level, BlockPos pos) {
        return level.getMaxLightLevel();
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new IntakeFanBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, BlockEntityRegistry.INTAKE_FAN_BLOCK_ENTITY.get(), IntakeFanBlockEntity::tick);
    }

    public static boolean hasExposedIntake(Level level, BlockPos pos, BlockState state) {
        for (Direction direction : Direction.values()) {
            if (AshAirtight.isOpenAirCell(level, pos.relative(direction))) {
                return true;
            }
        }
        return false;
    }

    public static boolean isExteriorValid(Level level, BlockPos pos, BlockState state) {
        if (!(level instanceof ServerLevel serverLevel)) {
            for (Direction direction : Direction.values()) {
                BlockPos neighbor = pos.relative(direction);
                if (AshAirtight.isOpenAirCell(level, neighbor) && AshAirtight.isSkyExposed(level, neighbor)) {
                    return true;
                }
            }
            return false;
        }
        return serverLevel.getCapability(CapabilityRegistry.ASH_ATMOSPHERE_CAPABILITY).map(cap -> {
            for (Direction direction : Direction.values()) {
                BlockPos neighbor = pos.relative(direction);
                if (AshAirtight.isOpenAirCell(level, neighbor) && cap.reachesExterior(serverLevel, neighbor)) {
                    return true;
                }
            }
            return false;
        }).orElse(false);
    }

    public static java.util.List<Direction> airFaces(Level level, BlockPos pos) {
        java.util.List<Direction> faces = new java.util.ArrayList<>(6);
        for (Direction direction : Direction.values()) {
            if (AshAirtight.isOpenAirCell(level, pos.relative(direction))) {
                faces.add(direction);
            }
        }
        return faces;
    }
}
