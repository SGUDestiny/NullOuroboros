package destiny.null_ouroboros.server.block;

import destiny.null_ouroboros.server.ash.AshAirtight;
import destiny.null_ouroboros.server.block.entity.OutputVentBlockEntity;
import destiny.null_ouroboros.server.item.FilterItem;
import destiny.null_ouroboros.server.item.RespiratorGear;
import destiny.null_ouroboros.server.item.WrenchItem;
import destiny.null_ouroboros.server.registry.BlockEntityRegistry;
import destiny.null_ouroboros.server.registry.SoundRegistry;
import destiny.null_ouroboros.server.vent.VentNetworkTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class OutputVentBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public static final EnumProperty<OutputVentMode> MODE = EnumProperty.create("mode", OutputVentMode.class);

    public OutputVentBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState()
                .setValue(FACING, Direction.NORTH)
                .setValue(POWERED, false)
                .setValue(MODE, OutputVentMode.OFF));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, POWERED, MODE);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(POWERED, false)
                .setValue(MODE, OutputVentMode.OFF);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof OutputVentBlockEntity vent)) {
            return InteractionResult.PASS;
        }
        ItemStack stack = player.getItemInHand(hand);
        if (stack.getItem() instanceof WrenchItem) {
            if (level.isClientSide) {
                return InteractionResult.SUCCESS;
            }
            ItemStack filter = vent.getFilter();
            if (filter.isEmpty()) {
                player.displayClientMessage(Component.literal("No filter installed"), true);
            } else {
                int left = RespiratorGear.FILTER_MAX_DAMAGE - filter.getDamageValue();
                player.displayClientMessage(Component.literal("Filter durability: " + left + "/" + RespiratorGear.FILTER_MAX_DAMAGE), true);
            }
            return InteractionResult.CONSUME;
        }
        if (stack.getItem() instanceof FilterItem) {
            if (level.isClientSide) {
                return InteractionResult.SUCCESS;
            }
            if (vent.insertFilter(stack)) {
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
                level.playSound(null, pos, SoundRegistry.RESPIRATOR_FILTER_SCREW.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
                updateMode(level, pos, state, vent);
                return InteractionResult.CONSUME;
            }
            return InteractionResult.PASS;
        }
        if (stack.isEmpty()) {
            if (level.isClientSide) {
                return InteractionResult.SUCCESS;
            }
            ItemStack removed = vent.removeFilter();
            if (!removed.isEmpty()) {
                if (!player.getInventory().add(removed)) {
                    player.drop(removed, false);
                }
                level.playSound(null, pos, SoundRegistry.RESPIRATOR_FILTER_SCREW.get(), SoundSource.BLOCKS, 1.0F, 0.85F);
                updateMode(level, pos, state, vent);
                return InteractionResult.CONSUME;
            }
            return InteractionResult.PASS;
        }
        return InteractionResult.PASS;
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        if (level.isClientSide) {
            return;
        }
        boolean signal = level.hasNeighborSignal(pos);
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof OutputVentBlockEntity vent) {
            if (signal && !vent.wasRedstone()) {
                boolean next = !state.getValue(POWERED);
                BlockState updated = state.setValue(POWERED, next);
                level.setBlock(pos, updated, Block.UPDATE_ALL);
                updateMode(level, pos, updated, vent);
                VentNetworkTracker.markDirty(level);
            }
            vent.setWasRedstone(signal);
        }
    }

    public static void updateMode(Level level, BlockPos pos, BlockState state, OutputVentBlockEntity vent) {
        OutputVentMode mode = OutputVentMode.OFF;
        if (state.getValue(POWERED)) {
            mode = vent.hasWorkingFilter() && vent.isAtmosphereActive()
                    ? OutputVentMode.ON
                    : OutputVentMode.ON_BROKEN;
        }
        if (state.getValue(MODE) != mode) {
            level.setBlock(pos, state.setValue(MODE, mode), Block.UPDATE_ALL);
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
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof OutputVentBlockEntity vent) {
                vent.dropFilter(level, pos);
            }
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
    public float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return 1.0F;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new OutputVentBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, BlockEntityRegistry.OUTPUT_VENT_BLOCK_ENTITY.get(), OutputVentBlockEntity::tick);
    }

    public static BlockPos outletPos(BlockPos pos, BlockState state) {
        return pos.relative(state.getValue(FACING));
    }

    public static boolean hasExposedOutlet(Level level, BlockPos pos, BlockState state) {
        for (Direction direction : Direction.values()) {
            if (AshAirtight.isOpenAirCell(level, pos.relative(direction))) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    public static BlockPos firstAirNeighbor(Level level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            BlockPos neighbor = pos.relative(direction);
            if (AshAirtight.isOpenAirCell(level, neighbor)) {
                return neighbor;
            }
        }
        return null;
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
