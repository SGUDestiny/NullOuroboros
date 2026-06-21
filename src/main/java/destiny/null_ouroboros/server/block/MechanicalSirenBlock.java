package destiny.null_ouroboros.server.block;

import destiny.null_ouroboros.server.block.entity.MechanicalSirenBlockEntity;
import destiny.null_ouroboros.server.registry.BlockEntityRegistry;
import destiny.null_ouroboros.server.util.ModUtil;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class MechanicalSirenBlock extends BaseEntityBlock {
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    public static final VoxelShape SHAPE = ModUtil.buildShape(
            Block.box(0, 5, 0, 16, 10, 16),
            Block.box(5, 0, 5, 11, 5, 11),
            Block.box(3, 10, 3, 13, 12, 13)
    );

    public MechanicalSirenBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.defaultBlockState().setValue(POWERED, false).setValue(ACTIVE, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(POWERED, ACTIVE);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter getter, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(POWERED, false).setValue(ACTIVE, false);
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
        BlockState newState = state.setValue(POWERED, hasPower);
        if (newState != state) {
            level.setBlock(pos, newState, 3);
        }

        if (level.getBlockEntity(pos) instanceof MechanicalSirenBlockEntity sirenEntity) {
            sirenEntity.checkRedstone(hasPower);
        }
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new MechanicalSirenBlockEntity(blockPos, blockState);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, BlockEntityRegistry.MECHANICAL_SIREN_BLOCK_ENTITY.get(), MechanicalSirenBlockEntity::tick);
    }
}
