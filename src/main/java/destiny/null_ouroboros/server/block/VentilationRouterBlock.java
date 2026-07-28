package destiny.null_ouroboros.server.block;

import destiny.null_ouroboros.server.block.entity.VentilationRouterBlockEntity;
import destiny.null_ouroboros.server.camouflage.Camouflage;
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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class VentilationRouterBlock extends BaseEntityBlock {
    public static final VoxelShape SHAPE = ModUtil.buildShape(
            Block.box(2, 2, 0, 14, 14, 16),
            Block.box(0, 2, 2, 16, 14, 14)
    );

    public VentilationRouterBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(Camouflage.CAMOUFLAGED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(Camouflage.CAMOUFLAGED);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Camouflage.shapeOrCamouflage(SHAPE, level, pos);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return state.getValue(Camouflage.CAMOUFLAGED) ? RenderShape.INVISIBLE : RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(Camouflage.CAMOUFLAGED, false);
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
        VentNetworkTracker.markDirty(level);
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
        if (level instanceof Level realLevel) {
            VentNetworkTracker.markDirty(realLevel);
        }
        return state;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new VentilationRouterBlockEntity(pos, state);
    }
}
