package destiny.null_ouroboros.server.block.entity;

import destiny.null_ouroboros.client.sound.OutputVentClientSoundHandler;
import destiny.null_ouroboros.client.vent.VentParticleFx;
import destiny.null_ouroboros.server.block.OutputVentBlock;
import destiny.null_ouroboros.server.block.OutputVentMode;
import destiny.null_ouroboros.server.item.FilterItem;
import destiny.null_ouroboros.server.item.RespiratorGear;
import destiny.null_ouroboros.server.registry.BlockEntityRegistry;
import destiny.null_ouroboros.server.vent.VentNetworkTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

public class OutputVentBlockEntity extends BlockEntity {
    private static final float MAX_SPEED = 1.0F;
    private static final float ACCELERATION = MAX_SPEED / 40.0F;
    private static final int FILTER_CELLS_PER_HURT = 5;

    private ItemStack filter = ItemStack.EMPTY;
    private boolean wasRedstone;
    private boolean visuallyActive;
    private boolean atmosphereActive;
    private boolean emittingClean;
    private float runSpeed;
    private int filterHurtTicker;

    public OutputVentBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.OUTPUT_VENT_BLOCK_ENTITY.get(), pos, state);
    }

    public boolean wasRedstone() {
        return wasRedstone;
    }

    public void setWasRedstone(boolean wasRedstone) {
        this.wasRedstone = wasRedstone;
    }

    public ItemStack getFilter() {
        return filter;
    }

    public boolean hasWorkingFilter() {
        return !filter.isEmpty()
                && filter.getItem() instanceof FilterItem
                && filter.getDamageValue() < RespiratorGear.FILTER_MAX_DAMAGE;
    }

    public boolean insertFilter(ItemStack stack) {
        if (!filter.isEmpty() || !(stack.getItem() instanceof FilterItem)) {
            return false;
        }
        filter = stack.copyWithCount(1);
        filterHurtTicker = 0;
        setChanged();
        sync();
        return true;
    }

    public ItemStack removeFilter() {
        if (filter.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack out = filter;
        filter = ItemStack.EMPTY;
        filterHurtTicker = 0;
        setChanged();
        sync();
        return out;
    }

    public void dropFilter(Level level, BlockPos pos) {
        if (!filter.isEmpty()) {
            Block.popResource(level, pos, filter);
            filter = ItemStack.EMPTY;
        }
    }

    public void setVisuallyActive(boolean visuallyActive) {
        if (this.visuallyActive != visuallyActive) {
            this.visuallyActive = visuallyActive;
            setChanged();
            sync();
        }
    }

    public void setAtmosphereActive(boolean atmosphereActive) {
        this.atmosphereActive = atmosphereActive;
    }

    public boolean isAtmosphereActive() {
        return atmosphereActive;
    }

    public void setEmittingClean(boolean emittingClean) {
        if (this.emittingClean != emittingClean) {
            this.emittingClean = emittingClean;
            setChanged();
            sync();
        }
    }

    public boolean isVisuallyActive() {
        return visuallyActive;
    }

    public boolean isEmittingClean() {
        return emittingClean;
    }

    public float getRunSpeed() {
        return runSpeed;
    }

    public static float getMaxSpeed() {
        return MAX_SPEED;
    }

    public void hurtFilterIfNeeded() {
        if (!hasWorkingFilter() || level == null || level.isClientSide) {
            return;
        }
        if (!visuallyActive) {
            return;
        }
        filterHurtTicker++;
        if (filterHurtTicker < FILTER_CELLS_PER_HURT) {
            return;
        }
        filterHurtTicker = 0;
        filter.setDamageValue(Math.min(RespiratorGear.FILTER_MAX_DAMAGE, filter.getDamageValue() + 1));
        setChanged();
        BlockState state = getBlockState();
        OutputVentBlock.updateMode(level, worldPosition, state, this);
        sync();
    }

    public static void tick(Level level, BlockPos pos, BlockState state, OutputVentBlockEntity be) {
        boolean wantRun = state.getValue(OutputVentBlock.POWERED) && be.visuallyActive;
        float target = wantRun ? MAX_SPEED : 0.0F;
        if (be.runSpeed < target) {
            be.runSpeed = Math.min(be.runSpeed + ACCELERATION, target);
        } else if (be.runSpeed > target) {
            be.runSpeed = Math.max(be.runSpeed - ACCELERATION, target);
        }

        if (!level.isClientSide) {
            OutputVentMode mode = OutputVentMode.OFF;
            if (state.getValue(OutputVentBlock.POWERED)) {
                mode = be.hasWorkingFilter() ? OutputVentMode.ON : OutputVentMode.ON_BROKEN;
            }
            if (state.getValue(OutputVentBlock.MODE) != mode) {
                level.setBlock(pos, state.setValue(OutputVentBlock.MODE, mode), Block.UPDATE_ALL);
            }
        }

        if (level.isClientSide) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                OutputVentClientSoundHandler.tick(be);
                if (be.runSpeed > 0.05F && be.visuallyActive) {
                    VentParticleFx.spawnOutletWhirlwind(level, pos, be.runSpeed, be.emittingClean);
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
                    OutputVentClientSoundHandler.stop(this));
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putBoolean("WasRedstone", wasRedstone);
        tag.putBoolean("VisuallyActive", visuallyActive);
        tag.putBoolean("AtmosphereActive", atmosphereActive);
        tag.putBoolean("EmittingClean", emittingClean);
        tag.putFloat("RunSpeed", runSpeed);
        tag.putInt("FilterHurtTicker", filterHurtTicker);
        if (!filter.isEmpty()) {
            tag.put("Filter", filter.save(new CompoundTag()));
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        wasRedstone = tag.getBoolean("WasRedstone");
        visuallyActive = tag.getBoolean("VisuallyActive");
        atmosphereActive = tag.getBoolean("AtmosphereActive");
        emittingClean = tag.getBoolean("EmittingClean");
        runSpeed = tag.getFloat("RunSpeed");
        filterHurtTicker = tag.getInt("FilterHurtTicker");
        if (tag.contains("Filter")) {
            filter = ItemStack.of(tag.getCompound("Filter"));
        } else {
            filter = ItemStack.EMPTY;
        }
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
