package destiny.null_ouroboros.server.entity.steel_leviathan;

import destiny.null_ouroboros.common.steel_leviathan.BurrowMissileColliders;
import destiny.null_ouroboros.server.entity.ParentLinkedHitboxEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

public class BurrowMissileDrillHitboxEntity extends ParentLinkedHitboxEntity {
    private static final EntityDataAccessor<Float> YAW =
            SynchedEntityData.defineId(BurrowMissileDrillHitboxEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> PITCH =
            SynchedEntityData.defineId(BurrowMissileDrillHitboxEntity.class, EntityDataSerializers.FLOAT);

    public BurrowMissileDrillHitboxEntity(EntityType<?> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineAdditionalSynchedData() {
        this.entityData.define(YAW, 0.0F);
        this.entityData.define(PITCH, 0.0F);
    }

    public void init(BurrowMissileEntity parent, double x, double y, double z) {
        initParentLink(parent.getId(), parent.getUUID(), x, y, z);
        syncOrientation(parent.getYRot(), parent.getXRot());
    }

    public void syncOrientation(float yaw, float pitch) {
        this.entityData.set(YAW, yaw);
        this.entityData.set(PITCH, pitch);
        setYRot(yaw);
        setXRot(pitch);
        refreshColliderBox();
    }

    public float getColliderYaw() {
        return this.entityData.get(YAW);
    }

    public float getColliderPitch() {
        return this.entityData.get(PITCH);
    }

    @Override
    protected Entity resolveParent() {
        return findParentOfType(BurrowMissileEntity.class);
    }

    @Override
    protected AABB makeBoundingBox() {
        return BurrowMissileColliders.drillColliderBoxAtCenter(position(), getColliderYaw(), getColliderPitch());
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            return;
        }
        BurrowMissileEntity parent = findParentOfType(BurrowMissileEntity.class);
        if (parent == null || parent.isRemoved()) {
            discard();
        }
    }

    @Override
    public boolean isPickable() {
        BurrowMissileEntity parent = findParentOfType(BurrowMissileEntity.class);
        return parent != null && parent.isPickable();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        BurrowMissileEntity parent = findParentOfType(BurrowMissileEntity.class);
        return parent != null && parent.hurt(source, amount);
    }

    @Override
    protected void readAdditionalHitboxData(CompoundTag tag) {
        this.entityData.set(YAW, tag.getFloat("Yaw"));
        this.entityData.set(PITCH, tag.getFloat("Pitch"));
    }

    @Override
    protected void writeAdditionalHitboxData(CompoundTag tag) {
        tag.putFloat("Yaw", getColliderYaw());
        tag.putFloat("Pitch", getColliderPitch());
    }
}
