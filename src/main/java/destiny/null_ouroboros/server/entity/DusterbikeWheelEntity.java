package destiny.null_ouroboros.server.entity;

import destiny.null_ouroboros.common.DusterbikeTransforms;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Optional;
import java.util.UUID;

public class DusterbikeWheelEntity extends Entity {
    public enum WheelType {
        FRONT,
        REAR
    }

    private static final EntityDataAccessor<Integer> PARENT_ID =
            SynchedEntityData.defineId(DusterbikeWheelEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Optional<UUID>> PARENT_UUID =
            SynchedEntityData.defineId(DusterbikeWheelEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Integer> WHEEL_TYPE =
            SynchedEntityData.defineId(DusterbikeWheelEntity.class, EntityDataSerializers.INT);

    private static final int NO_PARENT = -1;
    private static final int MISSING_PARENT_GRACE_TICKS = 120;
    private static final double MAX_MOVE_STEP = 0.015625D;
    private static final int MAX_PENETRATION_ATTEMPTS = 8;

    private int missingParentTicks;
    private double contactY;

    public DusterbikeWheelEntity(EntityType<? extends DusterbikeWheelEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    public DusterbikeWheelEntity(EntityType<? extends DusterbikeWheelEntity> type, Level level, int parentId, UUID parentUuid,
                                 WheelType wheelType, double x, double y, double z) {
        this(type, level);
        setParentId(parentId);
        setParentUuid(parentUuid);
        setWheelType(wheelType);
        setPos(x, y, z);
        setContactY(y);
        setDeltaMovement(Vec3.ZERO);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(PARENT_ID, NO_PARENT);
        this.entityData.define(PARENT_UUID, Optional.empty());
        this.entityData.define(WHEEL_TYPE, WheelType.REAR.ordinal());
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

    public WheelType getWheelType() {
        return WheelType.values()[this.entityData.get(WHEEL_TYPE)];
    }

    public void setWheelType(WheelType wheelType) {
        this.entityData.set(WHEEL_TYPE, wheelType.ordinal());
    }

    public double getContactY() {
        return contactY;
    }

    public void setContactY(double contactY) {
        this.contactY = contactY;
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
    public void lerpTo(double x, double y, double z, float yaw, float pitch, int posRotationIncrements, boolean teleport) {
        DusterbikeEntity parent = getParent();
        if (parent != null && parent.isControlledByLocalInstance()) {
            return;
        }
        setContactY(y);
        super.lerpTo(x, y, z, yaw, pitch, posRotationIncrements, teleport);
    }

    @Override
    protected AABB makeBoundingBox() {
        DusterbikeEntity parent = getParent();
        float yaw = parent != null ? parent.getYRot() : 0.0F;
        return DusterbikeTransforms.wheelColliderBox(getX(), getY(), getZ(), yaw);
    }

    public void syncColliderPosition(double centerX, double centerY, double centerZ) {
        setPos(centerX, centerY, centerZ);
        setContactY(centerY);
        setBoundingBox(makeBoundingBox());
    }

    public void moveWithCollision(Vec3 delta) {
        double length = delta.length();
        if (length < 1.0E-8D) {
            return;
        }

        int steps = Math.max(1, (int) Math.ceil(length / MAX_MOVE_STEP));
        Vec3 step = delta.scale(1.0D / steps);
        for (int i = 0; i < steps; i++) {
            this.move(MoverType.SELF, step);
            resolvePenetration();
        }
    }

    public void resolvePenetration() {
        for (int attempt = 0; attempt < MAX_PENETRATION_ATTEMPTS; attempt++) {
            AABB box = getBoundingBox();
            if (level().noCollision(this, box)) {
                return;
            }

            Vec3 push = computeMinimumPushOut(box);
            if (push.lengthSqr() < 1.0E-10D) {
                return;
            }

            setPos(getX() + push.x, getY() + push.y, getZ() + push.z);
        }
    }

    private Vec3 computeMinimumPushOut(AABB entityBox) {
        Level level = level();
        BlockPos minPos = BlockPos.containing(entityBox.minX - 0.5D, entityBox.minY - 0.5D, entityBox.minZ - 0.5D);
        BlockPos maxPos = BlockPos.containing(entityBox.maxX + 0.5D, entityBox.maxY + 0.5D, entityBox.maxZ + 0.5D);

        Vec3 bestPush = Vec3.ZERO;
        double bestLength = Double.MAX_VALUE;

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = minPos.getX(); x <= maxPos.getX(); x++) {
            for (int y = minPos.getY(); y <= maxPos.getY(); y++) {
                for (int z = minPos.getZ(); z <= maxPos.getZ(); z++) {
                    pos.set(x, y, z);
                    BlockState state = level.getBlockState(pos);
                    VoxelShape shape = state.getCollisionShape(level, pos);
                    if (shape.isEmpty()) {
                        continue;
                    }

                    for (AABB blockPart : shape.toAabbs()) {
                        AABB blockBox = blockPart.move(pos);
                        if (!entityBox.intersects(blockBox)) {
                            continue;
                        }

                        Vec3 push = minimumSeparation(entityBox, blockBox);
                        double pushLength = push.lengthSqr();
                        if (pushLength > 1.0E-10D && pushLength < bestLength) {
                            bestLength = pushLength;
                            bestPush = push;
                        }
                    }
                }
            }
        }

        return bestPush;
    }

    private static Vec3 minimumSeparation(AABB entity, AABB block) {
        double[][] options = {
                {block.minX - entity.maxX, 0.0D, 0.0D},
                {block.maxX - entity.minX, 0.0D, 0.0D},
                {0.0D, block.minY - entity.maxY, 0.0D},
                {0.0D, block.maxY - entity.minY, 0.0D},
                {0.0D, 0.0D, block.minZ - entity.maxZ},
                {0.0D, 0.0D, block.maxZ - entity.minZ},
        };

        Vec3 best = Vec3.ZERO;
        double bestAbs = Double.MAX_VALUE;
        for (double[] option : options) {
            double abs = Math.abs(option[0]) + Math.abs(option[1]) + Math.abs(option[2]);
            if (abs < 1.0E-10D || abs >= bestAbs) {
                continue;
            }

            bestAbs = abs;
            best = new Vec3(option[0], option[1], option[2]);
        }

        return best;
    }

    @Override
    public void tick() {
        super.tick();

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
            parent.onWheelRemoved(this);
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

        for (DusterbikeEntity bike : this.level().getEntitiesOfClass(DusterbikeEntity.class, this.getBoundingBox().inflate(16.0D))) {
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
        if (tag.contains("WheelType")) {
            setWheelType(WheelType.values()[tag.getInt("WheelType")]);
        }
        if (tag.contains("ContactY")) {
            contactY = tag.getDouble("ContactY");
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (getParentId() != NO_PARENT) {
            tag.putInt("Parent", getParentId());
        }
        getParentUuid().ifPresent(uuid -> tag.putUUID("ParentUuid", uuid));
        tag.putInt("WheelType", getWheelType().ordinal());
        tag.putDouble("ContactY", contactY);
        tag.putUUID("WheelUuid", this.getUUID());
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
        return false;
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
