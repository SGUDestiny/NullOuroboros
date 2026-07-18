package destiny.null_ouroboros.server.entity;

import destiny.null_ouroboros.common.dusterbike.DusterbikeTransforms;
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

import java.util.UUID;

public class DusterbikePartInteractionEntity extends ParentLinkedHitboxEntity {
    private static final EntityDataAccessor<Integer> TARGET_TYPE =
            SynchedEntityData.defineId(DusterbikePartInteractionEntity.class, EntityDataSerializers.INT);

    public DusterbikePartInteractionEntity(EntityType<? extends DusterbikePartInteractionEntity> type, Level level) {
        super(type, level);
    }

    public DusterbikePartInteractionEntity(EntityType<? extends DusterbikePartInteractionEntity> type, Level level, int parentId, UUID parentUuid,
            DusterbikePartTargetType targetType, double x, double y, double z) {
        this(type, level);
        setTargetType(targetType);
        initParentLink(parentId, parentUuid, x, y, z);
    }

    @Override
    protected void defineAdditionalSynchedData() {
        this.entityData.define(TARGET_TYPE, DusterbikePartTargetType.FRONT_LIGHT.id());
    }

    public DusterbikePartTargetType getTargetType() {
        return DusterbikePartTargetType.byId(this.entityData.get(TARGET_TYPE));
    }

    public void setTargetType(DusterbikePartTargetType targetType) {
        this.entityData.set(TARGET_TYPE, targetType.id());
    }

    public DusterbikeEntity getParent() {
        Entity entity = getParentId() != NO_PARENT ? this.level().getEntity(getParentId()) : null;
        return entity instanceof DusterbikeEntity bike ? bike : null;
    }

    public DusterbikeEntity findParent() {
        return findParentOfType(DusterbikeEntity.class);
    }

    @Override
    protected Entity resolveParent() {
        return findParent();
    }

    @Override
    protected AABB makeBoundingBox() {
        DusterbikeEntity parent = getParent();
        float yaw = parent != null ? parent.getYRot() : 0.0F;
        return DusterbikeTransforms.partTargetColliderBox(getX(), getY(), getZ(), yaw, getTargetType());
    }

    @Override
    protected void readAdditionalHitboxData(CompoundTag tag) {
        if (tag.contains("TargetType")) {
            setTargetType(DusterbikePartTargetType.byId(tag.getInt("TargetType")));
        }
    }

    @Override
    protected void writeAdditionalHitboxData(CompoundTag tag) {
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
}
