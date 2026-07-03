package destiny.null_ouroboros.server.entity;

import destiny.null_ouroboros.common.dusterbike.DusterbikeEngineState;
import destiny.null_ouroboros.server.registry.ItemRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.PushReaction;

public class EngineHoistEntity extends Entity {
    private final DusterbikeEngineState engineState = new DusterbikeEngineState();
    private boolean hasEngine;

    public EngineHoistEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    public boolean isEmpty() {
        return !hasEngine;
    }

    public boolean hasEngine() {
        return hasEngine;
    }

    public DusterbikeEngineState getEngineState() {
        return engineState;
    }

    public void setEngineState(DusterbikeEngineState state) {
        engineState.load(state.save());
        hasEngine = true;
    }

    @Override
    protected void defineSynchedData() {

    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compoundTag) {
        hasEngine = compoundTag.getBoolean("HasEngine");
        if (compoundTag.contains("EngineState")) {
            engineState.load(compoundTag.getCompound("EngineState"));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compoundTag) {
        compoundTag.putBoolean("HasEngine", hasEngine);
        if (hasEngine) {
            compoundTag.put("EngineState", engineState.save());
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.isInvulnerableTo(source) || source.is(DamageTypeTags.IS_FALL)) {
            return false;
        }
        if (!level().isClientSide) {
            Block.popResource(level(), blockPosition(), new ItemStack(ItemRegistry.ENGINE_HOIST.get()));
            discard();
        }
        return true;
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(ItemRegistry.ENGINE_HOIST.get());
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public PushReaction getPistonPushReaction() {
        return PushReaction.BLOCK;
    }
}
