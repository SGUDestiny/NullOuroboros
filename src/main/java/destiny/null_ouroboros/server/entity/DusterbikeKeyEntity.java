package destiny.null_ouroboros.server.entity;

import destiny.null_ouroboros.common.dusterbike.DusterbikeTransforms;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.UUID;

public class DusterbikeKeyEntity extends ParentLinkedHitboxEntity {
    public DusterbikeKeyEntity(EntityType<? extends DusterbikeKeyEntity> type, Level level) {
        super(type, level);
    }

    public DusterbikeKeyEntity(EntityType<? extends DusterbikeKeyEntity> type, Level level, int parentId, UUID parentUuid, double x, double y, double z) {
        this(type, level);
        initParentLink(parentId, parentUuid, x, y, z);
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
        return DusterbikeTransforms.keyColliderBox(getX(), getY(), getZ(), yaw);
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
            return;
        }
        super.tick();
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!this.level().isClientSide && reason.shouldDestroy() && reason != RemovalReason.DISCARDED) {
            DusterbikeEntity parent = findParent();
            if (parent != null) {
                parent.onKeyRemoved();
            }
        }
        super.remove(reason);
    }

    @Override
    protected void writeAdditionalHitboxData(CompoundTag tag) {
        tag.putUUID("KeyUuid", this.getUUID());
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.isInvulnerableTo(source) || source.is(DamageTypeTags.IS_FALL)) {
            return false;
        }

        if (!this.level().isClientSide) {
            DusterbikeEntity parent = findParent();
            if (parent != null) {
                parent.hurt(source, amount);
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
}
