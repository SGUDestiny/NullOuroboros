package destiny.null_ouroboros.server.entity;

import destiny.null_ouroboros.common.DusterbikeTransforms;
import destiny.null_ouroboros.common.dusterbike.DusterbikePartTargetType;
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

public class DusterbikePartInteractionEntity extends Entity {
    private static final EntityDataAccessor<Integer> PARENT_ID =
            SynchedEntityData.defineId(DusterbikePartInteractionEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Optional<UUID>> PARENT_UUID =
            SynchedEntityData.defineId(DusterbikePartInteractionEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Integer> TARGET_TYPE =
            SynchedEntityData.defineId(DusterbikePartInteractionEntity.class, EntityDataSerializers.INT);

    private static final int NO_PARENT = -1;
    private static final int MISSING_PARENT_GRACE_TICKS = 120;

    private int missingParentTicks;

    public DusterbikePartInteractionEntity(EntityType<? extends DusterbikePartInteractionEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    public DusterbikePartInteractionEntity(
            EntityType<? extends DusterbikePartInteractionEntity> type,
            Level level,
            int parentId,
            UUID parentUuid,
            DusterbikePartTargetType targetType,
            double x,
            double y,
            double z) {
        this(type, level);
        setParentId(parentId);
        setParentUuid(parentUuid);
        setTargetType(targetType);
        setPos(x, y, z);
        setDeltaMovement(Vec3.ZERO);
        refreshColliderBox();
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(PARENT_ID, NO_PARENT);
        this.entityData.define(PARENT_UUID, Optional.empty());
        this.entityData.define(TARGET_TYPE, DusterbikePartTargetType.FRONT_LIGHT.id());
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

    public DusterbikePartTargetType getTargetType() {
        return DusterbikePartTargetType.byId(this.entityData.get(TARGET_TYPE));
    }

    public void setTargetType(DusterbikePartTargetType targetType) {
        this.entityData.set(TARGET_TYPE, targetType.id());
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
        return DusterbikeTransforms.partTargetColliderBox(getX(), getY(), getZ(), yaw, getTargetType());
    }

    public void syncColliderPosition(double centerX, double centerY, double centerZ) {
        setPos(centerX, centerY, centerZ);
        refreshColliderBox();
    }

    public void refreshColliderBox() {
        setBoundingBox(makeBoundingBox());
    }

    @Override
    public void tick() {
        super.tick();
        refreshColliderBox();
        if (this.level().isClientSide) {
            return;
        }
        if (findParent() != null) {
            missingParentTicks = 0;
            return;
        }
        missingParentTicks++;
        if (missingParentTicks > MISSING_PARENT_GRACE_TICKS) {
            this.discard();
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
        if (tag.contains("TargetType")) {
            setTargetType(DusterbikePartTargetType.byId(tag.getInt("TargetType")));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (getParentId() != NO_PARENT) {
            tag.putInt("Parent", getParentId());
        }
        getParentUuid().ifPresent(uuid -> tag.putUUID("ParentUuid", uuid));
        tag.putInt("TargetType", getTargetType().id());
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.isInvulnerableTo(source) || source.is(DamageTypeTags.IS_FALL)) {
            return false;
        }
        DusterbikeEntity parent = findParent();
        return parent != null && parent.hurt(source, amount);
    }

    @Override
    public Entity.MovementEmission getMovementEmission() {
        return Entity.MovementEmission.NONE;
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
