package destiny.null_ouroboros.server.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;
import java.util.UUID;

public class EngineKeyEntity extends Entity {
    private static final EntityDataAccessor<Integer> PARENT_ID =
            SynchedEntityData.defineId(EngineKeyEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Optional<UUID>> PARENT_UUID =
            SynchedEntityData.defineId(EngineKeyEntity.class, EntityDataSerializers.OPTIONAL_UUID);

    private static final int NO_PARENT = -1;
    private static final int MISSING_PARENT_GRACE_TICKS = 120;
    private int missingParentTicks;

    public EngineKeyEntity(EntityType<?> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
    }

    public EngineKeyEntity(EntityType<?> type, Level level, int parentId, UUID parentUuid, double x, double y, double z) {
        this(type, level);
        setParentId(parentId);
        setParentUuid(parentUuid);
        setPos(x, y, z);
        setDeltaMovement(Vec3.ZERO);
        refreshColliderBox();
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(PARENT_ID, NO_PARENT);
        this.entityData.define(PARENT_UUID, Optional.empty());
    }

    public int getParentId() { return this.entityData.get(PARENT_ID); }
    public void setParentId(int parentId) { this.entityData.set(PARENT_ID, parentId); }
    public Optional<UUID> getParentUuid() { return this.entityData.get(PARENT_UUID); }
    public void setParentUuid(UUID parentUuid) { this.entityData.set(PARENT_UUID, Optional.ofNullable(parentUuid)); }

    public EngineEntity getParent() {
        if (getParentId() != NO_PARENT) {
            Entity entity = this.level().getEntity(getParentId());
            if (entity instanceof EngineEntity engine) return engine;
        }
        return null;
    }

    @Override
    protected AABB makeBoundingBox() {
        double hs = 0.05;
        return new AABB(getX() - hs, getY() - hs * 2, getZ() - hs,
                getX() + hs, getY() + hs * 2, getZ() + hs);
    }

    public void syncColliderPosition(double x, double y, double z) {
        setPos(x, y, z);
        refreshColliderBox();
    }

    public void refreshColliderBox() {
        setBoundingBox(makeBoundingBox());
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        EngineEntity engine = getParent();
        if (engine != null) {
            engine.handleKeyPortInteraction(player, hand);
            return InteractionResult.PASS;
        }
        return InteractionResult.PASS;
    }

    @Override
    public void tick() {
        super.tick();
        refreshColliderBox();
        if (this.level().isClientSide) return;

        if (findParent() != null) {
            missingParentTicks = 0;
            return;
        }
        missingParentTicks++;
        if (missingParentTicks > MISSING_PARENT_GRACE_TICKS) {
            this.discard();
        }
    }

    private EngineEntity findParent() {
        int parentId = getParentId();
        if (parentId != NO_PARENT) {
            Entity entity = this.level().getEntity(parentId);
            if (entity instanceof EngineEntity engine) return engine;
        }
        Optional<UUID> parentUuid = getParentUuid();
        if (parentUuid.isEmpty()) return null;
        for (EngineEntity engine : this.level().getEntitiesOfClass(EngineEntity.class, this.getBoundingBox().inflate(16.0D))) {
            if (engine.getUUID().equals(parentUuid.get())) {
                setParentId(engine.getId());
                return engine;
            }
        }
        return null;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.contains("Parent")) setParentId(tag.getInt("Parent"));
        if (tag.hasUUID("ParentUuid")) setParentUuid(tag.getUUID("ParentUuid"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (getParentId() != NO_PARENT) tag.putInt("Parent", getParentId());
        getParentUuid().ifPresent(uuid -> tag.putUUID("ParentUuid", uuid));
    }

    @Override public boolean hurt(DamageSource source, float amount) { return false; }
    @Override public boolean isPickable() { return true; }
    @Override public boolean isPushable() { return false; }
    @Override public boolean canBeCollidedWith() { return false; }
}