package destiny.null_ouroboros.server.entity;

import destiny.null_ouroboros.common.dusterbike.HoistPartTargetType;
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

import java.util.UUID;

public class HoistPartInteractionEntity extends ParentLinkedHitboxEntity {
    private static final EntityDataAccessor<Integer> TARGET_TYPE =
            SynchedEntityData.defineId(HoistPartInteractionEntity.class, EntityDataSerializers.INT);

    public HoistPartInteractionEntity(EntityType<?> type, Level level) {
        super(type, level);
    }

    public HoistPartInteractionEntity(EntityType<?> type, Level level,
                                      int parentId, UUID parentUuid,
                                      HoistPartTargetType targetType, double x, double y, double z) {
        this(type, level);
        setTargetType(targetType);
        initParentLink(parentId, parentUuid, x, y, z);
    }

    @Override
    protected void defineAdditionalSynchedData() {
        this.entityData.define(TARGET_TYPE, HoistPartTargetType.ENGINE.id());
    }

    public HoistPartTargetType getTargetType() {
        return HoistPartTargetType.byId(this.entityData.get(TARGET_TYPE));
    }

    public void setTargetType(HoistPartTargetType targetType) {
        this.entityData.set(TARGET_TYPE, targetType.id());
    }

    public EngineHoistEntity getParent() {
        Entity entity = getParentId() != NO_PARENT ? this.level().getEntity(getParentId()) : null;
        return entity instanceof EngineHoistEntity hoist ? hoist : null;
    }

    public EngineHoistEntity findParent() {
        return findParentOfType(EngineHoistEntity.class);
    }

    @Override
    protected Entity resolveParent() {
        return findParent();
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        EngineHoistEntity hoist = getParent();
        if (hoist != null) {
            if (getTargetType() == HoistPartTargetType.KEY) {
                hoist.handleKeyPortInteraction(player, hand);
            } else {
                hoist.handlePartInteraction(player, hand, getTargetType(), player.isSecondaryUseActive());
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        return InteractionResult.PASS;
    }

    @Override
    protected AABB makeBoundingBox() {
        EngineHoistEntity parent = getParent();
        float yaw = parent != null ? parent.getYRot() : 0.0F;
        HoistPartTargetType target = getTargetType();

        double hw = target.halfWidth();
        double hh = target.halfHeight();
        double hd = target.halfDepth();

        double rad = Math.toRadians(yaw);
        double extX = Math.abs(hw * Math.cos(rad)) + Math.abs(hd * Math.sin(rad));
        double extY = hh;
        double extZ = Math.abs(hw * Math.sin(rad)) + Math.abs(hd * Math.cos(rad));

        double cx = getX();
        double cy = getY();
        double cz = getZ();

        return new AABB(cx - extX, cy - extY, cz - extZ, cx + extX, cy + extY, cz + extZ);
    }

    @Override
    protected void readAdditionalHitboxData(CompoundTag tag) {
        if (tag.contains("TargetType")) {
            setTargetType(HoistPartTargetType.byId(tag.getInt("TargetType")));
        }
    }

    @Override
    protected void writeAdditionalHitboxData(CompoundTag tag) {
        tag.putInt("TargetType", getTargetType().id());
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        EngineHoistEntity parent = findParent();
        return parent != null && parent.hurt(source, amount);
    }
}
