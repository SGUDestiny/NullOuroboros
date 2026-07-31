package destiny.null_ouroboros.server.block;

import destiny.null_ouroboros.server.block.entity.GarageDoorBlockEntity;
import destiny.null_ouroboros.server.registry.BlockEntityRegistry;
import destiny.null_ouroboros.server.util.ModUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class GarageDoorBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<GarageDoorPart> PART = EnumProperty.create("part", GarageDoorPart.class);
    public static final IntegerProperty OPEN_STAGE = IntegerProperty.create("open_stage", 0, 3);

    public static final VoxelShape SHAPE_NORTH = ModUtil.buildShape(Block.box(0, 0, 6, 16, 16, 10));
    public static final VoxelShape SHAPE_SOUTH = ModUtil.buildShape(Block.box(0, 0, 6, 16, 16, 10));
    public static final VoxelShape SHAPE_WEST = ModUtil.buildShape(Block.box(6, 0, 0, 10, 16, 16));
    public static final VoxelShape SHAPE_EAST = ModUtil.buildShape(Block.box(6, 0, 0, 10, 16, 16));

    private static boolean destroying;

    public GarageDoorBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState()
                .setValue(FACING, Direction.NORTH)
                .setValue(PART, GarageDoorPart.CENTER_BOT)
                .setValue(OPEN_STAGE, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, PART, OPEN_STAGE);
    }

    public static VoxelShape shapeForFacing(Direction facing) {
        return switch (facing) {
            case SOUTH -> SHAPE_SOUTH;
            case WEST -> SHAPE_WEST;
            case EAST -> SHAPE_EAST;
            default -> SHAPE_NORTH;
        };
    }

    public static BlockPos controllerPos(BlockPos pos, BlockState state) {
        Direction facing = state.getValue(FACING);
        GarageDoorPart part = state.getValue(PART);
        BlockPos offset = part.offset(facing);
        return pos.subtract(offset);
    }

    public static BlockPos partPos(BlockPos controller, Direction facing, GarageDoorPart part) {
        return controller.offset(part.offset(facing));
    }

    public static boolean isStructurePowered(Level level, BlockPos controller, Direction facing) {
        for (GarageDoorPart part : GarageDoorPart.values()) {
            if (level.hasNeighborSignal(partPos(controller, facing, part))) {
                return true;
            }
        }
        return false;
    }

    public static boolean isPassable(BlockState state) {
        int stage = state.getValue(OPEN_STAGE);
        GarageDoorPart part = state.getValue(PART);
        if (stage >= 3) {
            return true;
        }
        if (stage >= 2) {
            return part.isBottomRow() || part.isMiddleRow();
        }
        return stage >= 1 && part.isBottomRow();
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapeForFacing(state.getValue(FACING));
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (isPassable(state)) {
            return Shapes.empty();
        }
        return shapeForFacing(state.getValue(FACING));
    }

    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return isPassable(state);
    }

    @Override
    public int getLightBlock(BlockState state, BlockGetter level, BlockPos pos) {
        return isPassable(state) ? 0 : level.getMaxLightLevel();
    }

    @Override
    public float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return 1.0F;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        BlockPos controller = context.getClickedPos();
        Level level = context.getLevel();

        if (controller.getY() + 2 >= level.getMaxBuildHeight()) {
            return null;
        }

        for (GarageDoorPart part : GarageDoorPart.values()) {
            BlockPos pos = partPos(controller, facing, part);
            if (!level.getBlockState(pos).canBeReplaced(context)) {
                return null;
            }
        }

        return defaultBlockState()
                .setValue(FACING, facing)
                .setValue(PART, GarageDoorPart.CENTER_BOT)
                .setValue(OPEN_STAGE, 0);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        Direction facing = state.getValue(FACING);
        for (GarageDoorPart part : GarageDoorPart.values()) {
            if (part.isController()) {
                continue;
            }
            level.setBlock(partPos(pos, facing, part),
                    state.setValue(PART, part),
                    Block.UPDATE_ALL);
        }
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        GarageDoorPart part = state.getValue(PART);
        Direction facing = state.getValue(FACING);
        if (part.isController()) {
            return pos.getY() + 2 < level.getMaxBuildHeight();
        }
        BlockPos controller = controllerPos(pos, state);
        BlockState controllerState = level.getBlockState(controller);
        return controllerState.is(this)
                && controllerState.getValue(PART).isController()
                && controllerState.getValue(FACING) == facing;
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (!canSurvive(state, level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide) {
            if (!state.getValue(PART).isController() && !player.isCreative()) {
                popResource(level, pos, new ItemStack(this));
            }
            destroySiblings(level, pos, state, player);
        }
        super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock()) && !level.isClientSide) {
            destroySiblings(level, pos, state, null);
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    private void destroySiblings(Level level, BlockPos pos, BlockState state, @Nullable Player player) {
        if (destroying) {
            return;
        }
        destroying = true;
        try {
            Direction facing = state.getValue(FACING);
            BlockPos controller = controllerPos(pos, state);
            for (GarageDoorPart part : GarageDoorPart.values()) {
                BlockPos partPos = partPos(controller, facing, part);
                if (partPos.equals(pos)) {
                    continue;
                }
                BlockState partState = level.getBlockState(partPos);
                if (partState.is(this)) {
                    level.setBlock(partPos, Blocks.AIR.defaultBlockState(),
                            Block.UPDATE_NEIGHBORS | Block.UPDATE_CLIENTS | Block.UPDATE_SUPPRESS_DROPS);
                    if (player != null) {
                        level.levelEvent(player, LevelEvent.PARTICLES_DESTROY_BLOCK, partPos, Block.getId(partState));
                    }
                }
            }
        } finally {
            destroying = false;
        }
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        if (level.isClientSide) {
            return;
        }

        BlockPos controller = controllerPos(pos, state);
        if (!(level.getBlockEntity(controller) instanceof GarageDoorBlockEntity garageDoor)) {
            return;
        }

        if (garageDoor.isCycling()) {
            return;
        }

        Direction facing = state.getValue(FACING);
        boolean powered = isStructurePowered(level, controller, facing);
        if (powered && !garageDoor.getLastRedstone()) {
            garageDoor.tryStartCycle();
        }
        garageDoor.setLastRedstone(powered);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (!state.getValue(PART).isController()) {
            return null;
        }
        return createTickerHelper(type, BlockEntityRegistry.GARAGE_DOOR_BLOCK_ENTITY.get(), GarageDoorBlockEntity::tick);
    }

    @Override
    @Nullable
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        if (!state.getValue(PART).isController()) {
            return null;
        }
        return BlockEntityRegistry.GARAGE_DOOR_BLOCK_ENTITY.get().create(pos, state);
    }
}
