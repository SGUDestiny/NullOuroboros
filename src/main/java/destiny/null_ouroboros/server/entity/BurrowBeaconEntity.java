package destiny.null_ouroboros.server.entity;

import destiny.null_ouroboros.client.network.ClientBoundBurrowBeaconSyncPacket;
import destiny.null_ouroboros.server.capability.ManifoldingCapability;
import destiny.null_ouroboros.server.registry.BlockRegistry;
import destiny.null_ouroboros.server.registry.DamageTypeRegistry;
import destiny.null_ouroboros.server.registry.ItemRegistry;
import destiny.null_ouroboros.server.registry.SoundRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.*;

public class BurrowBeaconEntity extends LivingEntity {
    public enum State {
        DEPLOY,
        LAND,
        DRILL,
        ACTIVE
    }

    private static final EntityDataAccessor<Integer> ANIMATION_STATE =
            SynchedEntityData.defineId(BurrowBeaconEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> ANIM_START_TIME =
            SynchedEntityData.defineId(BurrowBeaconEntity.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<CompoundTag> CONNECTIONS =
            SynchedEntityData.defineId(BurrowBeaconEntity.class, EntityDataSerializers.COMPOUND_TAG);
    public static final EntityDataAccessor<Float> PULSE_OFFSET =
            SynchedEntityData.defineId(BurrowBeaconEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DAMAGE_STATE =
            SynchedEntityData.defineId(BurrowBeaconEntity.class, EntityDataSerializers.FLOAT);

    private static final int LAND_DURATION = 10;
    private static final int DRILL_DURATION = 40;
    private static final float DAMAGE_PER_HIT = 0.2f;
    private static final float DRILL_DAMAGE = 4f;
    private static final int DRILL_DAMAGE_INTERVAL = 10;

    public long lastHit;
    private int drillDamageTimer = 0;

    public BurrowBeaconEntity(EntityType<? extends LivingEntity> type, Level level) {
        super(type, level);
        this.noPhysics = false;
        this.setNoGravity(false);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return LivingEntity.createLivingAttributes().add(Attributes.MAX_HEALTH, 1.0);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(ANIMATION_STATE, State.DEPLOY.ordinal());
        this.entityData.define(ANIM_START_TIME, 0);
        this.entityData.define(CONNECTIONS, new CompoundTag());
        this.entityData.define(PULSE_OFFSET, this.random.nextFloat());
        this.entityData.define(DAMAGE_STATE, 0f);
    }

    public State getAnimationState() {
        return State.values()[this.entityData.get(ANIMATION_STATE)];
    }

    public boolean isProvidingProtection() {
        return getAnimationState() == State.ACTIVE;
    }

    public int getAnimationStartTime() {
        return this.entityData.get(ANIM_START_TIME);
    }

    private void setAnimationState(State state) {
        this.entityData.set(ANIM_START_TIME, this.tickCount);
        this.entityData.set(ANIMATION_STATE, state.ordinal());
    }

    public Set<BlockPos> getConnectedPositions() {
        CompoundTag tag = this.entityData.get(CONNECTIONS);
        long[] packed = tag.getLongArray("list");
        Set<BlockPos> set = new HashSet<>();
        for (long l : packed) set.add(BlockPos.of(l));
        return set;
    }

    public void addConnection(BlockPos pos) {
        CompoundTag tag = this.entityData.get(CONNECTIONS).copy();
        long[] old = tag.getLongArray("list");
        long newVal = pos.asLong();
        for (long l : old) if (l == newVal) return;
        long[] newArr = Arrays.copyOf(old, old.length + 1);
        newArr[old.length] = newVal;
        tag.putLongArray("list", newArr);
        this.entityData.set(CONNECTIONS, tag);
    }

    public void removeConnection(BlockPos pos) {
        CompoundTag tag = this.entityData.get(CONNECTIONS).copy();
        long[] old = tag.getLongArray("list");
        List<Long> list = new ArrayList<>();
        long target = pos.asLong();
        for (long l : old) if (l != target) list.add(l);
        long[] newArr = new long[list.size()];
        for (int i = 0; i < newArr.length; i++) newArr[i] = list.get(i);
        tag.putLongArray("list", newArr);
        this.entityData.set(CONNECTIONS, tag);
    }

    public float getPulseOffset() {
        return this.entityData.get(PULSE_OFFSET);
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
                    setAnimationState(State.LAND);
                    this.setNoGravity(true);
                    this.playSound(SoundRegistry.BURROW_BEACON_LAND.get(), 1f, 1f);
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
                    setAnimationState(State.ACTIVE);

                    double sphereRadius = ManifoldingCapability.BEACON_PROTECTION_RANGE;
                    double sphereRadiusSq = sphereRadius * sphereRadius;

                    List<BurrowBeaconEntity> nearby = this.level().getEntitiesOfClass(BurrowBeaconEntity.class, this.getBoundingBox().inflate(sphereRadius));

                    for (BurrowBeaconEntity other : nearby) {
                        if (other == this) continue;

                        if (this.distanceToSqr(other) <= sphereRadiusSq) {
                            this.addConnection(other.blockPosition());
                            other.addConnection(this.blockPosition());

                            this.playSound(SoundRegistry.BURROW_BEACON_CONNECT.get(), 0.5f, 1f);

                            ClientBoundBurrowBeaconSyncPacket.send(this);
                            ClientBoundBurrowBeaconSyncPacket.send(other);
                        }
                    }
                } else {
                    drillDamageTimer++;

                    if (drillDamageTimer >= DRILL_DAMAGE_INTERVAL) {
                        drillDamageTimer = 0;
                        damageEntitiesDuringDrilling();
                    }

                    spawnDrillParticles();
                }
            }
        }
    }

