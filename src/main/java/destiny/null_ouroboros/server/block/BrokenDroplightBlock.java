package destiny.null_ouroboros.server.block;

import destiny.null_ouroboros.server.registry.SoundRegistry;
import destiny.null_ouroboros.server.util.ModUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class BrokenDroplightBlock extends FallingBlock {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public static final VoxelShape UP    = ModUtil.buildShape(Block.box(0, 11, 0, 16, 16, 16));
    public static final VoxelShape DOWN  = ModUtil.buildShape(Block.box(0,  0, 0, 16,  5, 16));
    public static final VoxelShape NORTH = ModUtil.buildShape(Block.box(0,  0, 0, 16, 16,  5));
    public static final VoxelShape SOUTH = ModUtil.buildShape(Block.box(0,  0, 11,16, 16, 16));
    public static final VoxelShape WEST  = ModUtil.buildShape(Block.box(0,  0, 0,  5, 16, 16));
    public static final VoxelShape EAST  = ModUtil.buildShape(Block.box(11, 0, 0, 16, 16, 16));

    private static final int FLICKER_CHECK_DELAY_MIN = 20;
    private static final int FLICKER_CHECK_DELAY_MAX = 60;
    private static final float FLICKER_CHANCE = 0.5F;
    private static final int FLICKER_OFF_TICKS_MIN = 5;
    private static final int FLICKER_OFF_TICKS_MAX = 10;

    public BrokenDroplightBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.defaultBlockState().setValue(FACING, Direction.UP).setValue(POWERED, false).setValue(ACTIVE, false)
                .setValue(LIT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, POWERED, ACTIVE, LIT);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getClickedFace().getOpposite()).setValue(POWERED, false)
                .setValue(ACTIVE, false).setValue(LIT, false);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        if (!level.isClientSide) {
            if (oldState.getBlock() != this) {
                checkAndFlip(state, (ServerLevel) level, pos);

                BlockState currentState = level.getBlockState(pos);
                if (currentState.getBlock() == this && currentState.getValue(ACTIVE)) {
                    level.scheduleTick(pos, this, 0);

                    scheduleNextFlickerCheck((ServerLevel) level, pos);
                }
            }
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
            if (!wasPowered) {
                boolean active = !state.getValue(ACTIVE);
                BlockState newState = state.setValue(ACTIVE, active);

                if (active) {
                    newState = newState.setValue(LIT, true);

                    level.playSound(null, pos, SoundRegistry.DROPLIGHT_ON.get(), SoundSource.BLOCKS, 0.8f, 1f);

                    level.scheduleTick(pos, this, FLICKER_CHECK_DELAY_MIN + level.random.nextInt(FLICKER_CHECK_DELAY_MAX - FLICKER_CHECK_DELAY_MIN));
                } else {
                    newState = newState.setValue(LIT, false);
                    level.playSound(null, pos, SoundRegistry.DROPLIGHT_OFF.get(), SoundSource.BLOCKS, 0.8f, 1f);
                }

                level.setBlock(pos, newState.setValue(POWERED, hasPower), 3);
            } else {
                level.setBlock(pos, state.setValue(POWERED, false), 3);
            }
        }
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.isAir()) {
            Direction facing = state.getValue(FACING);

            if (!canSupportCenter(level, pos.relative(facing), facing.getOpposite()) && isFree(level.getBlockState(pos.below())) && pos.getY() >= level.getMinBuildHeight()) {
                FallingBlockEntity.fall(level, pos, state);
                return;
            }
        }

        if (!state.getValue(ACTIVE)) {
            return;
        }

        boolean lit = state.getValue(LIT);

        if (!lit) {
            level.setBlock(pos, state.setValue(LIT, true), 3);

            scheduleNextFlickerCheck(level, pos);
        } else {
            if (random.nextFloat() < FLICKER_CHANCE) {
                level.setBlock(pos, state.setValue(LIT, false), 3);

                level.playSound(null, pos, SoundRegistry.DROPLIGHT_FLICKER.get(), SoundSource.BLOCKS, 0.25f, 1f);

                level.scheduleTick(pos, this, random.nextInt(FLICKER_OFF_TICKS_MIN, FLICKER_OFF_TICKS_MAX + 1));
            } else {
                scheduleNextFlickerCheck(level, pos);
            }
        }
    }

    private void scheduleNextFlickerCheck(ServerLevel level, BlockPos pos) {
        level.scheduleTick(pos, this, FLICKER_CHECK_DELAY_MIN + level.random.nextInt(FLICKER_CHECK_DELAY_MAX - FLICKER_CHECK_DELAY_MIN));
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!level.isClientSide) {
            level.scheduleTick(pos, this, 0);
        }

        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter getter, BlockPos pos, CollisionContext context) {
        Direction facing = state.getValue(FACING);

        return switch (facing) {
            case UP    -> UP;
            case DOWN  -> DOWN;
            case NORTH -> NORTH;
            case SOUTH -> SOUTH;
            case WEST  -> WEST;
            case EAST  -> EAST;
        };
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource randomSource) {
    }
}