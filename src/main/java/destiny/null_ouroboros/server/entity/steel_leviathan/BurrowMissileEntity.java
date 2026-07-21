package destiny.null_ouroboros.server.entity.steel_leviathan;

import destiny.null_ouroboros.common.steel_leviathan.BurrowMissileColliders;
import destiny.null_ouroboros.server.registry.DamageTypeRegistry;
import destiny.null_ouroboros.server.registry.EntityRegistry;
import destiny.null_ouroboros.server.registry.SoundRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.UUID;

public class BurrowMissileEntity extends Entity implements GeoAnimatable {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private static final EntityDataAccessor<Optional<UUID>> TARGET_UUID =
            SynchedEntityData.defineId(BurrowMissileEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Boolean> BURROWING =
            SynchedEntityData.defineId(BurrowMissileEntity.class, EntityDataSerializers.BOOLEAN);

    private static final double SPEED = 0.7D;
    private static final float TURN_RATE = 6F;
    private static final float TURN_RATE_CLOSE = 18F;
    private static final double TURN_BOOST_RANGE = 10.0D;
    private static final double COMMIT_RANGE = 4.0D;
    private static final float LOCK_ANGLE = 2.0F;
    private static final int FUSE_TICKS = 200;
    private static final int BURROW_TICKS = 20;
    private static final double BURROW_STEP = 0.15D;
    private static final float BURROW_DRILL_SPIN = 0.35F;
    private static final float MAX_HEALTH = 9.0F;
    private static final float EXPLOSION_DAMAGE = 8.0F;
    private static final double EXPLOSION_RADIUS = 3.0D;
    private static final double TARGET_SCAN_RANGE = 48.0D;

    @Nullable
    private UUID ownerUuid;
    @Nullable
    private LivingEntity cachedTarget;
    private int burrowTicks;
    private boolean exploded;
    private boolean tracking = true;
    private float health = MAX_HEALTH;
    private Vec3 burrowDir = new Vec3(0.0D, -1.0D, 0.0D);
    private AABB drillBoundingBox = new AABB(0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D);
    @Nullable
    private BurrowMissileDrillHitboxEntity drillHitbox;

    private float drillSpinAngle;

    public BurrowMissileEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(TARGET_UUID, Optional.empty());
        this.entityData.define(BURROWING, false);
    }

    public void setOwner(SteelLeviathanHeadEntity owner) {
        this.ownerUuid = owner.getUUID();
    }

    public void setTarget(@Nullable LivingEntity target) {
        if (!isValidMissileTarget(target)) {
            target = null;
        }
        this.cachedTarget = target;
        this.entityData.set(TARGET_UUID, target == null ? Optional.empty() : Optional.of(target.getUUID()));
    }

    private static boolean isValidMissileTarget(@Nullable LivingEntity target) {
        if (target == null || !target.isAlive()) {
            return false;
        }
        return !(target instanceof Player player) || !player.isSpectator();
    }

    public void launchInDirection(Vec3 dir) {
        Vec3 n = dir.lengthSqr() < 1.0E-8D ? new Vec3(0.0D, 0.0D, 1.0D) : dir.normalize();
        float yaw = (float) (Mth.atan2(-n.x, n.z) * Mth.RAD_TO_DEG);
        float pitch = (float) (Mth.atan2(-n.y, Math.sqrt(n.x * n.x + n.z * n.z)) * Mth.RAD_TO_DEG);
        setYRot(yaw);
        setXRot(pitch);
        yRotO = yaw;
        xRotO = pitch;
        tracking = true;
        setDeltaMovement(getLookAngle().scale(SPEED));
        refreshColliders();
    }

    public boolean isBurrowing() {
        return this.entityData.get(BURROWING);
    }

    public AABB getDrillBoundingBox() {
        return drillBoundingBox;
    }

    @Override
    protected AABB makeBoundingBox() {
        return BurrowMissileColliders.bodyColliderBox(position(), getYRot(), getXRot());
    }

    private void refreshColliders() {
        Vec3 origin = position();
        float yaw = getYRot();
        float pitch = getXRot();
        setBoundingBox(BurrowMissileColliders.bodyColliderBox(origin, yaw, pitch));
        drillBoundingBox = BurrowMissileColliders.drillColliderBox(origin, yaw, pitch);
        if (!this.level().isClientSide) {
            ensureDrillHitbox();
            if (drillHitbox != null && !drillHitbox.isRemoved()) {
                Vec3 drillCenter = BurrowMissileColliders.worldPointFromLocal(
                        origin, yaw, pitch, BurrowMissileColliders.DRILL_CENTER_LOCAL);
                drillHitbox.syncColliderPosition(drillCenter.x, drillCenter.y, drillCenter.z);
                drillHitbox.syncOrientation(yaw, pitch);
                drillBoundingBox = drillHitbox.getBoundingBox();
            }
        }
    }

