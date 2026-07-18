package destiny.null_ouroboros.server.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;
import java.util.UUID;

public abstract class ParentLinkedHitboxEntity extends Entity {
    protected static final int NO_PARENT = -1;
    protected static final int MISSING_PARENT_GRACE_TICKS = 120;

    private static final EntityDataAccessor<Integer> PARENT_ID =
            SynchedEntityData.defineId(ParentLinkedHitboxEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Optional<UUID>> PARENT_UUID =
            SynchedEntityData.defineId(ParentLinkedHitboxEntity.class, EntityDataSerializers.OPTIONAL_UUID);

    private int missingParentTicks;

    protected ParentLinkedHitboxEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(PARENT_ID, NO_PARENT);
        this.entityData.define(PARENT_UUID, Optional.empty());
        defineAdditionalSynchedData();
    }

    protected void defineAdditionalSynchedData() {}

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

    public void syncColliderPosition(double centerX, double centerY, double centerZ) {
        setPos(centerX, centerY, centerZ);
        refreshColliderBox();
    }

    public void refreshColliderBox() {
        setBoundingBox(makeBoundingBox());
    }

    protected void initParentLink(int parentId, UUID parentUuid, double x, double y, double z) {
        setParentId(parentId);
        setParentUuid(parentUuid);
        setPos(x, y, z);
        setDeltaMovement(Vec3.ZERO);
        refreshColliderBox();
    }

    @Override
    public void tick() {
        super.tick();
        refreshColliderBox();
        if (this.level().isClientSide) {
            return;
        }
        if (resolveParent() != null) {
            missingParentTicks = 0;
            return;
        }
        missingParentTicks++;
        if (missingParentTicks > MISSING_PARENT_GRACE_TICKS) {
            this.discard();
        }
    }

    protected abstract Entity resolveParent();

    protected <T extends Entity> T findParentOfType(Class<T> type) {
        int parentId = getParentId();
        if (parentId != NO_PARENT) {
            Entity entity = this.level().getEntity(parentId);
            if (type.isInstance(entity)) {
                return type.cast(entity);
            }
        }

        Optional<UUID> parentUuid = getParentUuid();
        if (parentUuid.isEmpty()) {
            return null;
        }

        for (T candidate : this.level().getEntitiesOfClass(type, this.getBoundingBox().inflate(16.0D))) {
            if (candidate.getUUID().equals(parentUuid.get())) {
                setParentId(candidate.getId());
                return candidate;
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
        readAdditionalHitboxData(tag);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (getParentId() != NO_PARENT) {
            tag.putInt("Parent", getParentId());
        }
        getParentUuid().ifPresent(uuid -> tag.putUUID("ParentUuid", uuid));
        writeAdditionalHitboxData(tag);
    }

    protected void readAdditionalHitboxData(CompoundTag tag) {}

    protected void writeAdditionalHitboxData(CompoundTag tag) {}

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
