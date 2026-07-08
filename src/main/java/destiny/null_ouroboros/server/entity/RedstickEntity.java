package destiny.null_ouroboros.server.entity;

import destiny.null_ouroboros.common.light.RedstickLightManager;
import destiny.null_ouroboros.server.registry.EntityRegistry;
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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Optional;
import java.util.UUID;

public class RedstickEntity extends Entity {
    private static final EntityDataAccessor<Integer> TOP_END_ID =
            SynchedEntityData.defineId(RedstickEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> BOTTOM_END_ID =
            SynchedEntityData.defineId(RedstickEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Optional<UUID>> TOP_END_UUID =
            SynchedEntityData.defineId(RedstickEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Optional<UUID>> BOTTOM_END_UUID =
            SynchedEntityData.defineId(RedstickEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Float> AXIS_X =
            SynchedEntityData.defineId(RedstickEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> AXIS_Y =
            SynchedEntityData.defineId(RedstickEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> AXIS_Z =
            SynchedEntityData.defineId(RedstickEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> RESTING =
            SynchedEntityData.defineId(RedstickEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> GLOW_AGE =
            SynchedEntityData.defineId(RedstickEntity.class, EntityDataSerializers.INT);

    public static final int GLOW_FADE_TICKS = 20 * 60 * 5;
    private static final int NO_END = -1;
    private static final int MISSING_END_GRACE_TICKS = 100;
    private static final int PHYSICS_SUBSTEPS = 4;

    private boolean discarding;
    private UUID savedTopUuid;
    private UUID savedBottomUuid;
    private int missingEndTicks;
    private Vec3 renderAxis = new Vec3(0.0D, 1.0D, 0.0D);
    private Vec3 previousRenderAxis = new Vec3(0.0D, 1.0D, 0.0D);
    private static final double SURFACE_CLEARANCE = 0.075D;
    private static final double THROW_SPIN_SPEED = 0.35D;
    private static final double REST_VELOCITY_SQR = 4.0E-6D;
    private static final double AIR_DAMPING = 0.992D;
    private static final double GROUND_DAMPING = 0.55D;
    private static final double LEDGE_PIVOT_DAMPING = 0.92D;
    private static final double MAX_SPEED = 2.0D;

    public RedstickEntity(EntityType<? extends RedstickEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(TOP_END_ID, NO_END);
        this.entityData.define(BOTTOM_END_ID, NO_END);
        this.entityData.define(TOP_END_UUID, Optional.empty());
        this.entityData.define(BOTTOM_END_UUID, Optional.empty());
        this.entityData.define(AXIS_X, 0.0F);
        this.entityData.define(AXIS_Y, 1.0F);
        this.entityData.define(AXIS_Z, 0.0F);
        this.entityData.define(RESTING, false);
        this.entityData.define(GLOW_AGE, 0);
    }

    public boolean isAtRest() {
        return this.entityData.get(RESTING);
    }

    private void setAtRest(boolean atRest) {
        this.entityData.set(RESTING, atRest);
    }

    public boolean isBurnedOut() {
        return getGlowAge() >= GLOW_FADE_TICKS;
    }

    public int getGlowAge() {
        return this.entityData.get(GLOW_AGE);
    }

    public float getGlowBrightness(float partialTick) {
        float age = Math.min(GLOW_FADE_TICKS, getGlowAge() + (this.level().isClientSide && !isBurnedOut() ? partialTick : 0.0F));
        return Math.max(0.0F, (GLOW_FADE_TICKS - age) / (float) GLOW_FADE_TICKS);
    }

    public int getBlockLightLevel() {
        if (isBurnedOut()) return 0;
        return (int) Math.ceil(RedstickLightManager.LIGHT_LEVEL * getGlowBrightness(0.0F));
    }

    public int getTopEndId() {
        return this.entityData.get(TOP_END_ID);
    }

    public int getBottomEndId() {
        return this.entityData.get(BOTTOM_END_ID);
    }

    public void setEndIds(int topId, int bottomId) {
        this.entityData.set(TOP_END_ID, topId);
        this.entityData.set(BOTTOM_END_ID, bottomId);
    }

    public void setEndRefs(RedstickEndEntity topEnd, RedstickEndEntity bottomEnd) {
        setEndIds(topEnd.getId(), bottomEnd.getId());
        this.entityData.set(TOP_END_UUID, Optional.of(topEnd.getUUID()));
        this.entityData.set(BOTTOM_END_UUID, Optional.of(bottomEnd.getUUID()));
        this.savedTopUuid = topEnd.getUUID();
        this.savedBottomUuid = bottomEnd.getUUID();
    }

    public RedstickEndEntity getTopEnd() {
        return getEndEntity(getTopEndId(), this.entityData.get(TOP_END_UUID).orElse(savedTopUuid));
    }

    public RedstickEndEntity getBottomEnd() {
        return getEndEntity(getBottomEndId(), this.entityData.get(BOTTOM_END_UUID).orElse(savedBottomUuid));
    }

    private RedstickEndEntity getEndEntity(int id, UUID uuid) {
        if (id != NO_END) {
            Entity entity = this.level().getEntity(id);
            if (entity instanceof RedstickEndEntity end) return end;
        }

        if (uuid == null) return null;

        for (RedstickEndEntity end : this.level().getEntitiesOfClass(RedstickEndEntity.class, this.getBoundingBox().inflate(16.0D))) {
            if (end.getUUID().equals(uuid)) return end;
        }

        return null;
    }

    public Vec3 getRenderAxis(float partialTick) {
        Vec3 from = previousRenderAxis;
        Vec3 to = renderAxis;
        if (from.lengthSqr() < 1.0E-6D || to.lengthSqr() < 1.0E-6D) {
            return to.lengthSqr() < 1.0E-6D ? new Vec3(0.0D, 1.0D, 0.0D) : to.normalize();
        }

        Vector3f start = new Vector3f((float) from.x, (float) from.y, (float) from.z).normalize();
        Vector3f end = new Vector3f((float) to.x, (float) to.y, (float) to.z).normalize();
        if (start.dot(end) > 0.9999F) {
            return to;
        }

        Quaternionf rotation = new Quaternionf().rotationTo(start, end);
        Vector3f result = new Vector3f(start);
        result.rotate(new Quaternionf().slerp(rotation, partialTick));
        return new Vec3(result.x, result.y, result.z);
    }

    private Vec3 getSyncedAxis() {
        Vec3 axis = new Vec3(this.entityData.get(AXIS_X), this.entityData.get(AXIS_Y), this.entityData.get(AXIS_Z));
        return axis.lengthSqr() < 1.0E-6D ? new Vec3(0.0D, 1.0D, 0.0D) : axis.normalize();
    }

    private void setSyncedAxis(Vec3 axis) {
        Vec3 normalized = axis.lengthSqr() < 1.0E-6D ? new Vec3(0.0D, 1.0D, 0.0D) : axis.normalize();
        this.entityData.set(AXIS_X, (float) normalized.x);
        this.entityData.set(AXIS_Y, (float) normalized.y);
        this.entityData.set(AXIS_Z, (float) normalized.z);
    }

    public static RedstickEntity spawnThrown(Level level, Vec3 midpoint, Vec3 direction, Vec3 velocity, double spinSpeed) {
        Vec3 travel = direction.lengthSqr() > 1.0E-6D ? direction.normalize() : new Vec3(0.0D, 0.0D, 1.0D);
        Vec3 axis = travel.cross(new Vec3(0.0D, 1.0D, 0.0D));

        if (axis.lengthSqr() < 1.0E-4D) {
            axis = travel.cross(new Vec3(1.0D, 0.0D, 0.0D));
        }

        return spawnAlongAxis(level, midpoint, axis.normalize(), velocity, spinSpeed);
    }

    private static RedstickEntity spawnAlongAxis(Level level, Vec3 midpoint, Vec3 axis, Vec3 linearVelocity, double spinSpeed) {
        Vec3 halfAxis = axis.normalize().scale(RedstickPhysics.STICK_LENGTH * 0.5D);
        Vec3 bottomPos = midpoint.subtract(halfAxis);
        Vec3 topPos = midpoint.add(halfAxis);

        RedstickEntity parent = EntityRegistry.REDSTICK.get().create(level);
        if (parent == null) return null;

        parent.setPos(midpoint);
        parent.setSyncedAxis(axis);

        RedstickEndEntity bottomEnd = new RedstickEndEntity(
                EntityRegistry.REDSTICK_END.get(), level, parent.getId(), parent.getUUID(),
                RedstickEndEntity.EndType.BOTTOM, bottomPos.x, bottomPos.y, bottomPos.z);
        RedstickEndEntity topEnd = new RedstickEndEntity(
                EntityRegistry.REDSTICK_END.get(), level, parent.getId(), parent.getUUID(),
                RedstickEndEntity.EndType.TOP, topPos.x, topPos.y, topPos.z);

        applyInitialMomentum(topEnd, bottomEnd, axis.normalize(), linearVelocity, spinSpeed);
        parent.setEndRefs(topEnd, bottomEnd);
        parent.setAtRest(false);

        level.addFreshEntity(bottomEnd);
        level.addFreshEntity(topEnd);
        parent.updateParentFromEnds();
        level.addFreshEntity(parent);

        return parent;
    }

    private static void applyInitialMomentum(RedstickEndEntity topEnd, RedstickEndEntity bottomEnd, Vec3 axis, Vec3 linearVelocity, double spinSpeed) {
        Vec3 spinAxis = axis.cross(linearVelocity.lengthSqr() > 1.0E-6D ? linearVelocity.normalize() : new Vec3(0.0D, 1.0D, 0.0D));

        if (spinAxis.lengthSqr() < 1.0E-4D) {
            spinAxis = axis.cross(new Vec3(1.0D, 0.0D, 0.0D));
        }
        if (spinAxis.lengthSqr() < 1.0E-4D) {
            spinAxis = axis.cross(new Vec3(0.0D, 0.0D, 1.0D));
        }

        Vec3 angularVelocity = spinAxis.normalize().scale(spinSpeed);
        Vec3 topRotationalVelocity = angularVelocity.cross(axis.scale(RedstickPhysics.STICK_LENGTH * 0.5D));
        Vec3 bottomRotationalVelocity = angularVelocity.cross(axis.scale(-RedstickPhysics.STICK_LENGTH * 0.5D));

        topEnd.setDeltaMovement(linearVelocity.add(topRotationalVelocity));
        bottomEnd.setDeltaMovement(linearVelocity.add(bottomRotationalVelocity));
    }

    @Override
    public void tick() {
        super.tick();

        previousRenderAxis = renderAxis;
        renderAxis = getSyncedAxis();

        if (this.level().isClientSide) {
            updateParentFromEnds();
            RedstickLightManager.update(this);
            return;
        }

        tickGlowAge();
        relinkEndsIfNeeded();

        RedstickEndEntity topEnd = getTopEnd();
        RedstickEndEntity bottomEnd = getBottomEnd();
        if (topEnd == null || bottomEnd == null) {
            missingEndTicks++;
            if (missingEndTicks > MISSING_END_GRACE_TICKS) {
                discardWithEnds();
            }
            return;
        }

        missingEndTicks = 0;

        wakeIfSupportLost(topEnd, bottomEnd);

        Vec3 topStart = topEnd.position();
        Vec3 bottomStart = bottomEnd.position();

        Vec3 topTickVelocity = topEnd.getDeltaMovement();
        Vec3 bottomTickVelocity = bottomEnd.getDeltaMovement();

        for (int step = 0; step < PHYSICS_SUBSTEPS; step++) {
            applyGravityAndMoveSubstep(topEnd, topTickVelocity);
            applyGravityAndMoveSubstep(bottomEnd, bottomTickVelocity);
            RedstickPhysics.enforceStickLength(topEnd, bottomEnd);
        }

        topEnd.resolvePenetration();
        bottomEnd.resolvePenetration();

        updateMomentumFromPositions(topEnd, bottomEnd, topStart);
        updateMomentumFromPositions(bottomEnd, topEnd, bottomStart);
        RedstickPhysics.resolveFlatEdgeWedging(topEnd, bottomEnd);
        settleIfResting(topEnd, bottomEnd);

        updateParentFromEnds();
    }

    private void tickGlowAge() {
        if (getGlowAge() < GLOW_FADE_TICKS) {
            this.entityData.set(GLOW_AGE, getGlowAge() + 1);
        }
    }

    private void applyGravityAndMoveSubstep(RedstickEndEntity end, Vec3 tickVelocity) {
        Vec3 step = tickVelocity.scale(1.0D / PHYSICS_SUBSTEPS);
        if (!end.isNoGravity()) {
            step = step.add(0.0D, RedstickPhysics.GRAVITY / PHYSICS_SUBSTEPS, 0.0D);
        }

        end.moveWithCollision(step);
    }

    private void relinkEndsIfNeeded() {
        if (getTopEnd() != null && getBottomEnd() != null) return;
        UUID topUuid = this.entityData.get(TOP_END_UUID).orElse(savedTopUuid);
        UUID bottomUuid = this.entityData.get(BOTTOM_END_UUID).orElse(savedBottomUuid);
        if (topUuid == null && bottomUuid == null) return;

        int topId = getTopEndId();
        int bottomId = getBottomEndId();

        for (RedstickEndEntity end : this.level().getEntitiesOfClass(RedstickEndEntity.class, this.getBoundingBox().inflate(16.0D))) {
            if (topUuid != null && end.getUUID().equals(topUuid)) {
                topId = end.getId();
            }
            if (bottomUuid != null && end.getUUID().equals(bottomUuid)) {
                bottomId = end.getId();
            }
        }

        if (topId != NO_END || bottomId != NO_END) {
            setEndIds(topId == NO_END ? getTopEndId() : topId, bottomId == NO_END ? getBottomEndId() : bottomId);
        }

        if (getTopEnd() != null && getBottomEnd() != null) {
            missingEndTicks = 0;
        }
    }

    private void updateMomentumFromPositions(RedstickEndEntity end, RedstickEndEntity partnerEnd, Vec3 previousPosition) {
        Vec3 velocity = end.position().subtract(previousPosition);

        boolean fullSupport = RedstickPhysics.hasFootprintSupport(end);
        boolean partialSupport = RedstickPhysics.hasPartialFootprintSupport(end);
        boolean partnerFullSupport = RedstickPhysics.hasFootprintSupport(partnerEnd);

        if (fullSupport && partnerFullSupport) {
            velocity = applyGroundFriction(velocity, GROUND_DAMPING);
        } else if (fullSupport) {
            velocity = applyGroundFriction(velocity, GROUND_DAMPING);
        } else if (partialSupport) {
            velocity = new Vec3(
                    velocity.x * LEDGE_PIVOT_DAMPING,
                    Math.min(velocity.y, RedstickPhysics.GRAVITY),
                    velocity.z * LEDGE_PIVOT_DAMPING);
        } else {
            velocity = velocity.scale(AIR_DAMPING);
            if (end.horizontalCollision) {
                velocity = new Vec3(velocity.x * 0.85D, velocity.y, velocity.z * 0.85D);
            }
        }

        if (velocity.lengthSqr() < 1.0E-8D) {
            velocity = Vec3.ZERO;
        }

        double maxSpeedSqr = MAX_SPEED * MAX_SPEED;
        if (velocity.lengthSqr() > maxSpeedSqr) {
            velocity = velocity.normalize().scale(MAX_SPEED);
        }

        end.setDeltaMovement(velocity);
    }

    private static Vec3 applyGroundFriction(Vec3 velocity, double horizontalFriction) {
        return new Vec3(
                velocity.x * horizontalFriction,
                Math.max(0.0D, velocity.y),
                velocity.z * horizontalFriction);
    }

    private void wakeIfSupportLost(RedstickEndEntity topEnd, RedstickEndEntity bottomEnd) {
        if (!isAtRest()) {
            return;
        }

        if (!RedstickPhysics.hasFootprintSupport(topEnd) || !RedstickPhysics.hasFootprintSupport(bottomEnd)) {
            setAtRest(false);
        }
    }

    private void settleIfResting(RedstickEndEntity topEnd, RedstickEndEntity bottomEnd) {
        if (!RedstickPhysics.hasFootprintSupport(topEnd) || !RedstickPhysics.hasFootprintSupport(bottomEnd)) {
            setAtRest(false);
            return;
        }

        if (topEnd.getDeltaMovement().horizontalDistanceSqr() > REST_VELOCITY_SQR
                || bottomEnd.getDeltaMovement().horizontalDistanceSqr() > REST_VELOCITY_SQR) {
            setAtRest(false);
            return;
        }

        if (topEnd.getDeltaMovement().lengthSqr() < REST_VELOCITY_SQR && bottomEnd.getDeltaMovement().lengthSqr() < REST_VELOCITY_SQR) {
            topEnd.setDeltaMovement(Vec3.ZERO);
            bottomEnd.setDeltaMovement(Vec3.ZERO);
            RedstickPhysics.snapToStickLength(topEnd, bottomEnd);
            setAtRest(true);
        }
    }

    public void updateParentFromEnds() {
        RedstickEndEntity topEnd = getTopEnd();
        RedstickEndEntity bottomEnd = getBottomEnd();
        if (topEnd == null || bottomEnd == null) return;

        Vec3 topPos = topEnd.position();
        Vec3 bottomPos = bottomEnd.position();
        Vec3 midpoint = topPos.add(bottomPos).scale(0.5D);
        setSyncedAxis(topPos.subtract(bottomPos));

        Vec3 delta = midpoint.subtract(this.position());
        this.setDeltaMovement(delta);
        if (delta.lengthSqr() > 0.0D) {
            this.move(MoverType.SELF, delta);
        } else if (this.position().distanceToSqr(midpoint) > 1.0E-8D) {
            this.setPos(midpoint);
        }

        AABB topBox = topEnd.getBoundingBox();
        AABB bottomBox = bottomEnd.getBoundingBox();
        this.setBoundingBox(topBox.minmax(bottomBox));
    }

    public void onEndRemoved(RedstickEndEntity end) {
        if (discarding) return;
        discardWithEnds();
    }

    @Override
    public void onAddedToWorld() {
        super.onAddedToWorld();
        RedstickLightManager.register(this);
    }

    @Override
    public void onRemovedFromWorld() {
        RedstickLightManager.unregister(this);
        super.onRemovedFromWorld();
    }

    public void discardWithEnds() {
        if (discarding) return;
        discarding = true;

        RedstickEndEntity topEnd = getTopEnd();
        RedstickEndEntity bottomEnd = getBottomEnd();
        if (topEnd != null && topEnd.isAlive()) topEnd.discard();
        if (bottomEnd != null && bottomEnd.isAlive()) bottomEnd.discard();
        this.discard();
    }

    @Override
    public void remove(RemovalReason reason) {
        RedstickLightManager.unregister(this);
        if (!this.level().isClientSide && !discarding && reason.shouldDestroy()) {
            discarding = true;
            RedstickEndEntity topEnd = getTopEnd();
            RedstickEndEntity bottomEnd = getBottomEnd();
            if (topEnd != null && topEnd.isAlive()) topEnd.discard();
            if (bottomEnd != null && bottomEnd.isAlive()) bottomEnd.discard();
        }
        super.remove(reason);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.contains("TopEnd")) {
            this.entityData.set(TOP_END_ID, tag.getInt("TopEnd"));
        }
        if (tag.contains("BottomEnd")) {
            this.entityData.set(BOTTOM_END_ID, tag.getInt("BottomEnd"));
        }
        if (tag.hasUUID("TopEndUuid")) {
            savedTopUuid = tag.getUUID("TopEndUuid");
            this.entityData.set(TOP_END_UUID, Optional.of(savedTopUuid));
        }
        if (tag.hasUUID("BottomEndUuid")) {
            savedBottomUuid = tag.getUUID("BottomEndUuid");
            this.entityData.set(BOTTOM_END_UUID, Optional.of(savedBottomUuid));
        }
        if (tag.contains("GlowAge")) {
            this.entityData.set(GLOW_AGE, Math.min(tag.getInt("GlowAge"), GLOW_FADE_TICKS));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        RedstickEndEntity topEnd = getTopEnd();
        RedstickEndEntity bottomEnd = getBottomEnd();
        if (topEnd != null) {
            tag.putInt("TopEnd", topEnd.getId());
            tag.putUUID("TopEndUuid", topEnd.getUUID());
        } else if (savedTopUuid != null) {
            tag.putUUID("TopEndUuid", savedTopUuid);
        }
        if (bottomEnd != null) {
            tag.putInt("BottomEnd", bottomEnd.getId());
            tag.putUUID("BottomEndUuid", bottomEnd.getUUID());
        } else if (savedBottomUuid != null) {
            tag.putUUID("BottomEndUuid", savedBottomUuid);
        }
        tag.putInt("GlowAge", getGlowAge());
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.isInvulnerableTo(source) || source.is(DamageTypeTags.IS_FALL)) {
            return false;
        }

        if (!this.level().isClientSide) {
            discardWithEnds();
        }

        return true;
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
