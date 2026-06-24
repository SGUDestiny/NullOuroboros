package destiny.null_ouroboros.server.entity;

import destiny.null_ouroboros.server.block.AshPileBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class FallingAshPileBlockEntity extends FallingBlockEntity {
    public FallingAshPileBlockEntity(EntityType<? extends FallingBlockEntity> type, Level level) {
        super(type, level);
    }

    private FallingAshPileBlockEntity(Level level, double x, double y, double z, BlockState state) {
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

    public static FallingAshPileBlockEntity fall(Level level, BlockPos pos, BlockState state) {
        FallingAshPileBlockEntity entity = new FallingAshPileBlockEntity(level,
                pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, state);
        level.setBlock(pos, state.getFluidState().createLegacyBlock(), 3);
        level.addFreshEntity(entity);
        return entity;
    }

    @Override
    public void tick() {
        if (this.blockState.isAir()) {
            this.discard();
            return;
        }

        ++this.time;

        if (!this.isNoGravity()) {
            this.setDeltaMovement(this.getDeltaMovement().add(0.0D, -0.04D, 0.0D));
        }

        this.move(MoverType.SELF, this.getDeltaMovement());

        if (!this.level().isClientSide && this.onGround()) {
            BlockPos pos = this.blockPosition();
            AshPileBlock.landingMerge((ServerLevel) this.level(), pos, this.blockState);
            this.discard();
            return;
        }


        if (this.time > 600) {
            this.discard();
        }
    }
}
