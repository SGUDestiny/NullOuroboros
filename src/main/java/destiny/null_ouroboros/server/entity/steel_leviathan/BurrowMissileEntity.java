package destiny.null_ouroboros.server.entity.steel_leviathan;

import destiny.null_ouroboros.server.registry.DamageTypeRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
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

    private static final double SPEED = 0.85D;
    private static final int BURROW_TICKS = 20;
    private static final float EXPLOSION_DAMAGE = 8.0F;
    private static final double EXPLOSION_RADIUS = 3.0D;

    @Nullable
    private UUID ownerUuid;
    @Nullable
    private LivingEntity cachedTarget;
    private int burrowTicks;
    private boolean exploded;

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
        this.cachedTarget = target;
        this.entityData.set(TARGET_UUID, target == null ? Optional.empty() : Optional.of(target.getUUID()));
    }

    public boolean isBurrowing() {
        return this.entityData.get(BURROWING);
    }

    @Nullable
    private LivingEntity resolveTarget() {
        if (cachedTarget != null && cachedTarget.isAlive()) {
            return cachedTarget;
        }
        Optional<UUID> uuid = this.entityData.get(TARGET_UUID);
        if (uuid.isEmpty()) {
            return null;
        }
        for (LivingEntity entity : this.level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(64.0D))) {
            if (entity.getUUID().equals(uuid.get())) {
                cachedTarget = entity;
                return entity;
            }
        }
        return null;
    }

    @Override
    public void tick() {
        super.tick();
        if (exploded) {
            return;
        }
        if (isBurrowing()) {
            burrowTicks++;
            if (!this.level().isClientSide && this.level() instanceof ServerLevel server) {
                BlockPos pos = BlockPos.containing(position());
                BlockState state = server.getBlockState(pos);
                if (!state.isAir()) {
                    server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state),
                            getX(), getY(), getZ(), 4, 0.2, 0.2, 0.2, 0.02);
                }
                setPos(getX(), getY() - 0.15D, getZ());
            }
            if (burrowTicks >= BURROW_TICKS) {
                explode();
            }
            return;
        }

        LivingEntity target = resolveTarget();
        Vec3 motion;
        if (target != null) {
            Vec3 to = target.getEyePosition().subtract(position());
            if (to.lengthSqr() > 1.0E-4D) {
                motion = to.normalize().scale(SPEED);
                float yaw = (float) (Mth.atan2(-motion.x, motion.z) * Mth.RAD_TO_DEG);
                float pitch = (float) (Mth.atan2(-motion.y, Math.sqrt(motion.x * motion.x + motion.z * motion.z)) * Mth.RAD_TO_DEG);
                setYRot(yaw);
                setXRot(pitch);
            } else {
                motion = getDeltaMovement();
            }
        } else {
            motion = getDeltaMovement().lengthSqr() < 1.0E-4D ? getLookAngle().scale(SPEED) : getDeltaMovement();
        }
        setDeltaMovement(motion);
        setPos(getX() + motion.x, getY() + motion.y, getZ() + motion.z);

        if (!this.level().isClientSide) {
            BlockPos pos = BlockPos.containing(position());
            if (!this.level().getBlockState(pos).isAir() && !this.level().getBlockState(pos).getCollisionShape(this.level(), pos).isEmpty()) {
                this.entityData.set(BURROWING, true);
                setDeltaMovement(Vec3.ZERO);
                burrowTicks = 0;
                return;
            }
            AABB box = getBoundingBox().inflate(0.35D);
            for (LivingEntity entity : this.level().getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
                entity.hurt(DamageTypeRegistry.getSimpleDamageSource(this.level(), DamageTypeRegistry.BURROW_MISSILE), EXPLOSION_DAMAGE);
                explode();
                return;
            }
        }

        if (tickCount > 20 * 20) {
            discard();
        }
    }

    private void explode() {
        if (exploded || this.level().isClientSide) {
            exploded = true;
            discard();
            return;
        }
        exploded = true;
        if (this.level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.EXPLOSION, getX(), getY(), getZ(), 8, 0.5, 0.5, 0.5, 0.05);
            AABB box = getBoundingBox().inflate(EXPLOSION_RADIUS);
            for (LivingEntity entity : server.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
                entity.hurt(DamageTypeRegistry.getSimpleDamageSource(server, DamageTypeRegistry.BURROW_MISSILE), EXPLOSION_DAMAGE);
            }
        }
        discard();
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
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (ownerUuid != null) {
            tag.putUUID("Owner", ownerUuid);
        }
        this.entityData.get(TARGET_UUID).ifPresent(uuid -> tag.putUUID("Target", uuid));
        tag.putBoolean("Burrowing", isBurrowing());
        tag.putInt("BurrowTicks", burrowTicks);
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

