package destiny.null_ouroboros.server.entity;

import destiny.null_ouroboros.common.DusterbikeGear;
import destiny.null_ouroboros.common.DusterbikeRiderAnimation;
import destiny.null_ouroboros.common.DusterbikeTransforms;
import destiny.null_ouroboros.server.network.ServerBoundDusterbikeDrivePacket;
import destiny.null_ouroboros.server.registry.EntityRegistry;
import destiny.null_ouroboros.server.registry.PacketHandlerRegistry;
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
    private static final EntityDataAccessor<Float> FORWARD_SPEED =
            SynchedEntityData.defineId(DusterbikeEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Byte> GEAR =
            SynchedEntityData.defineId(DusterbikeEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Float> STEER_ANGLE =
            SynchedEntityData.defineId(DusterbikeEntity.class, EntityDataSerializers.FLOAT);

    private static final int NO_WHEEL = -1;
    private static final int MISSING_WHEEL_GRACE_TICKS = 100;

    public static final float MAX_SPEED = DusterbikePhysics.MAX_FORWARD_SPEED;

    private boolean discarding;
    private UUID savedFrontUuid;
    private UUID savedRearUuid;
    private int missingWheelTicks;

    private float previousRenderPitch;
    private float renderPitch;
    private float previousRenderSteer;
    private float renderSteer;

    private boolean inputForward;
    private boolean inputBackward;
    private boolean inputLeft;
    private boolean inputRight;
    private boolean inputHandbrake;

    private float forwardSpeed;
    private float steerAngle;
    private boolean wheelsInitialized;
    private boolean wasLocallyControlled;
    private int clientDismountSyncGraceTicks;

    private float savedFrontWheelRotation;
    private float savedRearWheelRotation;
    private double savedFrontWheelAngularVelocity;
    private double savedRearWheelAngularVelocity;
    private boolean pendingWheelSpinRestore;

    /** Client-authoritative gear while driving; avoids server sync overwriting shifts before the packet lands. */
    private DusterbikeGear driverGear;

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
        this.entityData.define(FORWARD_SPEED, 0.0F);
        this.entityData.define(GEAR, (byte) DusterbikeGear.N.ordinal());
        this.entityData.define(STEER_ANGLE, 0.0F);
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

    public float getSyncedSteerAngle() {
        return this.entityData.get(STEER_ANGLE);
    }

    private void setSyncedSteerAngle(float steer) {
        this.steerAngle = steer;
        if (!usesClientDriveAuthority()) {
            this.entityData.set(STEER_ANGLE, steer);
        }
    }

    public float getRenderSteer(float partialTick) {
        return Mth.lerp(partialTick, previousRenderSteer, renderSteer);
    }

    public float getRenderRoll(float partialTick) {
        float steer = getRenderSteer(partialTick);
        float speed = getDriveForwardSpeed();
        float maxSteer = DusterbikePhysics.computeMaxSteerDegrees(Math.abs(speed));
        return DusterbikePhysics.computeRollDegrees(speed, steer, maxSteer);
    }

    public float getRenderYaw(float partialTick) {
        return Mth.rotLerp(partialTick, this.yRotO, this.getYRot());
    }

    public float getSyncedForwardSpeed() {
        return this.entityData.get(FORWARD_SPEED);
    }

    public float getDriveForwardSpeed() {
        if (usesClientDriveAuthority()) {
            return this.forwardSpeed;
        }
        return getSyncedForwardSpeed();
    }

    private boolean usesClientDriveAuthority() {
        return hasLocalDriver();
    }

    private boolean hasLocalDriver() {
        if (!this.level().isClientSide) {
            return false;
        }
        LivingEntity passenger = getControllingPassenger();
        return passenger != null && passenger.isControlledByLocalInstance();
    }

    private void setSyncedForwardSpeed(float speed) {
        this.forwardSpeed = speed;
        if (!usesClientDriveAuthority()) {
            this.entityData.set(FORWARD_SPEED, speed);
        }
    }

    private void publishDriveStateToServer() {
        if (!usesClientDriveAuthority()) {
            return;
        }

        DusterbikeGear gear = this.driverGear != null ? this.driverGear : getGear();
        this.entityData.set(FORWARD_SPEED, this.forwardSpeed);
        this.entityData.set(STEER_ANGLE, this.steerAngle);
        this.entityData.set(GEAR, (byte) gear.ordinal());
        PacketHandlerRegistry.INSTANCE.sendToServer(new ServerBoundDusterbikeDrivePacket(
                getId(), (byte) gear.ordinal(), this.forwardSpeed, this.steerAngle));
    }

    public void applyRiderInput(boolean forward, boolean backward, boolean left, boolean right, boolean handbrake) {
        this.inputForward = forward;
        this.inputBackward = backward;
        this.inputLeft = left;
        this.inputRight = right;
        this.inputHandbrake = handbrake;
    }

    public DusterbikeGear getGear() {
        if (this.level().isClientSide && getControllingPassenger() != null && this.driverGear != null) {
            return this.driverGear;
        }

        return gearFromSyncedData();
    }

    private DusterbikeGear gearFromSyncedData() {
        byte ordinal = this.entityData.get(GEAR);
        DusterbikeGear[] gears = DusterbikeGear.values();
        if (ordinal < 0 || ordinal >= gears.length) {
            return DusterbikeGear.N;
        }
        return gears[ordinal];
    }

    private void setGear(DusterbikeGear gear) {
        this.entityData.set(GEAR, (byte) gear.ordinal());
        if (this.level().isClientSide && getControllingPassenger() != null) {
            this.driverGear = gear;
        }
    }

    public boolean shiftGear(int direction) {
        DusterbikeGear current = getGear();
        DusterbikeGear next = current.shift(direction);
        if (next == current) {
            return false;
        }

        setGear(next);
        if (this.level().isClientSide) {
            this.driverGear = next;
        }
        setSyncedForwardSpeed(DusterbikePhysics.clampSpeedForGear(forwardSpeed, next));
        return true;
    }

    private void beginLocalControl() {
        this.driverGear = gearFromSyncedData();
        this.forwardSpeed = DusterbikePhysics.clampSpeedForGear(getSyncedForwardSpeed(), this.driverGear);
        this.steerAngle = getSyncedSteerAngle();
    }

    private void endLocalControl() {
        publishDriveStateToServer();
        this.driverGear = null;
        this.inputForward = false;
        this.inputBackward = false;
        this.inputLeft = false;
        this.inputRight = false;
        this.inputHandbrake = false;
    }

    public void applyClientDriveState(DusterbikeGear gear, float speed, float steer) {
        if (this.level().isClientSide) {
            return;
        }

        this.driverGear = null;
        this.entityData.set(GEAR, (byte) gear.ordinal());
        this.forwardSpeed = DusterbikePhysics.clampSpeedForGear(speed, gear);
        this.entityData.set(FORWARD_SPEED, this.forwardSpeed);
        this.steerAngle = steer;
        this.entityData.set(STEER_ANGLE, steer);
    }

    public float getGearMaxSpeedMagnitude() {
        return DusterbikePhysics.gearMaxSpeedMagnitude(getGear());
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
        return DusterbikeTransforms.bodyColliderBox(getX(), getY(), getZ(), getYRot());
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
        Vec3 frontOffset = DusterbikeTransforms.rotateSteeredWheelLocalOffset(
                DusterbikeTransforms.FRONT_WHEEL_LOCAL, steerAngle, yaw);
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
        applySavedWheelSpinToWheels(frontWheel, rearWheel);
        wheelsInitialized = true;
    }

    @Override
    public void tick() {
        super.tick();

        previousRenderPitch = renderPitch;
        renderPitch = getSyncedPitch();
        previousRenderSteer = renderSteer;
        renderSteer = usesClientDriveAuthority() ? steerAngle : getSyncedSteerAngle();

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

        restoreWheelSpinStateIfNeeded(frontWheel, rearWheel);

        boolean clientDriving = hasLocalDriver();
        boolean serverCoasting = !this.level().isClientSide
                && getControllingPassenger() == null
                && Math.abs(getSyncedForwardSpeed()) > DusterbikePhysics.SPEED_EPSILON;
        boolean shouldRunDrivePhysics = clientDriving || serverCoasting;

        if (!clientDriving && !this.level().isClientSide) {
            forwardSpeed = getSyncedForwardSpeed();
            steerAngle = getSyncedSteerAngle();
        }

        if (clientDriving) {
            if (!wasLocallyControlled) {
                beginLocalControl();
            }
            wasLocallyControlled = true;
            syncPacketPositionCodec(getX(), getY(), getZ());
            seedWheelContactHeights(frontWheel, rearWheel);
            runDriveSimulation(frontWheel, rearWheel);
        } else {
            if (!this.level().isClientSide) {
                this.setDeltaMovement(Vec3.ZERO);
            }
            if (this.level().isClientSide && wasLocallyControlled && getControllingPassenger() == null) {
                resetInterpolationToCurrentPosition();
                clientDismountSyncGraceTicks = 3;
                wasLocallyControlled = false;
                endLocalControl();
            }
        }

        if (serverCoasting) {
            runDriveSimulation(frontWheel, rearWheel);
        }

        if (clientDismountSyncGraceTicks > 0) {
            clientDismountSyncGraceTicks--;
        }

        if (!clientDriving && !serverCoasting
                && (!this.level().isClientSide || shouldRunDrivePhysics)) {
            updateWheelPhysics(frontWheel, rearWheel);
            updateBodyFromWheels(frontWheel, rearWheel);
        }

        LivingEntity rider = getControllingPassenger();
        if (rider != null) {
            rider.setDeltaMovement(Vec3.ZERO);
            rider.fallDistance = 0.0F;
            DusterbikeRiderAnimation.syncRiderToBike(rider, this);
        }
    }

    private static void seedWheelContactHeights(DusterbikeWheelEntity frontWheel, DusterbikeWheelEntity rearWheel) {
        frontWheel.setContactY(frontWheel.getY());
        rearWheel.setContactY(rearWheel.getY());
    }

    private void captureWheelSpinState(DusterbikeWheelEntity frontWheel, DusterbikeWheelEntity rearWheel) {
        savedFrontWheelRotation = frontWheel.getSyncedRotationAngle();
        savedFrontWheelAngularVelocity = frontWheel.getAngularVelocity();
        savedRearWheelRotation = rearWheel.getSyncedRotationAngle();
        savedRearWheelAngularVelocity = rearWheel.getAngularVelocity();
    }

    private void applySavedWheelSpinToWheels(DusterbikeWheelEntity frontWheel, DusterbikeWheelEntity rearWheel) {
        frontWheel.applySpinState(savedFrontWheelRotation, savedFrontWheelAngularVelocity);
        rearWheel.applySpinState(savedRearWheelRotation, savedRearWheelAngularVelocity);
    }

    private void restoreWheelSpinStateIfNeeded(DusterbikeWheelEntity frontWheel, DusterbikeWheelEntity rearWheel) {
        if (!pendingWheelSpinRestore) {
            return;
        }
        applySavedWheelSpinToWheels(frontWheel, rearWheel);
        pendingWheelSpinRestore = false;
    }

    private void resetInterpolationToCurrentPosition() {
        this.xo = getX();
        this.yo = getY();
        this.zo = getZ();
        this.yRotO = getYRot();
        this.xRotO = getXRot();
    }

    private void runDriveSimulation(DusterbikeWheelEntity frontWheel, DusterbikeWheelEntity rearWheel) {
        updateWheelPhysics(frontWheel, rearWheel);
        tickDrivePhysics(frontWheel, rearWheel);
        updateWheelPhysics(frontWheel, rearWheel);
        updateBodyFromWheels(frontWheel, rearWheel);
    }

    private void tickDrivePhysics(DusterbikeWheelEntity frontWheel, DusterbikeWheelEntity rearWheel) {
        refreshLocalRiderInput();

        boolean rearGrounded = rearWheel.isGrounded();
        boolean frontGrounded = frontWheel.isGrounded();
        DusterbikeGear gear = getGear();
        boolean holdingForward = gear.allowsForwardThrottle() && inputForward && !inputBackward;

        if (inputHandbrake) {
            setSyncedForwardSpeed(DusterbikePhysics.applyCoastDrag(forwardSpeed, DusterbikePhysics.HANDBRAKE_DECEL));
        } else if (holdingForward) {
            if (forwardSpeed < 0.0F) {
                setSyncedForwardSpeed(DusterbikePhysics.applyCoastDrag(forwardSpeed, DusterbikePhysics.BRAKE_DECEL));
            } else {
                float accel = DusterbikePhysics.computeForwardThrottleAcceleration(forwardSpeed, gear);
                setSyncedForwardSpeed(DusterbikePhysics.approachSpeed(
                        forwardSpeed, gear.maxSpeed(), accel));
            }
        } else if (gear.allowsReverseThrottle() && inputForward && !inputBackward && rearGrounded) {
            if (forwardSpeed > DusterbikePhysics.SPEED_EPSILON) {
                setSyncedForwardSpeed(DusterbikePhysics.applyCoastDrag(forwardSpeed, DusterbikePhysics.BRAKE_DECEL));
            } else {
                float accel = DusterbikePhysics.computeReverseThrottleAcceleration(forwardSpeed);
                setSyncedForwardSpeed(DusterbikePhysics.approachSpeed(
                        forwardSpeed, -gear.maxSpeed(), accel));
            }
        } else if (gear.allowsForwardThrottle() && inputBackward && !inputForward && rearGrounded) {
            if (forwardSpeed > DusterbikePhysics.SPEED_EPSILON) {
                setSyncedForwardSpeed(DusterbikePhysics.applyCoastDrag(forwardSpeed, DusterbikePhysics.BRAKE_DECEL));
            } else {
                setSyncedForwardSpeed(DusterbikePhysics.applyCoastDrag(forwardSpeed, DusterbikePhysics.COAST_DRAG));
            }
        } else if (gear.allowsReverseThrottle() && inputBackward && !inputForward && rearGrounded) {
            if (forwardSpeed < -DusterbikePhysics.SPEED_EPSILON) {
                setSyncedForwardSpeed(DusterbikePhysics.applyCoastDrag(forwardSpeed, DusterbikePhysics.BRAKE_DECEL));
            } else {
                setSyncedForwardSpeed(DusterbikePhysics.applyCoastDrag(forwardSpeed, DusterbikePhysics.COAST_DRAG));
            }
        } else {
            if (!holdingForward) {
                setSyncedForwardSpeed(DusterbikePhysics.applyCoastDrag(forwardSpeed, DusterbikePhysics.COAST_DRAG));
            }
        }

        forwardSpeed = DusterbikePhysics.clampSpeedForGear(forwardSpeed, gear);
        setSyncedForwardSpeed(forwardSpeed);

        if (frontGrounded) {
            frontWheel.setAngularVelocity(DusterbikePhysics.linearSpeedToAngular(forwardSpeed));
        } else {
            frontWheel.applyAirDrag();
        }

        if (rearGrounded) {
            rearWheel.setAngularVelocity(DusterbikePhysics.linearSpeedToAngular(forwardSpeed));
        } else {
            rearWheel.applyAirDrag();
            if (gear.allowsForwardThrottle() && inputForward && !inputBackward) {
                rearWheel.setAngularVelocity(rearWheel.getAngularVelocity() - DusterbikePhysics.AIR_REAR_DRIVE_TORQUE);
            } else if (gear.allowsReverseThrottle() && inputForward && !inputBackward) {
                rearWheel.setAngularVelocity(rearWheel.getAngularVelocity() + DusterbikePhysics.AIR_REAR_DRIVE_TORQUE);
            }
        }

        frontWheel.integrateRotation();
        rearWheel.integrateRotation();

        captureWheelSpinState(frontWheel, rearWheel);

        applySteering();
        moveBodyHorizontally(frontWheel, rearWheel);
        publishDriveStateToServer();
    }

    private void refreshLocalRiderInput() {
        if (!usesClientDriveAuthority()) {
            return;
        }

        var minecraft = net.minecraft.client.Minecraft.getInstance();
        if (minecraft.player == null || minecraft.player.getVehicle() != this) {
            return;
        }

        inputForward = minecraft.options.keyUp.isDown();
        inputBackward = minecraft.options.keyDown.isDown();
        inputLeft = minecraft.options.keyLeft.isDown();
        inputRight = minecraft.options.keyRight.isDown();
        inputHandbrake = minecraft.options.keyJump.isDown();
    }

    private void applySteering() {
        float steerInput = 0.0F;
        if (inputLeft && !inputRight) {
            steerInput = -1.0F;
        } else if (inputRight && !inputLeft) {
            steerInput = 1.0F;
        }

        float absSpeed = Math.abs(forwardSpeed);
        float maxSteer = DusterbikePhysics.computeMaxSteerDegrees(absSpeed);
        if (steerInput != 0.0F) {
            setSyncedSteerAngle(DusterbikePhysics.approachSpeed(
                    steerAngle, steerInput * maxSteer, DusterbikePhysics.STEER_RATE));
        } else {
            setSyncedSteerAngle(DusterbikePhysics.approachSpeed(
                    steerAngle, 0.0F, DusterbikePhysics.STEER_RETURN_RATE));
        }

        if (absSpeed > DusterbikePhysics.SPEED_EPSILON) {
            float yawRate = DusterbikePhysics.computeYawRateDegrees(forwardSpeed, steerAngle);
            setYRot(getYRot() + yawRate);
        }
    }

    private void moveBodyHorizontally(DusterbikeWheelEntity frontWheel, DusterbikeWheelEntity rearWheel) {
        if (Math.abs(forwardSpeed) <= DusterbikePhysics.SPEED_EPSILON) {
            return;
        }

        float yawRad = getYRot() * Mth.DEG_TO_RAD;
        Vec3 delta = new Vec3(-Mth.sin(yawRad) * forwardSpeed, 0.0D, Mth.cos(yawRad) * forwardSpeed);

        double nextX = getX() + delta.x;
        double nextZ = getZ() + delta.z;
        float yaw = getYRot();

        Vec3 frontOffset = DusterbikeTransforms.rotateSteeredWheelLocalOffset(
                DusterbikeTransforms.FRONT_WHEEL_LOCAL, steerAngle, yaw);
        Vec3 rearOffset = DusterbikeTransforms.rotateLocalOffset(DusterbikeTransforms.REAR_WHEEL_LOCAL, yaw);

        boolean frontIsLead = frontOffset.dot(delta) > rearOffset.dot(delta);
        DusterbikeWheelEntity leadWheel = frontIsLead ? frontWheel : rearWheel;
        Vec3 leadOffset = frontIsLead ? frontOffset : rearOffset;
        float probeYaw = frontIsLead ? yaw + steerAngle : yaw;
        DusterbikePhysics.WheelStepResult leadProbe = DusterbikePhysics.probeStepSurface(
                level(), nextX + leadOffset.x, nextZ + leadOffset.z, leadWheel.getContactY(), probeYaw);

        if (leadProbe.blocked()) {
            setSyncedForwardSpeed(DusterbikePhysics.applyCoastDrag(forwardSpeed, DusterbikePhysics.BRAKE_DECEL));
            return;
        }

        setPos(getX() + delta.x, getY(), getZ() + delta.z);
    }

    private void updateWheelPhysics(DusterbikeWheelEntity frontWheel, DusterbikeWheelEntity rearWheel) {
        Vec3 bodyPos = position();
        float yaw = getYRot();

        Vec3 frontOffset = DusterbikeTransforms.rotateSteeredWheelLocalOffset(
                DusterbikeTransforms.FRONT_WHEEL_LOCAL, steerAngle, yaw);
        Vec3 rearOffset = DusterbikeTransforms.rotateLocalOffset(DusterbikeTransforms.REAR_WHEEL_LOCAL, yaw);

        double frontTargetX = bodyPos.x + frontOffset.x;
        double frontTargetZ = bodyPos.z + frontOffset.z;
        double rearTargetX = bodyPos.x + rearOffset.x;
        double rearTargetZ = bodyPos.z + rearOffset.z;
        double frontRestY = bodyPos.y + DusterbikeTransforms.FRONT_WHEEL_LOCAL.y;
        double rearRestY = bodyPos.y + DusterbikeTransforms.REAR_WHEEL_LOCAL.y;

        DusterbikePhysics.WheelContactResult frontResult = DusterbikePhysics.probeGround(
                level(), frontTargetX, frontTargetZ, frontWheel.getContactY(), frontRestY, yaw + steerAngle);
        DusterbikePhysics.WheelContactResult rearResult = DusterbikePhysics.probeGround(
                level(), rearTargetX, rearTargetZ, rearWheel.getContactY(), rearRestY, yaw);

        double[] resolved = DusterbikePhysics.resolveAnchoredWheelHeights(
                frontWheel.getContactY(), frontResult.contactY(), frontRestY,
                rearWheel.getContactY(), rearResult.contactY(), rearRestY);

        frontWheel.syncColliderPosition(frontTargetX, resolved[0], frontTargetZ);
        rearWheel.syncColliderPosition(rearTargetX, resolved[1], rearTargetZ);

        frontWheel.setGrounded(frontResult.grounded());
        rearWheel.setGrounded(rearResult.grounded());

        DusterbikePhysics.BodyUpdateResult pitchResult = DusterbikePhysics.computePitch(
                frontWheel.getContactY(), rearWheel.getContactY());
        setSyncedPitch(pitchResult.pitchDegrees());
    }

    private void updateBodyFromWheels(DusterbikeWheelEntity frontWheel, DusterbikeWheelEntity rearWheel) {
        double targetY = computeBodyOriginY(frontWheel.getContactY(), rearWheel.getContactY());
        setPos(getX(), targetY, getZ());
        setBoundingBox(makeBoundingBox());
    }

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
        if (tag.contains("ForwardSpeed")) {
            setSyncedForwardSpeed(tag.getFloat("ForwardSpeed"));
        } else if (tag.contains("Speed")) {
            setSyncedForwardSpeed(tag.getFloat("Speed"));
        }
        if (tag.contains("SteerAngle")) {
            setSyncedSteerAngle(tag.getFloat("SteerAngle"));
        }
        if (tag.contains("Gear")) {
            byte ordinal = tag.getByte("Gear");
            DusterbikeGear[] gears = DusterbikeGear.values();
            if (ordinal >= 0 && ordinal < gears.length) {
                setGear(gears[ordinal]);
            }
        }
        if (tag.contains("FrontWheelRotation")) {
            savedFrontWheelRotation = tag.getFloat("FrontWheelRotation");
            savedRearWheelRotation = tag.getFloat("RearWheelRotation");
            savedFrontWheelAngularVelocity = tag.getDouble("FrontWheelAngularVelocity");
            savedRearWheelAngularVelocity = tag.getDouble("RearWheelAngularVelocity");
            pendingWheelSpinRestore = true;
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        DusterbikeWheelEntity frontWheel = getFrontWheel();
        DusterbikeWheelEntity rearWheel = getRearWheel();
        if (frontWheel != null && rearWheel != null) {
            captureWheelSpinState(frontWheel, rearWheel);
        }
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
        tag.putFloat("ForwardSpeed", forwardSpeed);
        tag.putFloat("SteerAngle", steerAngle);
        tag.putByte("Gear", (byte) getGear().ordinal());
        tag.putFloat("FrontWheelRotation", savedFrontWheelRotation);
        tag.putFloat("RearWheelRotation", savedRearWheelRotation);
        tag.putDouble("FrontWheelAngularVelocity", savedFrontWheelAngularVelocity);
        tag.putDouble("RearWheelAngularVelocity", savedRearWheelAngularVelocity);
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
    protected void removePassenger(Entity passenger) {
        if (passenger instanceof LivingEntity living) {
            DusterbikeRiderAnimation.clearRider(living);
        }
        super.removePassenger(passenger);
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

        float roll;
        float pitch;
        if (this.level().isClientSide) {
            roll = getRenderRoll(1.0F);
            pitch = getRenderPitch(1.0F);
        } else {
            pitch = getSyncedPitch();
            roll = DusterbikePhysics.computeRollDegrees(
                    getDriveForwardSpeed(), steerAngle,
                    DusterbikePhysics.computeMaxSteerDegrees(Math.abs(getDriveForwardSpeed())));
        }

        Vec3 feet = DusterbikeTransforms.worldPointFromLocal(
                position(),
                getYRot(),
                pitch,
                roll,
                DusterbikeTransforms.RIDER_FEET_LOCAL);
        moveFunction.accept(passenger, feet.x, feet.y, feet.z);
    }

    @Override
    public boolean shouldRiderSit() {
        return true;
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