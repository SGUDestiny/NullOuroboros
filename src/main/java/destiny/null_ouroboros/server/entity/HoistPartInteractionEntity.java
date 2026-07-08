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
import net.minecraft.world.phys.Vec3;

import java.util.Optional;
import java.util.UUID;

public class HoistPartInteractionEntity extends Entity {
    private static final EntityDataAccessor<Integer> PARENT_ID =
            SynchedEntityData.defineId(HoistPartInteractionEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Optional<UUID>> PARENT_UUID =
            SynchedEntityData.defineId(HoistPartInteractionEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Integer> TARGET_TYPE =
            SynchedEntityData.defineId(HoistPartInteractionEntity.class, EntityDataSerializers.INT);

    private static final int NO_PARENT = -1;
    private static final int MISSING_PARENT_GRACE_TICKS = 120;

    private int missingParentTicks;

    public HoistPartInteractionEntity(EntityType<?> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
    }

    public HoistPartInteractionEntity(EntityType<?> type, Level level,
                                      int parentId, UUID parentUuid,
                                      HoistPartTargetType targetType, double x, double y, double z) {
        this(type, level);
        setParentId(parentId);
        setParentUuid(parentUuid);
        setTargetType(targetType);
        setPos(x, y, z);
        setDeltaMovement(Vec3.ZERO);
        refreshColliderBox();
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
    protected void defineSynchedData() {
        this.entityData.define(PARENT_ID, NO_PARENT);
        this.entityData.define(PARENT_UUID, Optional.empty());
        this.entityData.define(TARGET_TYPE, HoistPartTargetType.ENGINE.id());
    }

    public int getParentId() { return this.entityData.get(PARENT_ID); }
    public void setParentId(int parentId) { this.entityData.set(PARENT_ID, parentId); }
    public Optional<UUID> getParentUuid() { return this.entityData.get(PARENT_UUID); }
    public void setParentUuid(UUID parentUuid) { this.entityData.set(PARENT_UUID, Optional.ofNullable(parentUuid)); }
    public HoistPartTargetType getTargetType() { return HoistPartTargetType.byId(this.entityData.get(TARGET_TYPE)); }
    public void setTargetType(HoistPartTargetType targetType) { this.entityData.set(TARGET_TYPE, targetType.id()); }

    public EngineHoistEntity getParent() {
        if (getParentId() != NO_PARENT) {
            Entity entity = this.level().getEntity(getParentId());
            if (entity instanceof EngineHoistEntity hoist) return hoist;
        }
        return null;
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

    public EngineHoistEntity findParent() {
        int parentId = getParentId();
        if (parentId != NO_PARENT) {
            Entity entity = this.level().getEntity(parentId);
            if (entity instanceof EngineHoistEntity hoist) return hoist;
        }
        Optional<UUID> parentUuid = getParentUuid();
        if (parentUuid.isEmpty()) return null;
        for (EngineHoistEntity hoist : this.level().getEntitiesOfClass(
                EngineHoistEntity.class, this.getBoundingBox().inflate(16.0D))) {
            if (hoist.getUUID().equals(parentUuid.get())) {
                setParentId(hoist.getId());
                return hoist;
            }
        }
        return null;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.contains("Parent")) setParentId(tag.getInt("Parent"));
        if (tag.hasUUID("ParentUuid")) setParentUuid(tag.getUUID("ParentUuid"));
        if (tag.contains("TargetType")) setTargetType(HoistPartTargetType.byId(tag.getInt("TargetType")));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (getParentId() != NO_PARENT) tag.putInt("Parent", getParentId());
        getParentUuid().ifPresent(uuid -> tag.putUUID("ParentUuid", uuid));
        tag.putInt("TargetType", getTargetType().id());
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        EngineHoistEntity parent = findParent();
        return parent != null && parent.hurt(source, amount);
    }

    @Override
    public boolean isPickable() { return true; }
    @Override
    public boolean isPushable() { return false; }
    @Override
    public boolean canBeCollidedWith() { return false; }
}