    private void ensureDrillHitbox() {
        if (this.level().isClientSide) {
            return;
        }
        if (drillHitbox != null && !drillHitbox.isRemoved()) {
            return;
        }
        Vec3 drillCenter = BurrowMissileColliders.worldPointFromLocal(
                position(), getYRot(), getXRot(), BurrowMissileColliders.DRILL_CENTER_LOCAL);
        BurrowMissileDrillHitboxEntity hitbox =
                new BurrowMissileDrillHitboxEntity(EntityRegistry.BURROW_MISSILE_DRILL.get(), this.level());
        hitbox.init(this, drillCenter.x, drillCenter.y, drillCenter.z);
        this.level().addFreshEntity(hitbox);
        drillHitbox = hitbox;
    }

    private void discardDrillHitbox() {
        if (drillHitbox != null && !drillHitbox.isRemoved()) {
            drillHitbox.discard();
        }
        drillHitbox = null;
    }

    @Nullable
    private LivingEntity resolveTarget() {
        if (isValidMissileTarget(cachedTarget)) {
            return cachedTarget;
        }
        cachedTarget = null;
        Optional<UUID> uuid = this.entityData.get(TARGET_UUID);
        if (uuid.isEmpty()) {
            return null;
        }
        for (LivingEntity entity : this.level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(64.0D))) {
            if (entity.getUUID().equals(uuid.get()) && isValidMissileTarget(entity)) {
                cachedTarget = entity;
                return entity;
            }
        }
        this.entityData.set(TARGET_UUID, Optional.empty());
        return null;
    }

    private void acquireNearbyTarget() {
        AABB scan = getBoundingBox().inflate(TARGET_SCAN_RANGE);
        LivingEntity bestPlayer = null;
        LivingEntity bestOther = null;
        double bestPlayerDist = Double.MAX_VALUE;
        double bestOtherDist = Double.MAX_VALUE;
        for (LivingEntity entity : this.level().getEntitiesOfClass(LivingEntity.class, scan, BurrowMissileEntity::isValidMissileTarget)) {
            double dist = entity.distanceToSqr(this);
            if (entity instanceof Player) {
                if (dist < bestPlayerDist) {
                    bestPlayerDist = dist;
                    bestPlayer = entity;
                }
            } else if (dist < bestOtherDist) {
                bestOtherDist = dist;
                bestOther = entity;
            }
        }
        LivingEntity chosen = bestPlayer != null ? bestPlayer : bestOther;
        if (chosen != null) {
            setTarget(chosen);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (exploded) {
            return;
        }
        if (this.level().isClientSide) {
            if (isBurrowing()) {
                drillSpinAngle += BURROW_DRILL_SPIN;
            }
            refreshColliders();
            return;
        }
        if (isBurrowing()) {
            if (this.level() instanceof ServerLevel server) {
                burrowTicks++;
                spawnBurrowFaceParticles(server);
                Vec3 next = position().add(burrowDir.scale(BURROW_STEP));
                setPos(next.x, next.y, next.z);
                refreshColliders();
                if (burrowTicks >= BURROW_TICKS) {
                    explode();
                }
            }
            return;
        }

        if (tracking) {
            LivingEntity target = resolveTarget();
            if (target == null) {
                acquireNearbyTarget();
                target = resolveTarget();
            }
            if (target != null) {
                Vec3 aim = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
                Vec3 to = aim.subtract(position());
                double dist = Math.sqrt(to.lengthSqr());
                if (to.lengthSqr() > 1.0E-4D) {
                    float desiredYaw = (float) (Mth.atan2(-to.x, to.z) * Mth.RAD_TO_DEG);
                    float desiredPitch = (float) (Mth.atan2(-to.y, Math.sqrt(to.x * to.x + to.z * to.z)) * Mth.RAD_TO_DEG);
                    float yawErr = Math.abs(Mth.wrapDegrees(desiredYaw - getYRot()));
                    float pitchErr = Math.abs(Mth.wrapDegrees(desiredPitch - getXRot()));
                    if (dist <= COMMIT_RANGE || (yawErr <= LOCK_ANGLE && pitchErr <= LOCK_ANGLE)) {
                        setYRot(desiredYaw);
                        setXRot(desiredPitch);
                        tracking = false;
                    } else {
                        float turnRate = TURN_RATE;
                        if (dist < TURN_BOOST_RANGE) {
                            float t = 1.0F - (float) (dist / TURN_BOOST_RANGE);
                            turnRate = Mth.lerp(t, TURN_RATE, TURN_RATE_CLOSE);
                        }
                        setYRot(Mth.approachDegrees(getYRot(), desiredYaw, turnRate));
                        setXRot(Mth.approachDegrees(getXRot(), desiredPitch, turnRate));
                    }
                }
            }
        }

        Vec3 from = position();
        Vec3 motion = getLookAngle().scale(SPEED);
        setDeltaMovement(motion);
        Vec3 to = from.add(motion);
        setPos(to.x, to.y, to.z);
        refreshColliders();

        if (tryBeginBurrow(from, to)) {
            return;
        }
        if (hitsLivingTarget()) {
            explode();
            return;
        }
        if (tickCount >= FUSE_TICKS) {
            explode();
        }
    }

    private boolean hitsLivingTarget() {
        AABB body = getBoundingBox();
        AABB drill = drillBoundingBox;
        AABB search = body.minmax(drill);
        for (LivingEntity entity : this.level().getEntitiesOfClass(LivingEntity.class, search, BurrowMissileEntity::isValidMissileTarget)) {
            AABB targetBox = entity.getBoundingBox();
            if (body.intersects(targetBox) || drill.intersects(targetBox)) {
                return true;
            }
        }
        return false;
    }

    private boolean tryBeginBurrow(Vec3 from, Vec3 to) {
        BlockHitResult bodyHit = clipSolid(from, to);
        Vec3 drillFrom = BurrowMissileColliders.worldPointFromLocal(from, getYRot(), getXRot(), BurrowMissileColliders.DRILL_CENTER_LOCAL);
        Vec3 drillTo = BurrowMissileColliders.worldPointFromLocal(to, getYRot(), getXRot(), BurrowMissileColliders.DRILL_CENTER_LOCAL);
        BlockHitResult drillHit = clipSolid(drillFrom, drillTo);

        BlockHitResult hit = null;
        if (bodyHit.getType() == HitResult.Type.BLOCK && drillHit.getType() == HitResult.Type.BLOCK) {
            hit = from.distanceToSqr(bodyHit.getLocation()) <= from.distanceToSqr(drillHit.getLocation())
                    ? bodyHit
                    : drillHit;
        } else if (bodyHit.getType() == HitResult.Type.BLOCK) {
            hit = bodyHit;
        } else if (drillHit.getType() == HitResult.Type.BLOCK) {
            hit = drillHit;
        }

        if (hit != null) {
            Vec3 loc = hit.getLocation();
            setPos(loc.x, loc.y, loc.z);
            beginBurrow();
            return true;
        }

        if (aabbTouchesSolid(getBoundingBox()) || aabbTouchesSolid(drillBoundingBox)) {
            beginBurrow();
            return true;
        }
        return false;
    }

    private BlockHitResult clipSolid(Vec3 from, Vec3 to) {
        return this.level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
    }

    private boolean aabbTouchesSolid(AABB box) {
        int minX = Mth.floor(box.minX);
        int minY = Mth.floor(box.minY);
        int minZ = Mth.floor(box.minZ);
        int maxX = Mth.floor(box.maxX);
        int maxY = Mth.floor(box.maxY);
        int maxZ = Mth.floor(box.maxZ);
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    cursor.set(x, y, z);
                    BlockState state = this.level().getBlockState(cursor);
                    if (!state.isAir() && !state.getCollisionShape(this.level(), cursor).isEmpty()) {
                        AABB shape = state.getCollisionShape(this.level(), cursor).bounds();
                        if (shape.move(cursor).intersects(box)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private void beginBurrow() {
        this.entityData.set(BURROWING, true);
        Vec3 look = getLookAngle();
        burrowDir = look.lengthSqr() < 1.0E-8D ? new Vec3(0.0D, -1.0D, 0.0D) : look.normalize();
        setDeltaMovement(Vec3.ZERO);
        burrowTicks = 0;
        playSound(SoundRegistry.BURROW_BEACON_DRILL.get(), 1.0F, 1.0F);
        refreshColliders();
    }

    private void spawnBurrowFaceParticles(ServerLevel server) {
        Vec3 from = position();
        Vec3 to = from.add(burrowDir.scale(0.5D));
        BlockHitResult hit = clipSolid(from, to);
        BlockPos pos;
        Direction face;
        Vec3 at;
        if (hit.getType() == HitResult.Type.BLOCK) {
            pos = hit.getBlockPos();
            face = hit.getDirection();
            at = hit.getLocation();
        } else {
            pos = BlockPos.containing(from.add(burrowDir.scale(0.25D)));
            BlockState probe = server.getBlockState(pos);
            if (probe.isAir() || probe.getCollisionShape(server, pos).isEmpty()) {
                return;
            }
            face = Direction.getNearest((float) -burrowDir.x, (float) -burrowDir.y, (float) -burrowDir.z);
            at = Vec3.atCenterOf(pos).add(face.getStepX() * 0.51D, face.getStepY() * 0.51D, face.getStepZ() * 0.51D);
        }
        BlockState state = server.getBlockState(pos);
        if (state.isAir() || state.getCollisionShape(server, pos).isEmpty()) {
            return;
        }
        Vec3 normal = Vec3.atLowerCornerOf(face.getNormal());
        Vec3 tangentA;
        Vec3 tangentB;
        if (face.getAxis() == Direction.Axis.Y) {
            tangentA = new Vec3(1.0D, 0.0D, 0.0D);
            tangentB = new Vec3(0.0D, 0.0D, 1.0D);
        } else {
            tangentA = new Vec3(0.0D, 1.0D, 0.0D);
            tangentB = normal.cross(tangentA);
            if (tangentB.lengthSqr() < 1.0E-6D) {
                tangentB = new Vec3(1.0D, 0.0D, 0.0D);
            } else {
                tangentB = tangentB.normalize();
            }
        }
        BlockParticleOption option = new BlockParticleOption(ParticleTypes.BLOCK, state);
        for (int i = 0; i < 12; i++) {
            double ja = (this.random.nextDouble() - 0.5D) * 1.1D;
            double jb = (this.random.nextDouble() - 0.5D) * 1.1D;
            double jn = 0.02D + this.random.nextDouble() * 0.1D;
            Vec3 p = at.add(tangentA.scale(ja)).add(tangentB.scale(jb)).add(normal.scale(jn));
            server.sendParticles(option, p.x, p.y, p.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }

    public float getDrillSpinAngle() {
        return drillSpinAngle;
    }

    @Override
    public boolean isPickable() {
        return !exploded && !isRemoved();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.level().isClientSide || exploded || isBurrowing() || amount <= 0.0F || isInvulnerableTo(source)) {
            return false;
        }
        if (source.is(DamageTypeRegistry.BURROW_MISSILE)) {
            return false;
        }
        Entity attacker = source.getEntity();
        if (attacker != null && ownerUuid != null && ownerUuid.equals(attacker.getUUID())) {
            return false;
        }
        health -= amount;
        if (health <= 0.0F) {
            explode();
        }
        return true;
    }

    private void explode() {
        if (exploded || this.level().isClientSide) {
            exploded = true;
            discardDrillHitbox();
            discard();
            return;
        }
        exploded = true;
        if (this.level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.EXPLOSION, getX(), getY(), getZ(), 8, 0.5, 0.5, 0.5, 0.05);
            float pitch = 0.9F + this.random.nextFloat() * 0.2F;
            server.playSound(null, getX(), getY(), getZ(), SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 1.0F, pitch);
            AABB box = getBoundingBox().minmax(drillBoundingBox).inflate(EXPLOSION_RADIUS);
            for (LivingEntity entity : server.getEntitiesOfClass(LivingEntity.class, box, BurrowMissileEntity::isValidMissileTarget)) {
                entity.hurt(DamageTypeRegistry.getSimpleDamageSource(server, DamageTypeRegistry.BURROW_MISSILE), EXPLOSION_DAMAGE);
            }
        }
        discardDrillHitbox();
        discard();
    }

    @Override
    public void remove(RemovalReason reason) {
        discardDrillHitbox();
        super.remove(reason);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("Owner")) {
            ownerUuid = tag.getUUID("Owner");
        }
        if (tag.hasUUID("Target")) {
            this.entityData.set(TARGET_UUID, Optional.of(tag.getUUID("Target")));
        }
        this.entityData.set(BURROWING, tag.getBoolean("Burrowing"));
        burrowTicks = tag.getInt("BurrowTicks");
        tracking = !tag.contains("Tracking") || tag.getBoolean("Tracking");
        health = tag.contains("Health") ? tag.getFloat("Health") : MAX_HEALTH;
        if (tag.contains("BurrowDX")) {
            burrowDir = new Vec3(tag.getDouble("BurrowDX"), tag.getDouble("BurrowDY"), tag.getDouble("BurrowDZ"));
            if (burrowDir.lengthSqr() < 1.0E-8D) {
                burrowDir = new Vec3(0.0D, -1.0D, 0.0D);
            } else {
                burrowDir = burrowDir.normalize();
            }
        }
        refreshColliders();
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (ownerUuid != null) {
            tag.putUUID("Owner", ownerUuid);
        }
        this.entityData.get(TARGET_UUID).ifPresent(uuid -> tag.putUUID("Target", uuid));
        tag.putBoolean("Burrowing", isBurrowing());
        tag.putInt("BurrowTicks", burrowTicks);
        tag.putBoolean("Tracking", tracking);
        tag.putFloat("Health", health);
        tag.putDouble("BurrowDX", burrowDir.x);
        tag.putDouble("BurrowDY", burrowDir.y);
        tag.putDouble("BurrowDZ", burrowDir.z);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public double getTick(Object animatable) {
        return this.tickCount;
    }
}
