package destiny.null_ouroboros.server.block.entity;

import destiny.null_ouroboros.server.block.IntakeFanBlock;
import destiny.null_ouroboros.server.registry.BlockEntityRegistry;
import destiny.null_ouroboros.server.vent.VentNetworkTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

public class IntakeFanBlockEntity extends BlockEntity {
    private static final float MAX_SPEED = 1.0F;
    private static final float ACCELERATION = MAX_SPEED / 40.0F;

    private boolean wasRedstone;
    private boolean operational;
    private float runSpeed;

    public IntakeFanBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.INTAKE_FAN_BLOCK_ENTITY.get(), pos, state);
    }

    public boolean wasRedstone() {
        return wasRedstone;
    }

    public void setWasRedstone(boolean wasRedstone) {
        this.wasRedstone = wasRedstone;
    }

    public void setOperational(boolean operational) {
        if (this.operational != operational) {
            this.operational = operational;
            setChanged();
            sync();
        }
    }

    public boolean isOperational() {
        return operational;
    }

    public float getRunSpeed() {
        return runSpeed;
    }

    public static float getMaxSpeed() {
        return MAX_SPEED;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, IntakeFanBlockEntity be) {
        boolean wantRun = state.getValue(IntakeFanBlock.POWERED) && (be.operational || level.isClientSide && be.runSpeed > 0);
        if (level.isClientSide) {
            wantRun = state.getValue(IntakeFanBlock.POWERED) && be.operational;
        } else {
            wantRun = state.getValue(IntakeFanBlock.POWERED) && be.operational;
        }

        float target = wantRun ? MAX_SPEED : 0.0F;
        if (be.runSpeed < target) {
            be.runSpeed = Math.min(be.runSpeed + ACCELERATION, target);
        } else if (be.runSpeed > target) {
            be.runSpeed = Math.max(be.runSpeed - ACCELERATION, target);
        }

        if (level.isClientSide) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                destiny.null_ouroboros.client.sound.IntakeFanClientSoundHandler.tick(be);
                if (be.runSpeed > 0.05F && state.getValue(IntakeFanBlock.POWERED) && be.operational) {
                    destiny.null_ouroboros.client.vent.VentParticleFx.spawnIntakeWhirlwind(level, pos, be.runSpeed);
                }
            });
        }
    }

    private void sync() {
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level != null && level.isClientSide) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                    destiny.null_ouroboros.client.sound.IntakeFanClientSoundHandler.stop(this));
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putBoolean("WasRedstone", wasRedstone);
        tag.putBoolean("Operational", operational);
        tag.putFloat("RunSpeed", runSpeed);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        wasRedstone = tag.getBoolean("WasRedstone");
        operational = tag.getBoolean("Operational");
        runSpeed = tag.getFloat("RunSpeed");
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) {
            VentNetworkTracker.addDuct(level, worldPosition);
        }
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        load(tag);
    }

    @Override
    public void onDataPacket(net.minecraft.network.Connection net, ClientboundBlockEntityDataPacket pkt) {
        CompoundTag tag = pkt.getTag();
        if (tag != null) {
            load(tag);
        }
    }
}
