package destiny.null_ouroboros.server.block;

import destiny.null_ouroboros.server.block.entity.DustyComputerBlockEntity;
import destiny.null_ouroboros.server.registry.BlockEntityRegistry;
import destiny.null_ouroboros.server.registry.SoundRegistry;
import destiny.null_ouroboros.server.util.ModUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class DustyComputerBlock extends BaseEntityBlock {
    public static final DirectionProperty HORIZONTAL_FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty POWERED = BooleanProperty.create("powered");

    public static final VoxelShape SHAPE_NORTH = ModUtil.buildShape(
            Block.box(5, 0, 5, 16, 13, 16),
            Block.box(0, 0, 5, 5, 12, 16),
            Block.box(2, 0, 1, 5, 2, 5),
            Block.box(5, 0, 0, 16, 1, 5)
    );
    public static final VoxelShape SHAPE_SOUTH = ModUtil.buildShape(
            Block.box(0, 0, 0, 11, 13, 11),
            Block.box(11, 0, 0, 16, 12, 11),
            Block.box(0, 0, 11, 11, 1, 16),
            Block.box(11, 0, 11, 14, 2, 15)
    );
    public static final VoxelShape SHAPE_WEST = ModUtil.buildShape(
            Block.box(5, 0, 0, 16, 13, 11),
            Block.box(5, 0, 11, 16, 12, 16),
            Block.box(0, 0, 0, 5, 1, 11),
            Block.box(1, 0, 11, 5, 2, 14)
    );
    public static final VoxelShape SHAPE_EAST = ModUtil.buildShape(
            Block.box(0, 0, 5, 11, 13, 16),
            Block.box(0, 0, 0, 11, 12, 5),
            Block.box(11, 0, 5, 16, 1, 16),
            Block.box(11, 0, 2, 15, 2, 5)
    );

    public DustyComputerBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.defaultBlockState().setValue(HORIZONTAL_FACING, Direction.NORTH).setValue(POWERED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HORIZONTAL_FACING, POWERED);
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
        return RenderShape.MODEL;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        boolean powered = state.getValue(POWERED);

        if (player.isShiftKeyDown()) {
            if (powered) {
                level.setBlock(pos, state.setValue(POWERED, false), 3);
                level.playSound(null, pos, SoundRegistry.DUSTY_COMPUTER_STOP.get(), SoundSource.BLOCKS, 0.8f, 1f);

                return InteractionResult.SUCCESS;
            }
        }

        if (!powered) {
            level.setBlock(pos, state.setValue(POWERED, true), 3);
            level.playSound(null, pos, SoundRegistry.DUSTY_COMPUTER_START.get(), SoundSource.BLOCKS, 0.8f, 1f);

            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection();

        return this.defaultBlockState().setValue(HORIZONTAL_FACING, facing.getOpposite()).setValue(POWERED, false);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DustyComputerBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, BlockEntityRegistry.DUSTY_COMPUTER_BLOCK_ENTITY.get(), DustyComputerBlockEntity::tick);
    }
}
