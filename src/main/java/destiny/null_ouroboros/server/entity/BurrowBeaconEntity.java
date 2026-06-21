package destiny.null_ouroboros.server.entity;

import destiny.null_ouroboros.server.registry.SoundRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Collections;

public class BurrowBeaconEntity extends LivingEntity {
    public enum State {
        DEPLOY,
        LAND,
        DRILL,
        DRILL_IDLE
    }

    private static final String ANIMATION_STATE_TAG = "AnimationState";
    private static final String ANIMATION_START_TIME_TAG = "AnimationStartTime";

    private static final EntityDataAccessor<Integer> ANIMATION_STATE = SynchedEntityData.defineId(BurrowBeaconEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> ANIM_START_TIME = SynchedEntityData.defineId(BurrowBeaconEntity.class, EntityDataSerializers.INT);

    private static final int LAND_DURATION = 10;
    private static final int DRILL_DURATION = 40;

    private int groundTicks = 0;

    public BurrowBeaconEntity(EntityType<? extends LivingEntity> type, Level level) {
        super(type, level);
        this.noPhysics = false;
        this.setNoGravity(false);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return LivingEntity.createLivingAttributes().add(Attributes.MAX_HEALTH, 1);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(ANIMATION_STATE, State.DEPLOY.ordinal());
        this.entityData.define(ANIM_START_TIME, 0);
    }

    public State getAnimationState() {
        return State.values()[this.entityData.get(ANIMATION_STATE)];
    }

    public int getAnimationStartTime() {
        return this.entityData.get(ANIM_START_TIME);
    }

    private void setAnimationState(State state) {
        this.entityData.set(ANIM_START_TIME, this.tickCount);
        this.entityData.set(ANIMATION_STATE, state.ordinal());
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide) return;

        State state = getAnimationState();
        int elapsed = this.tickCount - getAnimationStartTime();

        switch (state) {
            case DEPLOY -> {
                if (elapsed == 1) {
                    this.playSound(SoundRegistry.BURROW_BEACON_DEPLOY.get(), 1f, 1f);
                }

                if (this.onGround()) {
                    groundTicks++;
                    if (groundTicks >= 5) {
                        setAnimationState(State.LAND);
                        this.setNoGravity(true);
                        this.playSound(SoundRegistry.BURROW_BEACON_LAND.get(), 1f, 1f);
                        groundTicks = 0;
                    }
                } else {
                    groundTicks = 0;
                }
            }
            case LAND -> {
                if (elapsed >= LAND_DURATION) {
                    setAnimationState(State.DRILL);
                    this.playSound(SoundRegistry.BURROW_BEACON_DRILL.get(), 1f, 1f);
                }
            }
            case DRILL -> {
                if (elapsed >= DRILL_DURATION) {
                    setAnimationState(State.DRILL_IDLE);
                }
            }
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.isInvulnerableTo(source)) return false;

        if (source.is(DamageTypeTags.IS_FALL)) return false;

        this.remove(RemovalReason.KILLED);
        return true;
    }

    @Override
    public boolean isColliding(BlockPos pos, BlockState state) {
        return false;
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public Iterable<ItemStack> getArmorSlots() {
        return Collections.emptyList();
    }

    @Override public ItemStack getItemBySlot(EquipmentSlot slot) {
        return ItemStack.EMPTY;
    }

    @Override public void setItemSlot(EquipmentSlot slot, ItemStack stack) {
    }

    @Override public HumanoidArm getMainArm() {
        return HumanoidArm.RIGHT;
    }

    @Override public EntityDimensions getDimensions(Pose pose) {
        return new EntityDimensions(4f, 2f, false);
    }

    @Override public boolean isPushable() {
        return false;
    }

    @Override public boolean isPickable() {
        return true;
    }

    @Override public boolean canBeCollidedWith() {
        return true;
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.entityData.set(ANIMATION_STATE, tag.getInt(ANIMATION_STATE_TAG));
        this.entityData.set(ANIM_START_TIME, tag.getInt(ANIMATION_START_TIME_TAG));
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt(ANIMATION_STATE_TAG, getAnimationState().ordinal());
        tag.putInt(ANIMATION_START_TIME_TAG, getAnimationStartTime());
    }
}