package destiny.null_ouroboros.server.block;

import destiny.null_ouroboros.server.util.ModUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class HazardLeverBlock extends LeverBlock {
    public static final VoxelShape DOWN_AABB_X = ModUtil.buildShape(
            Block.box(3, 11, 5, 13, 16, 11)
    );
    public static final VoxelShape DOWN_AABB_Z = ModUtil.buildShape(
            Block.box(5, 11, 3, 11, 16, 13)
    );
    public static final VoxelShape UP_AABB_X = ModUtil.buildShape(
            Block.box(3, 0, 5, 13, 5, 11)
    );
    public static final VoxelShape UP_AABB_Z = ModUtil.buildShape(
            Block.box(5, 0, 3, 11, 5, 13)
    );
    public static final VoxelShape NORTH_AABB = ModUtil.buildShape(
            Block.box(5, 3, 11, 11, 13, 16)
    );
    public static final VoxelShape SOUTH_AABB = ModUtil.buildShape(
            Block.box(5, 3, 0, 11, 13, 5)
    );
    public static final VoxelShape WEST_AABB = ModUtil.buildShape(
            Block.box(11, 3, 5, 16, 13, 11)
    );
    public static final VoxelShape EAST_AABB = ModUtil.buildShape(
            Block.box(0, 3, 5, 5, 13, 11)
    );

    public final SoundEvent pressSound;
    public final SoundEvent unpressSound;

    public HazardLeverBlock(Properties properties, SoundEvent pressSound, SoundEvent unpressSound) {
        super(properties);
        this.pressSound = pressSound;
        this.unpressSound = unpressSound;
    }

    @Override
    public InteractionResult use(BlockState blockState, Level level, BlockPos blockPos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        } else {
            BlockState changedState = this.pull(blockState, level, blockPos);
            level.playSound(null, blockPos, changedState.getValue(POWERED) ? pressSound : unpressSound, SoundSource.BLOCKS, 1f, 1f);
            level.gameEvent(player, changedState.getValue(POWERED) ? GameEvent.BLOCK_ACTIVATE : GameEvent.BLOCK_DEACTIVATE, blockPos);
            return InteractionResult.CONSUME;
        }
    }



    public VoxelShape getShape(BlockState state, BlockGetter getter, BlockPos pos, CollisionContext context) {
        switch (state.getValue(FACE)) {
            case FLOOR:
                switch (state.getValue(FACING).getAxis()) {
                    case X:
                        return UP_AABB_X;
                    case Z:
                    default:
                        return UP_AABB_Z;
                }
            case WALL:
                switch (state.getValue(FACING)) {
                    case EAST:
                        return EAST_AABB;
                    case WEST:
                        return WEST_AABB;
                    case SOUTH:
                        return SOUTH_AABB;
                    case NORTH:
                    default:
                        return NORTH_AABB;
                }
            case CEILING:
            default:
                switch (state.getValue(FACING).getAxis()) {
                    case X:
                        return DOWN_AABB_X;
                    case Z:
                    default:
                        return DOWN_AABB_Z;
                }
        }
    }
}
