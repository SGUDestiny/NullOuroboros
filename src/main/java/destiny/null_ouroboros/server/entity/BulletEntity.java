package destiny.null_ouroboros.server.entity;

import destiny.null_ouroboros.server.registry.DamageTypeRegistry;
import destiny.null_ouroboros.server.registry.SoundRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class BulletEntity extends Entity {
    private static final int MAX_AGE = 600;
    private static final int TRAIL_LENGTH = 64;
    private static final double TRAIL_SPACING = 0.1D;
    private static final float RICOCHET_CHANCE = 0.6F;
    private static final float RICOCHET_SPEED_LOSS = 0.7F;

    private int age = 0;
    private final Vec3[] trail = new Vec3[TRAIL_LENGTH];
    private final Vec3[] trailOld = new Vec3[TRAIL_LENGTH];
    private boolean trailInitialized;
    private Vec3 lastTrailAnchor;
    private UUID ownerUUID;
    private boolean leftOwner;

    public BulletEntity(EntityType<? extends BulletEntity> type, Level level) {
        super(type, level);
        this.noPhysics = false;
        this.noCulling = true;
    }

    public void setOwner(Entity owner) {
        this.ownerUUID = owner.getUUID();
    }

    @Nullable
    public Entity getOwner() {
        if (this.level() instanceof ServerLevel) {
            return ((ServerLevel)this.level()).getEntity(this.ownerUUID);
        } else {
            return null;
        }
    }

    public void shoot(double x, double y, double z, float speed) {
        Vec3 direction = new Vec3(x, y, z).normalize();
        this.setDeltaMovement(direction.scale(speed));
        // Reset trail buffer
        trailInitialized = false;
        lastTrailAnchor = null;
    }

    private void updateTrail() {
        Vec3 current = this.position();
        if (!trailInitialized) {
            for (int i = 0; i < TRAIL_LENGTH; i++) {
                trail[i] = Vec3.ZERO;
                trailOld[i] = Vec3.ZERO;
            }
            trailInitialized = true;
            lastTrailAnchor = current;
            return;
        }

        for (int i = 0; i < TRAIL_LENGTH; i++) {
            trailOld[i] = trail[i];
        }

        Vec3 delta = current.subtract(lastTrailAnchor);
        if (delta.lengthSqr() != 0.0D) {
            for (int i = 0; i < TRAIL_LENGTH; i++) {
                trail[i] = trail[i].subtract(delta);
            }
        }

        Vec3 prev = Vec3.ZERO;
        Vec3 fallbackDir = getDeltaMovement();
        if (fallbackDir.lengthSqr() < 1.0E-8D) {
            fallbackDir = new Vec3(0.0D, 0.0D, -1.0D);
        } else {
            fallbackDir = fallbackDir.normalize();
        }

        for (int i = 0; i < TRAIL_LENGTH; i++) {
            Vec3 point = trail[i];
            Vec3 away = point.subtract(prev);
            if (away.lengthSqr() < 1.0E-8D) {
                away = fallbackDir.scale(-1.0D);
            } else {
                away = away.normalize();
            }
            trail[i] = prev.add(away.scale(TRAIL_SPACING));
            prev = trail[i];
        }

        lastTrailAnchor = current;
    }

    @Override
    public void tick() {
        super.tick();

        Vec3 motion = getDeltaMovement();
        if (motion.lengthSqr() == 0) {
            updateTrail();
            return;
        }

        if (level().isClientSide) {
            setPos(getX() + motion.x, getY() + motion.y, getZ() + motion.z);
            updateTrail();
            return;
        }

        if (++age >= MAX_AGE) {
            discard();
            return;
        }

        updateLeftOwner();

        Vec3 start = position();
        Vec3 end = start.add(motion);

        BlockHitResult blockHit = level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        Vec3 usedEnd = (blockHit.getType() == HitResult.Type.MISS) ? end : blockHit.getLocation();

        AABB bulletBox = getBoundingBox();
        AABB movementBox = bulletBox.move(start.subtract(position()))
                .minmax(bulletBox.move(usedEnd.subtract(position())));

        double halfX = bulletBox.getXsize() * 0.5D;
        double halfY = bulletBox.getYsize() * 0.5D;
        double halfZ = bulletBox.getZsize() * 0.5D;
        Vec3 delta = usedEnd.subtract(start);
        Vec3 startCenter = bulletBox.getCenter();
        Vec3 endCenter = startCenter.add(delta);

        List<Entity> potentialHits = level().getEntities(this, movementBox, this::canHitEntity);
        Entity closestHit = null;
        double closestDist = Double.MAX_VALUE;
        for (Entity target : potentialHits) {
            AABB expanded = target.getBoundingBox().inflate(halfX, halfY, halfZ);
            double dist;
            if (expanded.contains(startCenter)) {
                dist = 0.0D;
            } else {
                Optional<Vec3> hit = expanded.clip(startCenter, endCenter);
                if (hit.isEmpty()) {
                    continue;
                }
                dist = startCenter.distanceToSqr(hit.get());
            }
            if (dist < closestDist) {
                closestDist = dist;
                closestHit = target;
            }
        }
        if (closestHit != null) {
            onEntityHit(closestHit);
            return;
        }

        if (blockHit.getType() != HitResult.Type.MISS) {
            onBlockHit(blockHit);
            updateLeftOwner();
            updateTrail();
            return;
        }

        setPos(end.x, end.y, end.z);
        updateLeftOwner();
        updateTrail();
    }

    private void updateLeftOwner() {
        if (this.leftOwner || this.ownerUUID == null) {
            return;
        }
        Entity owner = getOwner();
        if (owner == null || !getBoundingBox().intersects(owner.getBoundingBox())) {
            this.leftOwner = true;
        }
    }

    private boolean canHitEntity(Entity entity) {
        if (!entity.isPickable() || entity == this) {
            return false;
        }
        if (!this.leftOwner && this.ownerUUID != null && this.ownerUUID.equals(entity.getUUID())) {
            return false;
        }
        return true;
    }

    private void onBlockHit(BlockHitResult hit) {
        if (random.nextFloat() < RICOCHET_CHANCE) {
            Vec3 normal = Vec3.atLowerCornerOf(hit.getDirection().getNormal());
            Vec3 velocity = getDeltaMovement();
            double dot = velocity.dot(normal);
            if (dot < 0) {
                Vec3 reflected = velocity.subtract(normal.scale(2 * dot));
                setDeltaMovement(reflected.scale(RICOCHET_SPEED_LOSS));
                this.hasImpulse = true;
                setPos(hit.getLocation().add(normal.scale(0.2)));
                playRicochetSound();
                return;
            }
        }
        playHitSound();

        discard();
    }

    private void onEntityHit(Entity target) {
        Entity shooter = getOwner();
        DamageSource damageSource;

        if (shooter != null) {
            damageSource = DamageTypeRegistry.getAttributedDamageSource(level(), DamageTypeRegistry.BULLET, this, shooter);
        } else {
            damageSource = DamageTypeRegistry.getSimpleDamageSource(level(), DamageTypeRegistry.BULLET);
        }

        target.hurt(damageSource, 6.0F);
        playHitSound();
        discard();
    }

    private void playRicochetSound() {
        level().playSound(null, this.blockPosition(),
                SoundRegistry.BULLET_RICOCHET.get(),
                SoundSource.PLAYERS, 8f, 1f);
    }

    private void playHitSound() {
        level().playSound(null, this.blockPosition(),
                SoundRegistry.BULLET_HIT.get(),
                SoundSource.PLAYERS, 8f, 1f);
    }

    public Vec3[] getTrailPositions(float partialTicks) {
        if (!trailInitialized) {
            return new Vec3[0];
        }
        Vec3[] out = new Vec3[TRAIL_LENGTH];
        for (int i = 0; i < TRAIL_LENGTH; i++) {
            Vec3 oldPos = trailOld[i];
            Vec3 newPos = trail[i];
            out[i] = new Vec3(
                    Mth.lerp(partialTicks, oldPos.x, newPos.x),
                    Mth.lerp(partialTicks, oldPos.y, newPos.y),
                    Mth.lerp(partialTicks, oldPos.z, newPos.z)
            );
        }
        return out;
    }

    @Override
    protected void defineSynchedData() {}

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        age = tag.getInt("Age");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Age", age);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return new ClientboundAddEntityPacket(this);
    }

    @Override
    public boolean shouldRender(double p_20296_, double p_20297_, double p_20298_) {
        return true;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double p_19883_) {
        return true;
    }
}
