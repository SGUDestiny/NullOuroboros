package destiny.null_ouroboros.server.entity;

import destiny.null_ouroboros.common.DusterbikeTransforms;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;
import java.util.UUID;

public class DusterbikeKeyEntity extends Entity {
    private static final EntityDataAccessor<Integer> PARENT_ID =
            SynchedEntityData.defineId(DusterbikeKeyEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Optional<UUID>> PARENT_UUID =
            SynchedEntityData.defineId(DusterbikeKeyEntity.class, EntityDataSerializers.OPTIONAL_UUID);

    private static final int NO_PARENT = -1;
    private static final int MISSING_PARENT_GRACE_TICKS = 120;

    private int missingParentTicks;

    public DusterbikeKeyEntity(EntityType<? extends DusterbikeKeyEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    public DusterbikeKeyEntity(EntityType<? extends DusterbikeKeyEntity> type, Level level, int parentId, UUID parentUuid, double x, double y, double z) {
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

    public int getParentId() {
        return this.entityData.get(PARENT_ID);
    }

    public void setParentId(int parentId) {
        this.entityData.set(PARENT_ID, parentId);
    }

    public Optional<UUID> getParentUuid() {
        return this.entityData.get(PARENT_UUID);
    }

    public void setParentUuid(UUID parentUuid) {
        this.entityData.set(PARENT_UUID, Optional.ofNullable(parentUuid));
    }

    public DusterbikeEntity getParent() {
        if (getParentId() != NO_PARENT) {
            Entity entity = this.level().getEntity(getParentId());
            if (entity instanceof DusterbikeEntity bike) {
                return bike;
            }
        }
        return null;
    }

    @Override
    protected AABB makeBoundingBox() {
        DusterbikeEntity parent = getParent();
        float yaw = parent != null ? parent.getYRot() : 0.0F;
        return DusterbikeTransforms.keyColliderBox(getX(), getY(), getZ(), yaw);
    }

    public void syncColliderPosition(double centerX, double centerY, double centerZ) {
        setPos(centerX, centerY, centerZ);
        refreshColliderBox();
    }

    public void refreshColliderBox() {
        setBoundingBox(makeBoundingBox());
    }

    @Override
    public void lerpTo(double x, double y, double z, float yaw, float pitch, int posRotationIncrements, boolean teleport) {
        DusterbikeEntity parent = getParent();
        if (parent != null && parent.isControlledByLocalInstance()) {
            return;
        }
        super.lerpTo(x, y, z, yaw, pitch, posRotationIncrements, teleport);
        refreshColliderBox();
    }

    @Override
    public void tick() {
        if (this.level().isClientSide) {
            super.tick();
            refreshColliderBox();
            return;
        }

        super.tick();

        if (findParent() != null) {
            missingParentTicks = 0;
            return;
        }

        missingParentTicks++;
        if (missingParentTicks > MISSING_PARENT_GRACE_TICKS) {
            this.discard();
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!this.level().isClientSide && reason.shouldDestroy() && reason != RemovalReason.DISCARDED) {
            notifyParentRemoved();
        }
        super.remove(reason);
    }

    private void notifyParentRemoved() {
        DusterbikeEntity parent = findParent();
        if (parent != null) {
            parent.onKeyRemoved(this);
        }
    }

    public DusterbikeEntity findParent() {
        int parentId = getParentId();
        if (parentId != NO_PARENT) {
            Entity entity = this.level().getEntity(parentId);
            if (entity instanceof DusterbikeEntity bike) {
                return bike;
            }
        }

        Optional<UUID> parentUuid = getParentUuid();
        if (parentUuid.isEmpty()) {
            return null;
        }

        for (DusterbikeEntity bike : this.level().getEntitiesOfClass(
                DusterbikeEntity.class, this.getBoundingBox().inflate(16.0D))) {
            if (bike.getUUID().equals(parentUuid.get())) {
                setParentId(bike.getId());
                return bike;
            }
        }

        return null;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.contains("Parent")) {
            setParentId(tag.getInt("Parent"));
        }
        if (tag.hasUUID("ParentUuid")) {
            setParentUuid(tag.getUUID("ParentUuid"));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (getParentId() != NO_PARENT) {
            tag.putInt("Parent", getParentId());
        }
        getParentUuid().ifPresent(uuid -> tag.putUUID("ParentUuid", uuid));
        tag.putUUID("KeyUuid", this.getUUID());
    }

    @Override
    public Entity.MovementEmission getMovementEmission() {
        return Entity.MovementEmission.NONE;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.isInvulnerableTo(source) || source.is(DamageTypeTags.IS_FALL)) {
            return false;
        }

        if (!this.level().isClientSide) {
            DusterbikeEntity parent = findParent();
            if (parent != null) {
                parent.discardWithWheels();
            } else {
                this.discard();
            }
        }

        return true;
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }
}
