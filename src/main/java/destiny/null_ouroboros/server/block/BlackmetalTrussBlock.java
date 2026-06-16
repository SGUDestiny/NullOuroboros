package destiny.null_ouroboros.server.block;

import destiny.null_ouroboros.server.util.ModUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class BlackmetalTrussBlock extends HorizontalDirectionalBlock {
    public static final IntegerProperty CONNECTION = IntegerProperty.create("connection", 1, 4);

    public static final VoxelShape LOWER_CORNER_NORTH = ModUtil.buildShape(
            Block.box(5, 0, 9, 11, 12, 11),
            Block.box(5, 10, 11, 11, 16, 13),
            Block.box(6, 0, 11, 10, 10, 16),
            Block.box(6, 10, 13, 10, 16, 16)
    );
    public static final VoxelShape LOWER_CORNER_SOUTH = ModUtil.buildShape(
            Block.box(5, 0, 5, 11, 12, 7),
            Block.box(5, 10, 3, 11, 16, 5),
            Block.box(6, 0, 0, 10, 10, 5),
            Block.box(6, 10, 0, 10, 16, 3)
    );
    public static final VoxelShape LOWER_CORNER_WEST = ModUtil.buildShape(
            Block.box(9, 0, 5, 11, 12, 11),
            Block.box(11, 10, 5, 13, 16, 11),
            Block.box(11, 0, 6, 16, 10, 10),
            Block.box(13, 10, 6, 16, 16, 10)
    );
    public static final VoxelShape LOWER_CORNER_EAST = ModUtil.buildShape(
            Block.box(5, 0, 5, 7, 12, 11),
            Block.box(3, 10, 5, 5, 16, 11),
            Block.box(0, 0, 6, 5, 10, 10),
            Block.box(0, 10, 6, 3, 16, 10)
    );

    public static final VoxelShape SIDE_NORTH = ModUtil.buildShape(
            Block.box(5, 0, 11, 11, 16, 13),
            Block.box(6, 0, 13, 10, 16, 16)
    );
    public static final VoxelShape SIDE_SOUTH = ModUtil.buildShape(
            Block.box(5, 0, 3, 11, 16, 5),
            Block.box(6, 0, 0, 10, 16, 3)
    );
    public static final VoxelShape SIDE_WEST = ModUtil.buildShape(
            Block.box(11, 0, 5, 13, 16, 11),
            Block.box(13, 0, 6, 16, 16, 10)
    );
    public static final VoxelShape SIDE_EAST = ModUtil.buildShape(
            Block.box(3, 0, 5, 5, 16, 11),
            Block.box(0, 0, 6, 3, 16, 10)
    );

    public static final VoxelShape UPPER_CORNER_NORTH = ModUtil.buildShape(
            Block.box(5, 11, 0, 11, 13, 13),
            Block.box(5, 0, 11, 11, 11, 13),
            Block.box(8, 0, 0, 8, 11, 11),
            Block.box(6, 0, 13, 10, 16, 16),
            Block.box(6, 13, 0, 10, 16, 13)
    );
    public static final VoxelShape UPPER_CORNER_SOUTH = ModUtil.buildShape(
            Block.box(5, 11, 3, 11, 13, 16),
            Block.box(5, 0, 3, 11, 11, 5),
            Block.box(8, 0, 5, 8, 11, 16),
            Block.box(6, 0, 0, 10, 16, 3),
            Block.box(6, 13, 3, 10, 16, 16)
    );
    public static final VoxelShape UPPER_CORNER_WEST = ModUtil.buildShape(
            Block.box(0, 11, 5, 13, 13, 11),
            Block.box(11, 0, 5, 13, 11, 11),
            Block.box(0, 0, 8, 11, 11, 8),
            Block.box(13, 0, 6, 16, 16, 10),
            Block.box(0, 13, 6, 13, 16, 10)
    );
    public static final VoxelShape UPPER_CORNER_EAST = ModUtil.buildShape(
            Block.box(3, 11, 5, 16, 13, 11),
            Block.box(3, 0, 5, 5, 11, 11),
            Block.box(5, 0, 8, 16, 11, 8),
            Block.box(0, 0, 6, 3, 16, 10),
            Block.box(3, 13, 6, 16, 16, 10)
    );

    public static final VoxelShape UPPER_SIDE_NORTH_SOUTH = ModUtil.buildShape(
            Block.box(5, 11, 0, 11, 13, 16),
            Block.box(6, 13, 0, 10, 16, 16)
    );
    public static final VoxelShape UPPER_SIDE_WEST_EAST = ModUtil.buildShape(
            Block.box(0, 11, 5, 16, 13, 11),
            Block.box(0, 13, 6, 16, 16, 10)
    );

    public BlackmetalTrussBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.defaultBlockState().setValue(FACING, Direction.NORTH).setValue(CONNECTION, 1));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, CONNECTION);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter getter, BlockPos pos, CollisionContext context) {
        int connection = state.getValue(CONNECTION);
        Direction facing = state.getValue(FACING);

        if (connection == 1) {
            switch (facing) {
                case NORTH -> {
                    return LOWER_CORNER_NORTH;
                }
                case SOUTH -> {
                    return LOWER_CORNER_SOUTH;
                }
                case WEST -> {
                    return LOWER_CORNER_WEST;
                }
                case EAST -> {
                    return LOWER_CORNER_EAST;
                }
            }
        }
        if (connection == 2) {
            switch (facing) {
                case NORTH -> {
                    return SIDE_NORTH;
                }
                case SOUTH -> {
                    return SIDE_SOUTH;
                }
                case WEST -> {
                    return SIDE_WEST;
                }
                case EAST -> {
                    return SIDE_EAST;
                }
            }
        }
        if (connection == 3) {
            switch (facing) {
                case NORTH -> {
                    return UPPER_CORNER_NORTH;
                }
                case SOUTH -> {
                    return UPPER_CORNER_SOUTH;
                }
                case WEST -> {
                    return UPPER_CORNER_WEST;
                }
                case EAST -> {
                    return UPPER_CORNER_EAST;
                }
            }
        }
        if (connection == 4) {
            switch (facing) {
                case NORTH, SOUTH -> {
                    return UPPER_SIDE_NORTH_SOUTH;
                }
                case WEST, EAST -> {
                    return UPPER_SIDE_WEST_EAST;
                }
            }
        }

        return Shapes.block();
    }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Direction facing = context.getHorizontalDirection().getOpposite();

        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = pos.relative(dir);
            BlockState neighbor = level.getBlockState(neighborPos);

            if (neighbor.is(this)) {
                Direction neighborFacing = neighbor.getValue(FACING);

                if (canSupportCenter(level, pos.above(), Direction.DOWN) && canSupportCenter(level, pos.relative(neighborFacing), neighborFacing.getOpposite())
                        && !level.getBlockState(pos.relative(neighborFacing)).is(this)) {
                    facing = neighborFacing.getOpposite();
                } else {
                    facing = neighborFacing;
                }
                break;
            }
        }
        return getConnectionState(facing, this.defaultBlockState(), level, pos);
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
        if (!level.isClientSide) {
            BlockState newState = getConnectionState(state.getValue(FACING), state, level, pos);

            if (newState != state) {
                level.setBlock(pos, newState, Block.UPDATE_CLIENTS);
            }
        }

        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
    }

    public BlockState getConnectionState(Direction facing, BlockState state, Level level, BlockPos pos) {
        BlockState below = level.getBlockState(pos.below());
        BlockState above = level.getBlockState(pos.above());
        BlockState front = level.getBlockState(pos.relative(facing));
        BlockState behind = level.getBlockState(pos.relative(facing.getOpposite()));

        int connection;
        if (below.is(this) && !above.is(this) && (Block.canSupportCenter(level, pos.above(), Direction.DOWN) || front.is(this))) {
            connection = 3;
        } else if (!below.is(this) && Block.canSupportCenter(level, pos.below(), Direction.UP)) {
            connection = 1;
        } else if (below.is(this) || above.is(this)) {
            connection = 2;
        } else if (front.is(this) || behind.is(this)) {
            connection = 4;
        } else {
            connection = 2;
        }

        return state.setValue(FACING, facing).setValue(CONNECTION, connection);
    }
}
