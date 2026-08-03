package destiny.null_ouroboros.server.block;

import destiny.null_ouroboros.server.ash.AshAirtight;
import destiny.null_ouroboros.server.block.entity.VentilationShaftBlockEntity;
import destiny.null_ouroboros.server.camouflage.Camouflage;
import destiny.null_ouroboros.server.structure.StructurePlacement;
import destiny.null_ouroboros.server.util.ModUtil;
import destiny.null_ouroboros.server.vent.VentNetworkTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class VentilationShaftBlock extends BaseEntityBlock {
    public static final EnumProperty<VentilationShaftShape> SHAPE = EnumProperty.create("shape", VentilationShaftShape.class);

    private static final VoxelShape CORE = Block.box(3, 3, 3, 13, 13, 13);
    private static final VoxelShape ARM_NORTH = Block.box(3, 3, 0, 13, 13, 3);
    private static final VoxelShape ARM_SOUTH = Block.box(3, 3, 13, 13, 13, 16);
    private static final VoxelShape ARM_WEST = Block.box(0, 3, 3, 3, 13, 13);
    private static final VoxelShape ARM_EAST = Block.box(13, 3, 3, 16, 13, 13);
    private static final VoxelShape ARM_DOWN = Block.box(3, 0, 3, 13, 3, 13);
    private static final VoxelShape ARM_UP = Block.box(3, 13, 3, 13, 16, 13);

    public static final VoxelShape SHAPE_X = Block.box(0, 3, 3, 16, 13, 13);
    public static final VoxelShape SHAPE_Y = Block.box(3, 0, 3, 13, 16, 13);
    public static final VoxelShape SHAPE_Z = Block.box(3, 3, 0, 13, 13, 16);
    public static final VoxelShape SHAPE_NORTH_EAST = ModUtil.buildShape(CORE, ARM_NORTH, ARM_EAST);
    public static final VoxelShape SHAPE_NORTH_WEST = ModUtil.buildShape(CORE, ARM_NORTH, ARM_WEST);
    public static final VoxelShape SHAPE_NORTH_UP = ModUtil.buildShape(CORE, ARM_NORTH, ARM_UP);
    public static final VoxelShape SHAPE_NORTH_DOWN = ModUtil.buildShape(CORE, ARM_NORTH, ARM_DOWN);
    public static final VoxelShape SHAPE_SOUTH_EAST = ModUtil.buildShape(CORE, ARM_SOUTH, ARM_EAST);
    public static final VoxelShape SHAPE_SOUTH_WEST = ModUtil.buildShape(CORE, ARM_SOUTH, ARM_WEST);
    public static final VoxelShape SHAPE_SOUTH_UP = ModUtil.buildShape(CORE, ARM_SOUTH, ARM_UP);
    public static final VoxelShape SHAPE_SOUTH_DOWN = ModUtil.buildShape(CORE, ARM_SOUTH, ARM_DOWN);
    public static final VoxelShape SHAPE_EAST_UP = ModUtil.buildShape(CORE, ARM_EAST, ARM_UP);
    public static final VoxelShape SHAPE_EAST_DOWN = ModUtil.buildShape(CORE, ARM_EAST, ARM_DOWN);
    public static final VoxelShape SHAPE_WEST_UP = ModUtil.buildShape(CORE, ARM_WEST, ARM_UP);
    public static final VoxelShape SHAPE_WEST_DOWN = ModUtil.buildShape(CORE, ARM_WEST, ARM_DOWN);

    public VentilationShaftBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState()
                .setValue(SHAPE, VentilationShaftShape.Z)
                .setValue(Camouflage.CAMOUFLAGED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(SHAPE, Camouflage.CAMOUFLAGED);
    }

    private static VoxelShape intrinsicShape(BlockState state) {
        return switch (state.getValue(SHAPE)) {
            case X -> SHAPE_X;
            case Y -> SHAPE_Y;
            case Z -> SHAPE_Z;
            case NORTH_EAST -> SHAPE_NORTH_EAST;
            case NORTH_WEST -> SHAPE_NORTH_WEST;
            case NORTH_UP -> SHAPE_NORTH_UP;
            case NORTH_DOWN -> SHAPE_NORTH_DOWN;
            case SOUTH_EAST -> SHAPE_SOUTH_EAST;
            case SOUTH_WEST -> SHAPE_SOUTH_WEST;
            case SOUTH_UP -> SHAPE_SOUTH_UP;
            case SOUTH_DOWN -> SHAPE_SOUTH_DOWN;
            case EAST_UP -> SHAPE_EAST_UP;
            case EAST_DOWN -> SHAPE_EAST_DOWN;
            case WEST_UP -> SHAPE_WEST_UP;
            case WEST_DOWN -> SHAPE_WEST_DOWN;
        };
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Camouflage.shapeOrCamouflage(intrinsicShape(state), level, pos);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return state.getValue(Camouflage.CAMOUFLAGED) ? RenderShape.INVISIBLE : RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction face = context.getClickedFace();
        return defaultBlockState()
                .setValue(SHAPE, resolveShape(context.getLevel(), context.getClickedPos(), face, null))
                .setValue(Camouflage.CAMOUFLAGED, false);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.isEmpty()) {
            return Camouflage.tryRemove(level, pos, player);
        }
        return Camouflage.tryApply(level, pos, player, stack);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        if (!oldState.is(this)) {
            VentNetworkTracker.addDuct(level, pos);
        }
        if (!level.isClientSide) {
            if (!StructurePlacement.isPlacing()) {
                VentilationShaftShape current = state.getValue(SHAPE);
                VentilationShaftShape shape = resolveShape(level, pos, current.first(), current);
                if (shape != current) {
                    level.setBlock(pos, state.setValue(SHAPE, shape), Block.UPDATE_ALL);
                }
            }
            VentNetworkTracker.markDirty(level);
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            Camouflage.dropCamouflage(level, pos);
            VentNetworkTracker.removeDuct(level, pos);
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (level instanceof Level realLevel && !realLevel.isClientSide) {
            VentNetworkTracker.markDirty(realLevel);
            if (!StructurePlacement.isPlacing()) {
                VentilationShaftShape current = state.getValue(SHAPE);
                VentilationShaftShape shape = resolveShape(realLevel, pos, current.first(), current);
                if (shape != current) {
                    return state.setValue(SHAPE, shape);
                }
            }
        }
        return state;
    }

    public static boolean connects(BlockState state, Direction direction) {
        return state.getValue(SHAPE).connects(direction);
    }

    public static List<Direction> openEnds(BlockState state, Level level, BlockPos pos) {
        List<Direction> ends = new ArrayList<>(2);
        VentilationShaftShape shape = state.getValue(SHAPE);
        for (Direction direction : new Direction[]{shape.first(), shape.second()}) {
            BlockPos next = pos.relative(direction);
            BlockState neighbor = level.getBlockState(next);
            if (!AshAirtight.isDuctBlock(neighbor) || !neighborConnects(neighbor, direction.getOpposite())) {
                ends.add(direction);
            }
        }
        return ends;
    }

    private static boolean neighborConnects(BlockState neighbor, Direction towardShaft) {
        if (neighbor.getBlock() instanceof VentilationShaftBlock) {
            return VentilationShaftBlock.connects(neighbor, towardShaft);
        }
        if (neighbor.getBlock() instanceof VentilationRouterBlock) {
            return towardShaft.getAxis().isHorizontal();
        }
        if (neighbor.getBlock() instanceof IntakeFanBlock || neighbor.getBlock() instanceof OutputVentBlock) {
            return true;
        }
        return false;
    }

    private static boolean hasConnection(Level level, BlockPos pos, Direction direction, @Nullable VentilationShaftShape current) {
        BlockPos neighborPos = pos.relative(direction);
        if (!level.isLoaded(neighborPos)) {
            return current != null && current.connects(direction);
        }
        BlockState neighbor = level.getBlockState(neighborPos);
        if (!AshAirtight.isDuctBlock(neighbor)) {
            return false;
        }
        return neighborConnects(neighbor, direction.getOpposite());
    }

    private static VentilationShaftShape resolveShape(Level level, BlockPos pos, Direction fallback, @Nullable VentilationShaftShape current) {
        List<Direction> connections = new ArrayList<>(2);
        if (current != null) {
            addUniqueConnection(connections, current.first(), level, pos, current);
            addUniqueConnection(connections, current.second(), level, pos, current);
            if (connections.size() == 2) {
                return VentilationShaftShape.from(connections.get(0), connections.get(1));
            }
        }
        for (Direction direction : Direction.values()) {
            if (connections.size() == 2) {
                break;
            }
            addUniqueConnection(connections, direction, level, pos, current);
        }
        if (connections.isEmpty()) {
            return VentilationShaftShape.fromAxis(fallback.getAxis());
        }
        if (connections.size() == 1) {
            Direction only = connections.get(0);
            return VentilationShaftShape.from(only, only.getOpposite());
        }
        return VentilationShaftShape.from(connections.get(0), connections.get(1));
    }

    private static void addUniqueConnection(List<Direction> connections, Direction direction, Level level, BlockPos pos, @Nullable VentilationShaftShape current) {
        if (connections.contains(direction) || !hasConnection(level, pos, direction, current)) {
            return;
        }
        connections.add(direction);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new VentilationShaftBlockEntity(pos, state);
    }
}
