package destiny.null_ouroboros.server.entity;

import destiny.null_ouroboros.server.registry.SoundRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

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

    public BulletEntity(EntityType<? extends BulletEntity> type, Level level) {
        super(type, level);
        this.noPhysics = false;
        this.noCulling = true;
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

        Vec3 start = position();
        Vec3 end = start.add(motion);

        BlockHitResult blockHit = level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        Vec3 usedEnd = (blockHit.getType() == HitResult.Type.MISS) ? end : blockHit.getLocation();

        AABB bulletBox = getBoundingBox();
        AABB movementBox = bulletBox.move(start.subtract(position()))
                .minmax(bulletBox.move(usedEnd.subtract(position())));

        List<Entity> potentialHits = level().getEntities(this, movementBox, e -> e.isPickable() && e != this);
        for (Entity target : potentialHits) {
            if (target.getBoundingBox().inflate(0.15).clip(start, usedEnd).isPresent()) {
                onEntityHit(target);
                return;
            }
        }

        if (blockHit.getType() != HitResult.Type.MISS) {
            onBlockHit(blockHit);
            updateTrail();
            return;
        }

        setPos(end.x, end.y, end.z);
        updateTrail();
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
        target.hurt(level().damageSources().generic(), 6.0F);
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
