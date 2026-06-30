package destiny.null_ouroboros.server.entity;

import destiny.null_ouroboros.common.DusterbikeTransforms;
import destiny.null_ouroboros.server.registry.EntityRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;
import java.util.UUID;

public class DusterbikeEntity extends Entity {
    private static final EntityDataAccessor<Integer> FRONT_WHEEL_ID =
            SynchedEntityData.defineId(DusterbikeEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> REAR_WHEEL_ID =
            SynchedEntityData.defineId(DusterbikeEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Optional<UUID>> FRONT_WHEEL_UUID =
            SynchedEntityData.defineId(DusterbikeEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Optional<UUID>> REAR_WHEEL_UUID =
            SynchedEntityData.defineId(DusterbikeEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Float> PITCH =
            SynchedEntityData.defineId(DusterbikeEntity.class, EntityDataSerializers.FLOAT);

    private static final int NO_WHEEL = -1;
    private static final int MISSING_WHEEL_GRACE_TICKS = 100;

    public static final float MAX_SPEED = 0.35F;
    public static final float TURN_RATE = 5F;
    public static final float TURN_RATE_MOVING = 5F;

    private boolean discarding;
    private UUID savedFrontUuid;
    private UUID savedRearUuid;
    private int missingWheelTicks;

    private float previousRenderPitch;
    private float renderPitch;

    private boolean inputForward;
    private boolean inputBackward;
    private boolean inputLeft;
    private boolean inputRight;

    private float speed;
    private boolean wheelsInitialized;
    private boolean wasLocallyControlled;
    private int clientDismountSyncGraceTicks;

    public DusterbikeEntity(EntityType<? extends DusterbikeEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(false);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(FRONT_WHEEL_ID, NO_WHEEL);
        this.entityData.define(REAR_WHEEL_ID, NO_WHEEL);
        this.entityData.define(FRONT_WHEEL_UUID, Optional.empty());
        this.entityData.define(REAR_WHEEL_UUID, Optional.empty());
        this.entityData.define(PITCH, 0.0F);
    }

    public float getSyncedPitch() {
        return this.entityData.get(PITCH);
    }

    private void setSyncedPitch(float pitch) {
        this.entityData.set(PITCH, pitch);
    }

    public float getRenderPitch(float partialTick) {
        return Mth.lerp(partialTick, previousRenderPitch, renderPitch);
    }

    public void applyRiderInput(boolean forward, boolean backward, boolean left, boolean right) {
        this.inputForward = forward;
        this.inputBackward = backward;
        this.inputLeft = left;
        this.inputRight = right;
    }

    @Override
    public void lerpTo(double x, double y, double z, float yaw, float pitch, int posRotationIncrements, boolean teleport) {
        if (this.level().isClientSide) {
            if (this.isControlledByLocalInstance()) {
                return;
            }
            if (clientDismountSyncGraceTicks > 0) {
                return;
            }
        }
        super.lerpTo(x, y, z, yaw, pitch, posRotationIncrements, teleport);
    }

    private static double computeBodyOriginY(double frontCenterY, double rearCenterY) {
        double avgCenterY = (frontCenterY + rearCenterY) * 0.5D;
        return avgCenterY - DusterbikeTransforms.WHEEL_HALF_HEIGHT;
    }

    @Override
    protected AABB makeBoundingBox() {
        return DusterbikeTransforms.bodyColliderBox(getX(), getY(), getZ());
    }

    public int getFrontWheelId() {
        return this.entityData.get(FRONT_WHEEL_ID);
    }

    public int getRearWheelId() {
        return this.entityData.get(REAR_WHEEL_ID);
    }

    public void setWheelIds(int frontId, int rearId) {
        this.entityData.set(FRONT_WHEEL_ID, frontId);
        this.entityData.set(REAR_WHEEL_ID, rearId);
    }

    public void setWheelRefs(DusterbikeWheelEntity frontWheel, DusterbikeWheelEntity rearWheel) {
        setWheelIds(frontWheel.getId(), rearWheel.getId());
        this.entityData.set(FRONT_WHEEL_UUID, Optional.of(frontWheel.getUUID()));
        this.entityData.set(REAR_WHEEL_UUID, Optional.of(rearWheel.getUUID()));
        this.savedFrontUuid = frontWheel.getUUID();
        this.savedRearUuid = rearWheel.getUUID();
    }

    public DusterbikeWheelEntity getFrontWheel() {
        return getWheelEntity(getFrontWheelId(), this.entityData.get(FRONT_WHEEL_UUID).orElse(savedFrontUuid));
    }

    public DusterbikeWheelEntity getRearWheel() {
        return getWheelEntity(getRearWheelId(), this.entityData.get(REAR_WHEEL_UUID).orElse(savedRearUuid));
    }

    private DusterbikeWheelEntity getWheelEntity(int id, UUID uuid) {
        if (id != NO_WHEEL) {
            Entity entity = this.level().getEntity(id);
            if (entity instanceof DusterbikeWheelEntity wheel) {
                return wheel;
            }
        }

        if (uuid == null) {
            return null;
        }

        for (DusterbikeWheelEntity wheel : this.level().getEntitiesOfClass(DusterbikeWheelEntity.class, this.getBoundingBox().inflate(16.0D))) {
            if (wheel.getUUID().equals(uuid)) {
                return wheel;
            }
        }

        return null;
    }

    public static DusterbikeEntity spawn(Level level, double x, double y, double z, float yaw) {
        DusterbikeEntity parent = EntityRegistry.DUSTERBIKE.get().create(level);
        if (parent == null) {
            return null;
        }

        parent.setYRot(yaw);
        parent.setPos(x, y, z);

        Vec3 frontOffset = DusterbikeTransforms.rotateLocalOffset(DusterbikeTransforms.FRONT_WHEEL_LOCAL, yaw);
        Vec3 rearOffset = DusterbikeTransforms.rotateLocalOffset(DusterbikeTransforms.REAR_WHEEL_LOCAL, yaw);

        DusterbikeWheelEntity frontWheel = new DusterbikeWheelEntity(
                EntityRegistry.DUSTERBIKE_WHEEL.get(), level, parent.getId(), parent.getUUID(),
                DusterbikeWheelEntity.WheelType.FRONT,
                x + frontOffset.x, y + frontOffset.y, z + frontOffset.z);
        DusterbikeWheelEntity rearWheel = new DusterbikeWheelEntity(
                EntityRegistry.DUSTERBIKE_WHEEL.get(), level, parent.getId(), parent.getUUID(),
                DusterbikeWheelEntity.WheelType.REAR,
                x + rearOffset.x, y + rearOffset.y, z + rearOffset.z);

        parent.setWheelRefs(frontWheel, rearWheel);
        parent.wheelsInitialized = true;

        level.addFreshEntity(frontWheel);
        level.addFreshEntity(rearWheel);
        level.addFreshEntity(parent);

        return parent;
    }

    private void ensureWheelsSpawned() {
        if (getFrontWheel() != null && getRearWheel() != null) {
            wheelsInitialized = true;
            return;
        }

        Vec3 pos = position();
        float yaw = getYRot();
        Vec3 frontOffset = DusterbikeTransforms.rotateLocalOffset(DusterbikeTransforms.FRONT_WHEEL_LOCAL, yaw);
        Vec3 rearOffset = DusterbikeTransforms.rotateLocalOffset(DusterbikeTransforms.REAR_WHEEL_LOCAL, yaw);

        DusterbikeWheelEntity frontWheel = new DusterbikeWheelEntity(
                EntityRegistry.DUSTERBIKE_WHEEL.get(), level(), getId(), getUUID(),
                DusterbikeWheelEntity.WheelType.FRONT,
                pos.x + frontOffset.x, pos.y + frontOffset.y, pos.z + frontOffset.z);
        DusterbikeWheelEntity rearWheel = new DusterbikeWheelEntity(
                EntityRegistry.DUSTERBIKE_WHEEL.get(), level(), getId(), getUUID(),
                DusterbikeWheelEntity.WheelType.REAR,
                pos.x + rearOffset.x, pos.y + rearOffset.y, pos.z + rearOffset.z);

        setWheelRefs(frontWheel, rearWheel);
        level().addFreshEntity(frontWheel);
        level().addFreshEntity(rearWheel);
        wheelsInitialized = true;
    }

    @Override
    public void tick() {
        super.tick();

        previousRenderPitch = renderPitch;
        renderPitch = getSyncedPitch();

        if (!this.level().isClientSide) {
            if (!wheelsInitialized) {
                ensureWheelsSpawned();
            }
            relinkWheelsIfNeeded();
        }

        DusterbikeWheelEntity frontWheel = getFrontWheel();
        DusterbikeWheelEntity rearWheel = getRearWheel();
        if (frontWheel == null || rearWheel == null) {
            if (!this.level().isClientSide) {
                missingWheelTicks++;
                if (missingWheelTicks > MISSING_WHEEL_GRACE_TICKS) {
                    discardWithWheels();
                }
            }
            return;
        }

        if (!this.level().isClientSide) {
            missingWheelTicks = 0;
        }

        boolean locallyControlled = getControllingPassenger() != null && this.isControlledByLocalInstance();
        if (locallyControlled) {
            wasLocallyControlled = true;
            syncPacketPositionCodec(getX(), getY(), getZ());
            seedWheelContactHeights(frontWheel, rearWheel);
            if (getControllingPassenger() != null) {
                processRiderControl();
            } else {
                inputForward = false;
                inputBackward = false;
                inputLeft = false;
                inputRight = false;
                speed = 0.0F;
            }
        } else {
            this.setDeltaMovement(Vec3.ZERO);
            if (this.level().isClientSide && wasLocallyControlled) {
                resetInterpolationToCurrentPosition();
                clientDismountSyncGraceTicks = 3;
                wasLocallyControlled = false;
            }
        }

        if (clientDismountSyncGraceTicks > 0) {
            clientDismountSyncGraceTicks--;
        }

        if (!this.level().isClientSide || locallyControlled) {
            updateWheelPhysics(frontWheel, rearWheel);
            updateBodyFromWheels(frontWheel, rearWheel);
        }

        Entity rider = getControllingPassenger();
        if (rider instanceof LivingEntity living) {
            living.setDeltaMovement(Vec3.ZERO);
            living.fallDistance = 0.0F;
        }
    }

    private static void seedWheelContactHeights(DusterbikeWheelEntity frontWheel, DusterbikeWheelEntity rearWheel) {
        frontWheel.setContactY(frontWheel.getY());
        rearWheel.setContactY(rearWheel.getY());
    }

    private void resetInterpolationToCurrentPosition() {
        this.xo = getX();
        this.yo = getY();
        this.zo = getZ();
        this.yRotO = getYRot();
        this.xRotO = getXRot();
    }

    private void processRiderControl() {
        applyControlInput();
        DusterbikeWheelEntity frontWheel = getFrontWheel();
        DusterbikeWheelEntity rearWheel = getRearWheel();
        if (frontWheel != null && rearWheel != null) {
            moveBodyHorizontally(frontWheel, rearWheel);
        }
    }

    private void applyControlInput() {
        if (inputForward && !inputBackward) {
            speed = MAX_SPEED;
        } else if (inputBackward && !inputForward) {
            speed = -MAX_SPEED;
        } else {
            speed = 0.0F;
        }

        float turnRate = speed != 0.0F ? TURN_RATE_MOVING : TURN_RATE;
        if (inputLeft && !inputRight) {
            setYRot(getYRot() - turnRate);
        } else if (inputRight && !inputLeft) {
            setYRot(getYRot() + turnRate);
        }
    }

    private void moveBodyHorizontally(DusterbikeWheelEntity frontWheel, DusterbikeWheelEntity rearWheel) {
        if (speed == 0.0F) {
            return;
        }

        float yawRad = getYRot() * Mth.DEG_TO_RAD;
        Vec3 delta = new Vec3(-Mth.sin(yawRad) * speed, 0.0D, Mth.cos(yawRad) * speed);

        double nextX = getX() + delta.x;
        double nextZ = getZ() + delta.z;
        float yaw = getYRot();

        Vec3 frontOffset = DusterbikeTransforms.rotateLocalOffset(DusterbikeTransforms.FRONT_WHEEL_LOCAL, yaw);
        Vec3 rearOffset = DusterbikeTransforms.rotateLocalOffset(DusterbikeTransforms.REAR_WHEEL_LOCAL, yaw);

        boolean frontIsLead = frontOffset.dot(delta) > rearOffset.dot(delta);
        DusterbikeWheelEntity leadWheel = frontIsLead ? frontWheel : rearWheel;
        Vec3 leadOffset = frontIsLead ? frontOffset : rearOffset;
        DusterbikePhysics.WheelStepResult leadProbe = DusterbikePhysics.probeStepSurface(
                level(), nextX + leadOffset.x, nextZ + leadOffset.z, leadWheel.getContactY());

        if (leadProbe.blocked()) {
            return;
        }

        setPos(getX() + delta.x, getY(), getZ() + delta.z);
    }

    private void updateWheelPhysics(DusterbikeWheelEntity frontWheel, DusterbikeWheelEntity rearWheel) {
        Vec3 bodyPos = position();
        float yaw = getYRot();

        Vec3 frontOffset = DusterbikeTransforms.rotateLocalOffset(DusterbikeTransforms.FRONT_WHEEL_LOCAL, yaw);
        Vec3 rearOffset = DusterbikeTransforms.rotateLocalOffset(DusterbikeTransforms.REAR_WHEEL_LOCAL, yaw);

        double frontTargetX = bodyPos.x + frontOffset.x;
        double frontTargetZ = bodyPos.z + frontOffset.z;
        double rearTargetX = bodyPos.x + rearOffset.x;
        double rearTargetZ = bodyPos.z + rearOffset.z;
        double frontRestY = bodyPos.y + DusterbikeTransforms.FRONT_WHEEL_LOCAL.y;
        double rearRestY = bodyPos.y + DusterbikeTransforms.REAR_WHEEL_LOCAL.y;

        DusterbikePhysics.WheelContactResult frontResult = DusterbikePhysics.probeGround(
                level(), frontTargetX, frontTargetZ, frontWheel.getContactY(), frontRestY);
        DusterbikePhysics.WheelContactResult rearResult = DusterbikePhysics.probeGround(
                level(), rearTargetX, rearTargetZ, rearWheel.getContactY(), rearRestY);

        double[] resolved = DusterbikePhysics.resolveAnchoredWheelHeights(
                frontWheel.getContactY(), frontResult.contactY(), frontRestY,
                rearWheel.getContactY(), rearResult.contactY(), rearRestY);

        frontWheel.syncColliderPosition(frontTargetX, resolved[0], frontTargetZ);
        rearWheel.syncColliderPosition(rearTargetX, resolved[1], rearTargetZ);

        DusterbikePhysics.BodyUpdateResult pitchResult = DusterbikePhysics.computePitch(
                frontWheel.getContactY(), rearWheel.getContactY());
        setSyncedPitch(pitchResult.pitchDegrees());
    }

    private void updateBodyFromWheels(DusterbikeWheelEntity frontWheel, DusterbikeWheelEntity rearWheel) {
        double targetY = computeBodyOriginY(frontWheel.getContactY(), rearWheel.getContactY());
        setPos(getX(), targetY, getZ());
        setBoundingBox(makeBoundingBox());
    }

    /** Called on spawn to align wheels; client must not run this. */
    public void updateBodyFromWheels() {
        DusterbikeWheelEntity frontWheel = getFrontWheel();
        DusterbikeWheelEntity rearWheel = getRearWheel();
        if (frontWheel == null || rearWheel == null) {
            return;
        }

        updateWheelPhysics(frontWheel, rearWheel);
        updateBodyFromWheels(frontWheel, rearWheel);
    }

    private void relinkWheelsIfNeeded() {
        if (getFrontWheel() != null && getRearWheel() != null) {
            return;
        }

        UUID frontUuid = this.entityData.get(FRONT_WHEEL_UUID).orElse(savedFrontUuid);
        UUID rearUuid = this.entityData.get(REAR_WHEEL_UUID).orElse(savedRearUuid);
        if (frontUuid == null && rearUuid == null) {
            return;
        }

        int frontId = getFrontWheelId();
        int rearId = getRearWheelId();

        for (DusterbikeWheelEntity wheel : this.level().getEntitiesOfClass(DusterbikeWheelEntity.class, this.getBoundingBox().inflate(16.0D))) {
            if (frontUuid != null && wheel.getUUID().equals(frontUuid)) {
                frontId = wheel.getId();
            }
            if (rearUuid != null && wheel.getUUID().equals(rearUuid)) {
                rearId = wheel.getId();
            }
        }

        if (frontId != NO_WHEEL || rearId != NO_WHEEL) {
            setWheelIds(frontId == NO_WHEEL ? getFrontWheelId() : frontId, rearId == NO_WHEEL ? getRearWheelId() : rearId);
        }

        if (getFrontWheel() != null && getRearWheel() != null) {
            missingWheelTicks = 0;
        }
    }

    public void onWheelRemoved(DusterbikeWheelEntity wheel) {
        if (discarding) {
            return;
        }
        discardWithWheels();
    }

    public void discardWithWheels() {
        if (discarding) {
            return;
        }
        discarding = true;

        DusterbikeWheelEntity frontWheel = getFrontWheel();
        DusterbikeWheelEntity rearWheel = getRearWheel();
        if (frontWheel != null && frontWheel.isAlive()) {
            frontWheel.discard();
        }
        if (rearWheel != null && rearWheel.isAlive()) {
            rearWheel.discard();
        }
        this.discard();
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!this.level().isClientSide && !discarding && reason.shouldDestroy()) {
            discarding = true;
            DusterbikeWheelEntity frontWheel = getFrontWheel();
            DusterbikeWheelEntity rearWheel = getRearWheel();
            if (frontWheel != null && frontWheel.isAlive()) {
                frontWheel.discard();
            }
            if (rearWheel != null && rearWheel.isAlive()) {
                rearWheel.discard();
            }
        }
        super.remove(reason);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.contains("FrontWheel")) {
            this.entityData.set(FRONT_WHEEL_ID, tag.getInt("FrontWheel"));
        }
        if (tag.contains("RearWheel")) {
            this.entityData.set(REAR_WHEEL_ID, tag.getInt("RearWheel"));
        }
        if (tag.hasUUID("FrontWheelUuid")) {
            savedFrontUuid = tag.getUUID("FrontWheelUuid");
            this.entityData.set(FRONT_WHEEL_UUID, Optional.of(savedFrontUuid));
        }
        if (tag.hasUUID("RearWheelUuid")) {
            savedRearUuid = tag.getUUID("RearWheelUuid");
            this.entityData.set(REAR_WHEEL_UUID, Optional.of(savedRearUuid));
        }
        if (tag.contains("Pitch")) {
            setSyncedPitch(tag.getFloat("Pitch"));
        }
        if (tag.contains("WheelsInitialized")) {
            wheelsInitialized = tag.getBoolean("WheelsInitialized");
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        DusterbikeWheelEntity frontWheel = getFrontWheel();
        DusterbikeWheelEntity rearWheel = getRearWheel();
        if (frontWheel != null) {
            tag.putInt("FrontWheel", frontWheel.getId());
            tag.putUUID("FrontWheelUuid", frontWheel.getUUID());
        } else if (savedFrontUuid != null) {
            tag.putUUID("FrontWheelUuid", savedFrontUuid);
        }
        if (rearWheel != null) {
            tag.putInt("RearWheel", rearWheel.getId());
            tag.putUUID("RearWheelUuid", rearWheel.getUUID());
        } else if (savedRearUuid != null) {
            tag.putUUID("RearWheelUuid", savedRearUuid);
        }
        tag.putFloat("Pitch", getSyncedPitch());
        tag.putBoolean("WheelsInitialized", wheelsInitialized);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.isInvulnerableTo(source) || source.is(DamageTypeTags.IS_FALL)) {
            return false;
        }

        if (!this.level().isClientSide) {
            discardWithWheels();
        }

        return true;
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (player.isSecondaryUseActive() || !getPassengers().isEmpty()) {
            return InteractionResult.PASS;
        }

        if (!this.level().isClientSide) {
            player.startRiding(this);
        }

        return InteractionResult.sidedSuccess(this.level().isClientSide);
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return getPassengers().isEmpty();
    }

    @Override
    public LivingEntity getControllingPassenger() {
        Entity entity = getFirstPassenger();
        return entity instanceof LivingEntity living ? living : null;
    }

    @Override
    protected void positionRider(Entity passenger, MoveFunction moveFunction) {
        if (!hasPassenger(passenger)) {
            return;
        }

        Vec3 seat = DusterbikeTransforms.worldPointFromLocal(
                position(), getYRot(), getSyncedPitch(), DusterbikeTransforms.DRIVER_LOCAL);
        moveFunction.accept(passenger, seat.x, seat.y - 0.875D, seat.z);
    }

    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        Vec3 offset = DusterbikeTransforms.rotateLocalOffset(new Vec3(0.75D, 0.0D, 0.0D), getYRot());
        return position().add(offset);
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
