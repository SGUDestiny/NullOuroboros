package destiny.null_ouroboros.server.block;

import destiny.null_ouroboros.server.block.entity.FuseBoxBlockEntity;
import destiny.null_ouroboros.server.registry.BlockEntityRegistry;
import destiny.null_ouroboros.server.util.ModUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
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
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

public class FuseBoxBlock extends BaseEntityBlock {
    public static final DirectionProperty HORIZONTAL_FACING = BlockStateProperties.HORIZONTAL_FACING;

    public static final VoxelShape SHAPE_NORTH = ModUtil.buildShape(
            Block.box(2, 0, 11, 14, 16, 16)
    );
    public static final VoxelShape SHAPE_SOUTH = ModUtil.buildShape(
            Block.box(2, 0, 0, 14, 16, 5)
    );
    public static final VoxelShape SHAPE_WEST = ModUtil.buildShape(
            Block.box(11, 0, 2, 16, 16, 14)
    );
    public static final VoxelShape SHAPE_EAST = ModUtil.buildShape(
            Block.box(0, 0, 2, 5, 16, 14)
    );

    public FuseBoxBlock(Properties properties) {
        super(properties);
        registerDefaultState(this.defaultBlockState().setValue(HORIZONTAL_FACING, Direction.NORTH));
    }

    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HORIZONTAL_FACING);
    }

    @Override
    public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        switch (pState.getValue(HORIZONTAL_FACING)) {
            case NORTH:
                return SHAPE_NORTH;
            case SOUTH:
                return SHAPE_SOUTH;
            case EAST:
                return SHAPE_EAST;
            case WEST:
                return SHAPE_WEST;
            default:
                return SHAPE_NORTH;
        }
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction clickedFace = context.getClickedFace();

        if (clickedFace.getAxis() == Direction.Axis.Y) return null;

        Direction facing = context.getHorizontalDirection();

        return this.defaultBlockState().setValue(HORIZONTAL_FACING, facing.getOpposite());
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        if (!(level.getBlockEntity(pos) instanceof FuseBoxBlockEntity box)) {
            return InteractionResult.PASS;
        }

        if (box.isCycling()) {
            return InteractionResult.SUCCESS;
        }

        if (player.isShiftKeyDown()) {
            box.tryStartCycle();
            return InteractionResult.SUCCESS;
        }

        MenuProvider provider = state.getMenuProvider(level, pos);
        if (provider == null) {
            return InteractionResult.PASS;
        }

        NetworkHooks.openScreen((ServerPlayer) player, provider, buf -> buf.writeBlockPos(pos));
        return InteractionResult.SUCCESS;
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        if (level.isClientSide) {
            return;
        }
        if (!(level.getBlockEntity(pos) instanceof FuseBoxBlockEntity box)) {
            return;
        }
        if (box.isCycling()) {
            return;
        }

        boolean powered = level.hasNeighborSignal(pos);
        if (powered && !box.getLastRedstone()) {
            box.tryStartCycle();
        }
        box.setLastRedstone(powered);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof FuseBoxBlockEntity box) {
            box.onRemove();
            ItemStackHandler inventory = box.getInventory();
            for (int i = 0; i < inventory.getSlots(); i++) {
                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), inventory.getStackInSlot(i));
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntity) {
        return createTickerHelper(blockEntity, BlockEntityRegistry.FUSE_BOX_BLOCK_ENTITY.get(), FuseBoxBlockEntity::tick);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return BlockEntityRegistry.FUSE_BOX_BLOCK_ENTITY.get().create(blockPos, blockState);
    }

    @Nullable
    @Override
    public MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        return be instanceof MenuProvider provider ? provider : null;
    }
}
