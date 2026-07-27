package destiny.null_ouroboros.server.entity;

import destiny.null_ouroboros.NullOuroboros;
import destiny.null_ouroboros.common.revolver.RevolverCartridge;
import destiny.null_ouroboros.server.registry.DamageTypeRegistry;
import destiny.null_ouroboros.server.registry.SoundRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class BulletEntity extends Entity {
    private static final int MAX_AGE = 600;
    private static final int TRAIL_LENGTH = 64;
    private static final double TRAIL_SPACING = 0.1D;
    private static final float RICOCHET_CHANCE = 0.2F;
    private static final float RICOCHET_SPEED_LOSS = 0.7F;
    private static final int OWNER_HIT_IMMUNITY_TICKS = 20;
    private static final EntityDataAccessor<Byte> CARTRIDGE =
            SynchedEntityData.defineId(BulletEntity.class, EntityDataSerializers.BYTE);
    private static final net.minecraft.tags.TagKey<net.minecraft.world.level.block.Block> BULLET_SHATTER =
            BlockTags.create(ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, "bullet_shatter"));

    private int age = 0;
    private final Vec3[] trail = new Vec3[TRAIL_LENGTH];
    private final Vec3[] trailOld = new Vec3[TRAIL_LENGTH];
    private boolean trailInitialized;
    private Vec3 lastTrailAnchor;
    private UUID ownerUUID;
    @Nullable
    private Entity cachedOwner;
    private boolean hasRicocheted;
    private boolean opGuaranteedPenetration = true;
    private int entityPenetrations;
    private final Set<UUID> hitEntities = new HashSet<>();

    public BulletEntity(EntityType<? extends BulletEntity> type, Level level) {
        super(type, level);
        this.noPhysics = false;
        this.noCulling = true;
    }

    public void setOwner(Entity owner) {
        this.ownerUUID = owner.getUUID();
        this.cachedOwner = owner;
    }

    public void setCartridge(RevolverCartridge cartridge) {
        entityData.set(CARTRIDGE, (byte) cartridge.ordinal());
        entityPenetrations = cartridge == RevolverCartridge.AP ? 1 : 0;
        opGuaranteedPenetration = true;
    }

    public RevolverCartridge getCartridge() {
        return RevolverCartridge.byOrdinal(entityData.get(CARTRIDGE));
    }

    @Nullable
    public Entity getOwner() {
        if (this.cachedOwner != null && !this.cachedOwner.isRemoved()) {
            return this.cachedOwner;
        }
        if (this.ownerUUID != null && this.level() instanceof ServerLevel serverLevel) {
            Entity entity = serverLevel.getEntity(this.ownerUUID);
            if (entity == null) {
                Player player = serverLevel.getPlayerByUUID(this.ownerUUID);
                if (player != null) {
                    entity = player;
                }
            }
            this.cachedOwner = entity;
            return entity;
        }
        return null;
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

        double halfX = bulletBox.getXsize() * 0.5D;
        double halfY = bulletBox.getYsize() * 0.5D;
        double halfZ = bulletBox.getZsize() * 0.5D;
        Vec3 delta = usedEnd.subtract(start);
        Vec3 startCenter = bulletBox.getCenter();
        Vec3 endCenter = startCenter.add(delta);

        List<Entity> potentialHits = level().getEntities(this, movementBox, this::canHitEntity);
        Entity closestHit = null;
        Vec3 closestHitLocation = null;
        double closestDist = Double.MAX_VALUE;
        for (Entity target : potentialHits) {
            AABB expanded = target.getBoundingBox().inflate(halfX, halfY, halfZ);
            double dist;
            Vec3 hitLocation;
            if (expanded.contains(startCenter)) {
                dist = 0.0D;
                hitLocation = startCenter;
            } else {
                Optional<Vec3> hit = expanded.clip(startCenter, endCenter);
                if (hit.isEmpty()) {
                    continue;
                }
                hitLocation = hit.get();
                dist = startCenter.distanceToSqr(hitLocation);
            }
            if (dist < closestDist) {
                closestDist = dist;
                closestHit = target;
                closestHitLocation = hitLocation;
            }
        }
        if (closestHit != null) {
            if (onEntityHit(closestHit)) {
                setPos(closestHitLocation.add(motion.normalize().scale(0.1D)));
                updateTrail();
            }
            return;
        }

        if (blockHit.getType() != HitResult.Type.MISS) {
            onBlockHit(blockHit);
            updateTrail();
            return;
        }

        setPos(end.x, end.y, end.z);
        updateTrail();
    }

    private boolean canHitEntity(Entity entity) {
        if (!entity.isPickable() || entity == this || hitEntities.contains(entity.getUUID())) {
            return false;
        }
        if (this.ownerUUID != null && this.ownerUUID.equals(entity.getUUID())
                && !this.hasRicocheted && this.age <= OWNER_HIT_IMMUNITY_TICKS) {
            return false;
        }
        return true;
    }

    private void onBlockHit(BlockHitResult hit) {
        BlockState state = level().getBlockState(hit.getBlockPos());
        if (state.is(BULLET_SHATTER)) {
            level().destroyBlock(hit.getBlockPos(), true, getOwner());
            if (getCartridge() == RevolverCartridge.IC) {
                igniteBlock(hit.getBlockPos().relative(hit.getDirection()));
            }
            setPos(hit.getLocation().add(getDeltaMovement().normalize().scale(0.1D)));
            playHitSound();
            return;
        }
        if (getCartridge() == RevolverCartridge.IC) {
            igniteBlock(hit.getBlockPos().relative(hit.getDirection()));
        }
        if (random.nextFloat() < RICOCHET_CHANCE) {
            Vec3 normal = Vec3.atLowerCornerOf(hit.getDirection().getNormal());
            Vec3 velocity = getDeltaMovement();
            double dot = velocity.dot(normal);
            if (dot < 0) {
                Vec3 reflected = velocity.subtract(normal.scale(2 * dot));
                setDeltaMovement(reflected.scale(RICOCHET_SPEED_LOSS));
                this.hasImpulse = true;
                this.hasRicocheted = true;
                setPos(hit.getLocation().add(normal.scale(0.2)));
                playRicochetSound();
                return;
            }
        }
        playHitSound();

        discard();
    }

    private boolean onEntityHit(Entity target) {
        Entity shooter = getOwner();
        DamageSource damageSource;

        if (shooter != null) {
            damageSource = DamageTypeRegistry.getAttributedDamageSource(level(), DamageTypeRegistry.BULLET, this, shooter);
        } else {
            damageSource = DamageTypeRegistry.getSimpleDamageSource(level(), DamageTypeRegistry.BULLET);
        }

        RevolverCartridge cartridge = getCartridge();
        float damage = switch (cartridge) {
            case HP -> hpDamage(target);
            case AP -> hasArmor(target) ? 8.0F : 4.0F;
            case IC -> 4.0F;
            case OP -> 16.0F;
            default -> 0.0F;
        };
        target.hurt(damageSource, damage);
        if (cartridge == RevolverCartridge.IC) {
            target.setSecondsOnFire(5);
        }
        hitEntities.add(target.getUUID());
        playHitSound();
        if (shouldPenetrateEntity(cartridge)) {
            return true;
        }
        discard();
        return false;
    }

    private float hpDamage(Entity target) {
        if (!hasArmor(target)) {
            return 8.0F;
        }
        LivingEntity living = (LivingEntity) target;
        return 8.0F * living.getMaxHealth() / living.getArmorValue();
    }

    private boolean hasArmor(Entity target) {
        return target instanceof LivingEntity living && living.getArmorValue() > 0;
    }

    private boolean shouldPenetrateEntity(RevolverCartridge cartridge) {
        if (cartridge == RevolverCartridge.AP && entityPenetrations > 0) {
            entityPenetrations--;
            return true;
        }
        if (cartridge == RevolverCartridge.OP) {
            if (opGuaranteedPenetration) {
                opGuaranteedPenetration = false;
                return true;
            }
            return random.nextFloat() < 0.2F;
        }
        return false;
    }

    private void igniteBlock(BlockPos position) {
        if (!level().isEmptyBlock(position)) {
            return;
        }
        BlockState fire = Blocks.FIRE.defaultBlockState();
        if (fire.canSurvive(level(), position)) {
            level().setBlock(position, fire, 3);
        }
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
    protected void defineSynchedData() {
        entityData.define(CARTRIDGE, (byte) RevolverCartridge.HP.ordinal());
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        age = tag.getInt("Age");
        hasRicocheted = tag.getBoolean("HasRicocheted");
        entityData.set(CARTRIDGE, tag.getByte("Cartridge"));
        entityPenetrations = tag.getInt("EntityPenetrations");
        opGuaranteedPenetration = tag.getBoolean("OpGuaranteedPenetration");
        if (tag.hasUUID("Owner")) {
            ownerUUID = tag.getUUID("Owner");
            cachedOwner = null;
        }
        hitEntities.clear();
        ListTag hitEntityTags = tag.getList("HitEntities", 10);
        for (int i = 0; i < hitEntityTags.size(); i++) {
            hitEntities.add(hitEntityTags.getCompound(i).getUUID("Id"));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Age", age);
        tag.putBoolean("HasRicocheted", hasRicocheted);
        tag.putByte("Cartridge", entityData.get(CARTRIDGE));
        tag.putInt("EntityPenetrations", entityPenetrations);
        tag.putBoolean("OpGuaranteedPenetration", opGuaranteedPenetration);
        if (ownerUUID != null) {
            tag.putUUID("Owner", ownerUUID);
        }
        ListTag hitEntityTags = new ListTag();
        for (UUID hitEntity : hitEntities) {
            CompoundTag hitEntityTag = new CompoundTag();
            hitEntityTag.putUUID("Id", hitEntity);
            hitEntityTags.add(hitEntityTag);
        }
        tag.put("HitEntities", hitEntityTags);
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
