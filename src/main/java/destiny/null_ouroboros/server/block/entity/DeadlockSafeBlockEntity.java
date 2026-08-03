package destiny.null_ouroboros.server.block.entity;

import destiny.null_ouroboros.server.fuse.FusePowerTracker;
import destiny.null_ouroboros.server.registry.BlockEntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;

public class DeadlockSafeBlockEntity extends SafeBlockEntity {
    private boolean indicatorOn;
    private boolean clientIndicatorOn;

    public DeadlockSafeBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.DEADLOCK_SAFE_BLOCK_ENTITY.get(), pos, state, true);
    }

    @Override
    public boolean canSpinBothWays() {
        if (level != null && level.isClientSide) {
            return clientIndicatorOn;
        }
        return level != null && FusePowerTracker.isPowered(level, worldPosition);
    }

    public boolean isIndicatorOn() {
        if (level != null && level.isClientSide) {
            return clientIndicatorOn;
        }
        return indicatorOn;
    }

    @Override
    protected void serverTick() {
        super.serverTick();
        boolean powered = level != null && FusePowerTracker.isPowered(level, worldPosition);
        if (powered != indicatorOn) {
            indicatorOn = powered;
            setChangedAndSync();
        }
    }

    public void refreshPowerState() {
        if (level == null || level.isClientSide) {
            return;
        }
        boolean powered = FusePowerTracker.isPowered(level, worldPosition);
        if (powered != indicatorOn) {
            indicatorOn = powered;
            setChangedAndSync();
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.null_ouroboros.deadlock_safe");
    }

    @Override
    protected void saveSafeExtra(CompoundTag tag) {
        tag.putBoolean("IndicatorOn", indicatorOn);
    }

    @Override
    protected void loadSafeExtra(CompoundTag tag) {
        indicatorOn = tag.getBoolean("IndicatorOn");
    }

    @Override
    protected void writeClientExtra(CompoundTag tag) {
        tag.putBoolean("WheelUnlocked", indicatorOn);
        tag.putBoolean("IndicatorOn", indicatorOn);
    }

    @Override
    protected void readClientExtra(CompoundTag tag) {
        clientIndicatorOn = tag.getBoolean("IndicatorOn");
        clientWheelUnlocked = clientIndicatorOn;
    }
}