    private void damageEntitiesDuringDrilling() {
        AABB box = this.getBoundingBox().inflate(0.2);
        List<LivingEntity> targets = this.level().getEntitiesOfClass(LivingEntity.class, box, entity -> !(entity instanceof BurrowBeaconEntity)
                        && !(entity instanceof Player player && (player.isCreative() || player.isSpectator())));

        for (LivingEntity target : targets) {
            target.hurt(DamageTypeRegistry.getSimpleDamageSource(this.level(), DamageTypeRegistry.BURROW_BEACON_DRILL), DRILL_DAMAGE);
        }
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(ItemRegistry.BURROW_BEACON.get());
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.isInvulnerableTo(source)) return false;
        if (source.is(DamageTypeTags.IS_FALL)) return false;

        if (source.getEntity() instanceof Player player && player.isCreative()) {
            brokenByPlayer(source, false);

            return true;
        }

        float currentDamage = getDamageState();

        currentDamage += DAMAGE_PER_HIT;
        if (currentDamage >= 1f) {
            currentDamage = 1f;
            this.entityData.set(DAMAGE_STATE, currentDamage);
            brokenByPlayer(source, true);

            this.kill();
        } else {
            this.entityData.set(DAMAGE_STATE, currentDamage);
            this.level().broadcastEntityEvent(this, (byte)32);
            this.lastHit = this.level().getGameTime();

            this.playSound(SoundRegistry.BURROW_BEACON_HIT.get(), 1f, 1f);
            this.gameEvent(GameEvent.ENTITY_DAMAGE, source.getEntity());
        }

        return true;
    }

    private void brokenByPlayer(DamageSource source, boolean shouldDrop) {
        ItemStack item = new ItemStack(ItemRegistry.BURROW_BEACON.get());

        if (this.hasCustomName()) {
            item.setHoverName(this.getCustomName());
        }

        spawnBreakParticles();

        if (shouldDrop) {
            Block.popResource(this.level(), this.blockPosition(), item);
        }

        this.playSound(SoundRegistry.BURROW_BEACON_BREAK.get(), 1f, 1f);

        this.gameEvent(GameEvent.ENTITY_DIE, source.getEntity());
        this.remove(RemovalReason.KILLED);
    }

    private void spawnBreakParticles() {
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, BlockRegistry.BLACKMETAL.get().defaultBlockState()),
                    this.getX(), this.getY(0.6), this.getZ(), 20, this.getBbWidth() / 2f,
                    this.getBbHeight() / 2f, this.getBbWidth() / 2f, 0.05);
        }
    }

    private void spawnDrillParticles() {
        Level level = this.level();
        BlockPos onPos = this.getOnPos();
        Vec3 particleVec = onPos.above().getCenter();

        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, serverLevel.getBlockState(onPos)),
                    particleVec.x, particleVec.y - 0.25f, particleVec.z, 10, this.getBbWidth() / 4f,
                    this.getBbHeight() / 4f, this.getBbWidth() / 4f, 0.05);
        }
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 32) {
            this.lastHit = this.level().getGameTime();
        } else {
            super.handleEntityEvent(id);
        }
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!this.level().isClientSide) {
            for (BlockPos pos : getConnectedPositions()) {
                List<BurrowBeaconEntity> list = this.level().getEntitiesOfClass(BurrowBeaconEntity.class, new AABB(pos).inflate(0.5));

                for (BurrowBeaconEntity other : list) {
                    if (other != this) {
                        other.playSound(SoundRegistry.BURROW_BEACON_DISCONNECT.get(), 0.5f, 1f);

                        other.removeConnection(this.blockPosition());
                        ClientBoundBurrowBeaconSyncPacket.send(other);
                    }
                }
            }
        }
        super.remove(reason);
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

    public float getDamageState() {
        return this.entityData.get(DAMAGE_STATE);
    }

    @Override public boolean isPushable() {
        return false;

    }

    @Override public boolean isPickable() {
        return true;

    }

    @Override public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public PushReaction getPistonPushReaction() {
        return PushReaction.BLOCK;
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    protected void doPush(Entity entity) {
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.entityData.set(ANIMATION_STATE, tag.getInt("AnimationState"));
        this.entityData.set(ANIM_START_TIME, tag.getInt("AnimationStartTime"));
        this.entityData.set(CONNECTIONS, tag.getCompound("Connections"));
        this.entityData.set(PULSE_OFFSET, tag.getFloat("PulseOffset"));
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("AnimationState", getAnimationState().ordinal());
        tag.putInt("AnimationStartTime", getAnimationStartTime());
        tag.put("Connections", this.entityData.get(CONNECTIONS));
        tag.putFloat("PulseOffset", this.entityData.get(PULSE_OFFSET));
    }
}