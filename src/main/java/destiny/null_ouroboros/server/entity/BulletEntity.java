package destiny.null_ouroboros.server.entity;

import destiny.null_ouroboros.server.registry.SoundRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.sounds.SoundSource;
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
    private static final int TRAIL_LENGTH = 50;
    private static final float RICOCHET_CHANCE = 0.6F;
    private static final float RICOCHET_SPEED_LOSS = 0.7F;

    private int age = 0;
    private final Vec3[] trailPositions = new Vec3[TRAIL_LENGTH];
    private int trailIndex = 0;
    private boolean trailFilled = false;

    public BulletEntity(EntityType<? extends BulletEntity> type, Level level) {
        super(type, level);
        this.noPhysics = false;
        this.noCulling = true;
    }

    public void shoot(double x, double y, double z, float speed) {
        Vec3 direction = new Vec3(x, y, z).normalize();
        this.setDeltaMovement(direction.scale(speed));
        // Reset trail buffer
        trailIndex = 0;
        trailFilled = false;
    }

    private void recordPosition() {
        trailPositions[trailIndex % TRAIL_LENGTH] = this.position();
        trailIndex++;
        if (trailIndex >= TRAIL_LENGTH) {
            trailFilled = true;
        }
    }

    @Override
    public void tick() {
        super.tick();

        recordPosition();

        if (level().isClientSide) {
            return;
        }

        if (++age >= MAX_AGE) {
            discard();
            return;
        }

        Vec3 motion = getDeltaMovement();
        if (motion.lengthSqr() == 0) return;

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
            return;
        }

        setPos(end.x, end.y, end.z);
    }

    private void onBlockHit(BlockHitResult hit) {
        if (random.nextFloat() < RICOCHET_CHANCE) {
            Vec3 normal = Vec3.atLowerCornerOf(hit.getDirection().getNormal());
            Vec3 velocity = getDeltaMovement();
            double dot = velocity.dot(normal);
            if (dot < 0) {
                Vec3 reflected = velocity.subtract(normal.scale(2 * dot));
                setDeltaMovement(reflected.scale(RICOCHET_SPEED_LOSS));

                setPos(hit.getLocation().add(normal.scale(0.05)));
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

    @Override
    public void lerpTo(double x, double y, double z, float yRot, float xRot, int lerpSteps, boolean teleport) {
        this.setPos(x, y, z);
        this.setRot(yRot, xRot);
    }

    @Override
    public void lerpMotion(double x, double y, double z) {
        super.lerpMotion(x, y, z);
    }

    public Vec3[] getTrailPositions() {
        if (!trailFilled && trailIndex == 0) return new Vec3[0];
        int len = Math.min(trailIndex, TRAIL_LENGTH);
        Vec3[] out = new Vec3[len];
        for (int i = 0; i < len; i++) {
            int idx = (trailIndex - 1 - i) % TRAIL_LENGTH;
            if (idx < 0) idx += TRAIL_LENGTH;
            out[i] = trailPositions[idx];
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