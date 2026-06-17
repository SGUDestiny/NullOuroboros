package destiny.null_ouroboros.server.entity;

import destiny.null_ouroboros.server.registry.BlockRegistry;
import destiny.null_ouroboros.server.registry.SoundRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

import static destiny.null_ouroboros.server.block.BrokenDroplightBlock.ACTIVE;
import static destiny.null_ouroboros.server.block.DroplightBlock.*;

public class FallingDroplightBlockEntity extends FallingBlockEntity {
    public FallingDroplightBlockEntity(EntityType<? extends FallingBlockEntity> type, Level level) {
        super(type, level);
    }

    private FallingDroplightBlockEntity(Level level, double x, double y, double z, BlockState state) {
        this(EntityType.FALLING_BLOCK, level);
        this.blockState = state;
        this.blocksBuilding = true;
        this.setPos(x, y, z);
        this.setDeltaMovement(Vec3.ZERO);
        this.xo = x;
        this.yo = y;
        this.zo = z;
        this.setStartPos(this.blockPosition());
    }

    public static FallingDroplightBlockEntity fall(Level level, BlockPos pos, BlockState state) {
        FallingDroplightBlockEntity fallingblockentity = new FallingDroplightBlockEntity(level, (double)pos.getX() + (double)0.5F, (double)pos.getY(), (double)pos.getZ() + (double)0.5F, state.hasProperty(BlockStateProperties.WATERLOGGED) ? (BlockState)state.setValue(BlockStateProperties.WATERLOGGED, false) : state);
        level.setBlock(pos, state.getFluidState().createLegacyBlock(), 3);
        level.addFreshEntity(fallingblockentity);
        return fallingblockentity;
    }

    @Override
    public void tick() {
        Level level = this.level();

        if (this.getBlockState().isAir()) {
            this.discard();
            return;
        }
        this.time++;
        if (!this.isNoGravity()) {
            this.setDeltaMovement(this.getDeltaMovement().add(0.0, -0.04, 0.0));
        }
        this.move(MoverType.SELF, this.getDeltaMovement());

        if (!level.isClientSide && this.onGround()) {
            this.discard();

            BlockState state = this.getBlockState();
            BlockState brokenState = BlockRegistry.BROKEN_DROPLIGHT.get().defaultBlockState().setValue(FACING, state.getValue(FACING))
                    .setValue(LIT, state.getValue(LIT)).setValue(POWERED, state.getValue(POWERED)).setValue(ACTIVE, state.getValue(LIT));

            level.setBlock(this.blockPosition(), brokenState, 3);

            level.playSound(null, this.blockPosition(), SoundRegistry.DROPLIGHT_DROP.get(), SoundSource.BLOCKS, 1f, 1f);
        } else if (this.time > 600) {
            this.discard();
        }
    }
}
