package destiny.null_ouroboros.server.entity.steel_leviathan;

import destiny.null_ouroboros.common.steel_leviathan.BurrowMissileColliders;
import destiny.null_ouroboros.common.steel_leviathan.SteelLeviathanBones;
import destiny.null_ouroboros.common.steel_leviathan.SteelLeviathanConstants;
import destiny.null_ouroboros.common.steel_leviathan.SteelLeviathanModelBones;
import destiny.null_ouroboros.common.steel_leviathan.SteelLeviathanSinew;
import destiny.null_ouroboros.server.entity.ParentLinkedHitboxEntity;
import destiny.null_ouroboros.server.registry.DamageTypeRegistry;
import destiny.null_ouroboros.server.registry.EntityRegistry;
import destiny.null_ouroboros.server.registry.ParticleTypeRegistry;
import destiny.null_ouroboros.server.registry.SoundRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public abstract class SteelLeviathanPartEntity extends Entity implements GeoAnimatable {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private static final EntityDataAccessor<Integer> HEAD_ID =
            SynchedEntityData.defineId(SteelLeviathanPartEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Optional<UUID>> HEAD_UUID =
            SynchedEntityData.defineId(SteelLeviathanPartEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Integer> PREV_ID =
            SynchedEntityData.defineId(SteelLeviathanPartEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Optional<UUID>> PREV_UUID =
            SynchedEntityData.defineId(SteelLeviathanPartEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Integer> NEXT_ID =
            SynchedEntityData.defineId(SteelLeviathanPartEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Optional<UUID>> NEXT_UUID =
            SynchedEntityData.defineId(SteelLeviathanPartEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Integer> CHAIN_INDEX =
            SynchedEntityData.defineId(SteelLeviathanPartEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> VULNERABLE =
            SynchedEntityData.defineId(SteelLeviathanPartEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Byte> HEATSINK_COUNT =
            SynchedEntityData.defineId(SteelLeviathanPartEntity.class, EntityDataSerializers.BYTE);

    private static final EntityDataAccessor<Byte> HEATSINK_PRESENT_MASK =
            SynchedEntityData.defineId(SteelLeviathanPartEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Byte> HEATSINK_DESTROYED_MASK =
            SynchedEntityData.defineId(SteelLeviathanPartEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Boolean> HEATSINKS_OPEN =
            SynchedEntityData.defineId(SteelLeviathanPartEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> THRUSTERS_ACTIVE =
            SynchedEntityData.defineId(SteelLeviathanPartEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> BODY_YAW =
            SynchedEntityData.defineId(SteelLeviathanPartEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> BODY_PITCH =
            SynchedEntityData.defineId(SteelLeviathanPartEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> UNDERGROUND =
            SynchedEntityData.defineId(SteelLeviathanPartEntity.class, EntityDataSerializers.BOOLEAN);

    private static final EntityDataAccessor<Integer> ARMOR_SHED_TICKS =
            SynchedEntityData.defineId(SteelLeviathanPartEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Byte> MISSILE_RELEASED_MASK =
            SynchedEntityData.defineId(SteelLeviathanPartEntity.class, EntityDataSerializers.BYTE);

    private static final int NO_LINK = -1;
    private static final int MISSING_HEAD_GRACE = 120;
    private static final int NO_CHUNK_HINT = Integer.MAX_VALUE;

    private int missingHeadTicks;
    private int contactCooldown;
    private final SteelLeviathanHeatsinkHitboxEntity[] heatsinkEntities =
            new SteelLeviathanHeatsinkHitboxEntity[SteelLeviathanConstants.MAX_HEATSINKS];
    protected byte missilePendingMask;
    protected int missileLaunchCooldown;
    protected UUID savedHeadUuid;
    protected UUID savedPrevUuid;
    protected UUID savedNextUuid;

    private int hintHeadChunkX = NO_CHUNK_HINT;
    private int hintHeadChunkZ;
    private int hintPrevChunkX = NO_CHUNK_HINT;
    private int hintPrevChunkZ;
    private int hintNextChunkX = NO_CHUNK_HINT;
    private int hintNextChunkZ;

    private float bodyYawO;
    private float bodyPitchO;

    private float clientBodyYaw;
    private float clientBodyPitch;
    private boolean clientLookInitialized;

    private int lastLookSyncTick = Integer.MIN_VALUE;

    private float bodyGearSpinAngle;
    private float mawGearSpinAngle;
    private float drillSpinAngle;
    private double lastSpinX;
    private double lastSpinY;
    private double lastSpinZ;
    private boolean spinPosInitialized;

    protected SteelLeviathanPartEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(HEAD_ID, NO_LINK);
        this.entityData.define(HEAD_UUID, Optional.empty());
        this.entityData.define(PREV_ID, NO_LINK);
        this.entityData.define(PREV_UUID, Optional.empty());
        this.entityData.define(NEXT_ID, NO_LINK);
        this.entityData.define(NEXT_UUID, Optional.empty());
        this.entityData.define(CHAIN_INDEX, 0);
        this.entityData.define(VULNERABLE, false);
        this.entityData.define(HEATSINK_COUNT, (byte) 0);
        this.entityData.define(HEATSINK_PRESENT_MASK, (byte) 0);
        this.entityData.define(HEATSINK_DESTROYED_MASK, (byte) 0);
        this.entityData.define(HEATSINKS_OPEN, false);
        this.entityData.define(THRUSTERS_ACTIVE, false);
        this.entityData.define(BODY_YAW, 0.0F);
        this.entityData.define(BODY_PITCH, 0.0F);
        this.entityData.define(UNDERGROUND, false);
        this.entityData.define(ARMOR_SHED_TICKS, 0);
        this.entityData.define(MISSILE_RELEASED_MASK, (byte) 0);
    }

    public abstract PartKind getPartKind();

    public enum PartKind {
        HEAD, SEGMENT, TAIL
    }

    public int getHeadId() {
        return this.entityData.get(HEAD_ID);
    }

    public void setHeadRef(SteelLeviathanHeadEntity head) {
        this.entityData.set(HEAD_ID, head.getId());
        this.entityData.set(HEAD_UUID, Optional.of(head.getUUID()));
        this.savedHeadUuid = head.getUUID();
        hintHeadChunkX = head.chunkPosition().x;
        hintHeadChunkZ = head.chunkPosition().z;
    }

    public Optional<UUID> getHeadUuid() {
        return this.entityData.get(HEAD_UUID);
    }

    public void setPrevRef(@Nullable SteelLeviathanPartEntity prev) {
        if (prev == null) {
            this.entityData.set(PREV_ID, NO_LINK);
            this.entityData.set(PREV_UUID, Optional.empty());
            this.savedPrevUuid = null;
            return;
        }
        this.entityData.set(PREV_ID, prev.getId());
        this.entityData.set(PREV_UUID, Optional.of(prev.getUUID()));
        this.savedPrevUuid = prev.getUUID();
    }

    public void setNextRef(@Nullable SteelLeviathanPartEntity next) {
        if (next == null) {
            this.entityData.set(NEXT_ID, NO_LINK);
            this.entityData.set(NEXT_UUID, Optional.empty());
            this.savedNextUuid = null;
            return;
        }
        this.entityData.set(NEXT_ID, next.getId());
        this.entityData.set(NEXT_UUID, Optional.of(next.getUUID()));
        this.savedNextUuid = next.getUUID();
    }

    public int getChainIndex() {
        return this.entityData.get(CHAIN_INDEX);
    }

    public void setChainIndex(int index) {
        this.entityData.set(CHAIN_INDEX, index);
    }

    public boolean isVulnerable() {
        return this.entityData.get(VULNERABLE);
    }

    public void setVulnerable(boolean vulnerable) {
        this.entityData.set(VULNERABLE, vulnerable);
    }

    public int getHeatsinkCount() {
        return this.entityData.get(HEATSINK_COUNT) & 0xFF;
    }

    public void setHeatsinkCount(int count) {
        this.entityData.set(HEATSINK_COUNT, (byte) Mth.clamp(count, 0, SteelLeviathanConstants.MAX_HEATSINKS));
    }

    public byte getHeatsinkPresentMask() {
        return this.entityData.get(HEATSINK_PRESENT_MASK);
    }

    public void setHeatsinkPresentMask(byte mask) {
        this.entityData.set(HEATSINK_PRESENT_MASK, (byte) (mask & 0x0F));
    }

    public boolean isHeatsinkPresent(int index) {
        if (index < 0 || index >= SteelLeviathanConstants.MAX_HEATSINKS) {
            return false;
        }
        byte present = getHeatsinkPresentMask();
        if (present == 0) {

            return index < getHeatsinkCount();
        }
        return (present & (1 << index)) != 0;
    }

    public boolean isHeatsinkSlotActive(int index) {
        return isHeatsinkPresent(index) && !isHeatsinkDestroyed(index) && !isVulnerable();
    }

    public byte getHeatsinkDestroyedMask() {
        return this.entityData.get(HEATSINK_DESTROYED_MASK);
    }

    public boolean isHeatsinkDestroyed(int index) {
        return (getHeatsinkDestroyedMask() & (1 << index)) != 0;
    }

    public void setHeatsinkDestroyed(int index, boolean destroyed) {
        if (!isHeatsinkPresent(index)) {
            return;
        }
        byte mask = getHeatsinkDestroyedMask();
        if (destroyed) {
            mask = (byte) (mask | (1 << index));
        } else {
            mask = (byte) (mask & ~(1 << index));
        }
        this.entityData.set(HEATSINK_DESTROYED_MASK, mask);
        if (destroyed && areAllHeatsinksDestroyed()) {
            beginArmorShed();
        } else if (!destroyed && isArmorShedding() && !areAllHeatsinksDestroyed()) {
            setArmorShedTicks(0);
        }
    }

    public int getArmorShedTicks() {
        return this.entityData.get(ARMOR_SHED_TICKS);
    }

    public void setArmorShedTicks(int ticks) {
        this.entityData.set(ARMOR_SHED_TICKS, Math.max(0, ticks));
    }

    public boolean isArmorShedding() {
        return getArmorShedTicks() > 0;
    }

    public float getArmorShedHeat(float partialTick) {
        int ticks = getArmorShedTicks();
        if (ticks <= 0) {
            return 0.0F;
        }
        float duration = SteelLeviathanConstants.ARMOR_SHED_TELEGRAPH_TICKS;
        float remaining = ticks - partialTick;
        return Mth.clamp(1.0F - remaining / duration, 0.0F, 1.0F);
    }

    public boolean areAllHeatsinksDestroyed() {
        boolean any = false;
        for (int i = 0; i < SteelLeviathanConstants.MAX_HEATSINKS; i++) {
            if (!isHeatsinkPresent(i)) {
                continue;
            }
            any = true;
            if (!isHeatsinkDestroyed(i)) {
                return false;
            }
        }
        return any;
    }

    public boolean areHeatsinksOpen() {
        return this.entityData.get(HEATSINKS_OPEN);
    }

    public void setHeatsinksOpen(boolean open) {
        this.entityData.set(HEATSINKS_OPEN, open);
    }

    public boolean areThrustersActive() {

        SteelLeviathanHeadEntity head = this instanceof SteelLeviathanHeadEntity self
                ? self
                : resolveHead();
        if (head != null && head.isPhaseTwo()) {
            return true;
        }
        return this.entityData.get(THRUSTERS_ACTIVE);
    }

    public void setThrustersActive(boolean active) {
        this.entityData.set(THRUSTERS_ACTIVE, active);
    }

    public float getBodyYaw() {
        return this.entityData.get(BODY_YAW);
    }

    public float getBodyYawO() {
        return this.bodyYawO;
    }

    public float getBodyPitch() {
        return this.entityData.get(BODY_PITCH);
    }

    public float getBodyPitchO() {
        return this.bodyPitchO;
    }

    protected void setBodyYawO(float yaw) {
        this.bodyYawO = yaw;
    }

    protected void setBodyPitchO(float pitch) {
        this.bodyPitchO = pitch;
    }

    public void setLookRotation(float yaw, float pitch) {
        if (!this.level().isClientSide) {
            if (Float.compare(getYRot(), yaw) != 0) {
                super.setYRot(yaw);
            }
            writeLookSynched(yaw, pitch);
            return;
        }
        super.setYRot(yaw);
        this.entityData.set(BODY_YAW, yaw);
        this.entityData.set(BODY_PITCH, pitch);
    }

    public void setBodyPitch(float pitch) {
        setLookRotation(getYRot(), pitch);
    }

    @Override
    public void setYRot(float yaw) {
        if (!this.level().isClientSide) {
            setLookRotation(yaw, getBodyPitch());
        } else {
            super.setYRot(yaw);
        }
    }

    private void writeLookSynched(float yaw, float pitch) {
        if (Float.compare(this.entityData.get(BODY_YAW), yaw) == 0
                && Float.compare(this.entityData.get(BODY_PITCH), pitch) == 0) {
            return;
        }

        this.entityData.set(BODY_YAW, yaw);
        this.entityData.set(BODY_PITCH, pitch);
    }

    public Vec3 getBackConnectionWorld() {
        if (getPartKind() == PartKind.HEAD) {

            return position();
        }
        return localToWorld(new Vec3(0.0D, 0.0D, -SteelLeviathanConstants.SEGMENT_SPACING));
    }

    public boolean isUnderground() {
        return this.entityData.get(UNDERGROUND);
    }

    public void setUnderground(boolean underground) {
        this.entityData.set(UNDERGROUND, underground);
        this.noPhysics = true;
    }

    public void rollHeatsinks(net.minecraft.util.RandomSource random) {
        int count = 1 + random.nextInt(SteelLeviathanConstants.MAX_HEATSINKS);
        int[] slots = new int[SteelLeviathanConstants.MAX_HEATSINKS];
        for (int i = 0; i < slots.length; i++) {
            slots[i] = i;
        }
        for (int i = slots.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            int tmp = slots[i];
            slots[i] = slots[j];
            slots[j] = tmp;
        }
        byte present = 0;
        for (int i = 0; i < count; i++) {
            present |= (byte) (1 << slots[i]);
        }
        setHeatsinkPresentMask(present);
        setHeatsinkCount(count);
        this.entityData.set(HEATSINK_DESTROYED_MASK, (byte) 0);
        setArmorShedTicks(0);
        setVulnerable(false);
        clearMissileReleaseState();
    }

    public void beginArmorShed() {
        if (isVulnerable() || isArmorShedding()) {
            return;
        }
        setHeatsinksOpen(false);
        setArmorShedTicks(SteelLeviathanConstants.ARMOR_SHED_TELEGRAPH_TICKS);
        playSound(SoundRegistry.STEEL_LEVIATHAN_SEGMENT_OVERHEAT.get(), SteelLeviathanConstants.SOUND_VOLUME_64, 1.0F);
    }

    private void tickArmorShed() {
        int ticks = getArmorShedTicks();
        if (ticks <= 0) {
            return;
        }
        int next = ticks - 1;
        setArmorShedTicks(next);
        if (next <= 0) {
            finishArmorShed();
        }
    }

    private void finishArmorShed() {
        if (isVulnerable()) {
            return;
        }
        spawnArmorShedExplosion();
        applyArmorShedDamage();
        setVulnerable(true);
        setHeatsinksOpen(false);
        if (getPartKind() == PartKind.TAIL && missilePendingMask == 0 && getMissileReleasedMask() == 0) {
            beginTailMissileRelease();
        }
    }

    private void spawnArmorShedExplosion() {
        if (!(this.level() instanceof ServerLevel server)) {
            return;
        }
        server.sendParticles(ParticleTypes.EXPLOSION_EMITTER, getX(), getY(), getZ(), 1, 0.0, 0.0, 0.0, 0.0);
        server.sendParticles(ParticleTypes.EXPLOSION, getX(), getY(), getZ(), 8, 1.2, 1.2, 1.2, 0.0);
        for (int i = 0; i < 40; i++) {
            double vx = (random.nextDouble() - 0.5) * 0.8;
            double vy = random.nextDouble() * 0.6;
            double vz = (random.nextDouble() - 0.5) * 0.8;
            server.sendParticles(ParticleTypeRegistry.BLOOD.get(),
                    getX(), getY(), getZ(),
                    0, vx, vy, vz, 1.0);
        }
    }

    private void applyArmorShedDamage() {
        SteelLeviathanHeadEntity head = this instanceof SteelLeviathanHeadEntity self ? self : resolveHead();
        if (head == null) {
            return;
        }
        float damage = SteelLeviathanConstants.MAX_HEALTH * SteelLeviathanConstants.ARMOR_SHED_DAMAGE_FRACTION;
        float next = head.getMainHealth() - damage;
        head.setMainHealth(next);
        if (next <= 0.0F) {
            head.startDeathPublic(head.getCombatTarget());
        }
    }

    public void restoreArmor() {
        setVulnerable(false);
        setArmorShedTicks(0);
        this.entityData.set(HEATSINK_DESTROYED_MASK, (byte) 0);
        clearMissileReleaseState();
        for (int i = 0; i < SteelLeviathanConstants.MAX_HEATSINKS; i++) {
            if (isHeatsinkPresent(i) && heatsinkEntities[i] != null) {
                heatsinkEntities[i].resetHealth();
            }
        }
    }

    public byte getMissileReleasedMask() {
        return this.entityData.get(MISSILE_RELEASED_MASK);
    }

    public void setMissileReleasedMask(byte mask) {
        this.entityData.set(MISSILE_RELEASED_MASK, mask);
    }

    public boolean isMissileSlotReleased(int index) {
        return (getMissileReleasedMask() & (1 << index)) != 0;
    }

    protected void clearMissileReleaseState() {
        setMissileReleasedMask((byte) 0);
        missilePendingMask = 0;
        missileLaunchCooldown = 0;
    }

    protected void beginTailMissileRelease() {
        missilePendingMask = (byte) ((1 << SteelLeviathanModelBones.TAIL_MISSILE_COUNT) - 1);
        missileLaunchCooldown = 0;
    }

    protected void tickMissileRelease() {
        if (this.level().isClientSide || missilePendingMask == 0) {
            return;
        }
        if (missileLaunchCooldown > 0) {
            missileLaunchCooldown--;
            return;
        }
        if (getPartKind() == PartKind.TAIL) {
            tickTailMissileRelease();
        }
    }

    private void tickTailMissileRelease() {
        SteelLeviathanHeadEntity head = resolveHead();
        if (head == null) {
            return;
        }
        List<Integer> clear = new ArrayList<>();
        for (int i = 0; i < SteelLeviathanModelBones.TAIL_MISSILE_COUNT; i++) {
            if ((missilePendingMask & (1 << i)) == 0) {
                continue;
            }
            Vec3 pos = localToWorld(SteelLeviathanModelBones.tailMissileLocal(i));
            if (isMissileSpawnClear(pos) && this.level().canSeeSky(BlockPos.containing(pos))) {
                clear.add(i);
            }
        }
        if (clear.isEmpty()) {
            return;
        }
        int slot = clear.get(this.random.nextInt(clear.size()));
        spawnTailMissile(head, slot);
        missilePendingMask &= (byte) ~(1 << slot);
        setMissileReleasedMask((byte) (getMissileReleasedMask() | (1 << slot)));
        missileLaunchCooldown = SteelLeviathanConstants.MISSILE_LAUNCH_INTERVAL;
    }

    private void spawnTailMissile(SteelLeviathanHeadEntity head, int slot) {
        Vec3 pos = localToWorld(SteelLeviathanModelBones.tailMissileLocal(slot));
        BurrowMissileEntity missile = new BurrowMissileEntity(EntityRegistry.BURROW_MISSILE.get(), this.level());
        missile.setPos(pos.x, pos.y, pos.z);
        missile.setOwner(head);
        missile.setTarget(head.getCombatTarget());
        Vec3 facing = SteelLeviathanSinew.facingFromYawPitch(getYRot(), getBodyPitch());
        if (facing.lengthSqr() < 1.0E-6D) {
            facing = new Vec3(0.0D, 0.0D, 1.0D);
        }
        missile.launchInDirection(facing.scale(-1.0D));
        this.level().addFreshEntity(missile);
        this.level().playSound(null, pos.x, pos.y, pos.z,
                SoundRegistry.STEEL_LEVIATHAN_MISSILE_LAUNCH.get(), SoundSource.HOSTILE, SteelLeviathanConstants.SOUND_VOLUME_64, 1.0F);
    }

    protected boolean isMissileSpawnClear(Vec3 pos) {
        BlockPos blockPos = BlockPos.containing(pos);
        return this.level().getBlockState(blockPos).isAir()
                || this.level().getBlockState(blockPos).getCollisionShape(this.level(), blockPos).isEmpty();
    }

    public Vec3 localToWorldDir(Vec3 local) {
        float yaw = this.getYRot() * Mth.DEG_TO_RAD;
        float pitch = getBodyPitch() * Mth.DEG_TO_RAD;
        float cosPitch = Mth.cos(pitch);
        float sinPitch = Mth.sin(pitch);
        double x1 = local.x;
        double y1 = local.y * cosPitch - local.z * sinPitch;
        double z1 = local.y * sinPitch + local.z * cosPitch;
        float cosYaw = Mth.cos(yaw);
        float sinYaw = Mth.sin(yaw);
        double x2 = x1 * cosYaw - z1 * sinYaw;
        double z2 = x1 * sinYaw + z1 * cosYaw;
        return new Vec3(x2, y1, z2);
    }

    public void repairRandomHeatsink(net.minecraft.util.RandomSource random) {
        if (isVulnerable()) {
            return;
        }
        java.util.List<Integer> broken = new java.util.ArrayList<>();
        for (int i = 0; i < SteelLeviathanConstants.MAX_HEATSINKS; i++) {
            if (isHeatsinkPresent(i) && isHeatsinkDestroyed(i)) {
                broken.add(i);
            }
        }
        if (broken.isEmpty()) {
            return;
        }
        int index = broken.get(random.nextInt(broken.size()));
        setHeatsinkDestroyed(index, false);
        if (heatsinkEntities[index] != null) {
            heatsinkEntities[index].resetHealth();
        }
    }

    @Nullable
    public SteelLeviathanHeadEntity resolveHead() {
        if (this instanceof SteelLeviathanHeadEntity head) {
            return head;
        }
        int id = getHeadId();
        if (id != NO_LINK) {
            Entity entity = this.level().getEntity(id);
            if (entity instanceof SteelLeviathanHeadEntity head) {
                return head;
            }
            Optional<UUID> linked = getHeadUuid();
            UUID target = linked.orElse(savedHeadUuid);
            if (target != null) {
                Entity byUuid = findEntityByUuid(target);
                if (byUuid instanceof SteelLeviathanHeadEntity head) {
                    setHeadRef(head);
                    return head;
                }
            }
            return null;
        }
        Optional<UUID> uuid = getHeadUuid();
        if (uuid.isEmpty() && savedHeadUuid != null) {
            uuid = Optional.of(savedHeadUuid);
        }
        if (uuid.isEmpty()) {
            return null;
        }
        Entity byUuid = findEntityByUuid(uuid.get());
        if (byUuid instanceof SteelLeviathanHeadEntity head) {
            setHeadRef(head);
            return head;
        }
        if (this.level().isClientSide) {
            for (SteelLeviathanHeadEntity candidate : this.level().getEntitiesOfClass(
                    SteelLeviathanHeadEntity.class, this.getBoundingBox().inflate(256.0D))) {
                if (candidate.getUUID().equals(uuid.get())) {
                    setHeadRef(candidate);
                    return candidate;
                }
            }
        }
        return null;
    }

    @Nullable
    public SteelLeviathanPartEntity resolvePrev() {
        return resolveLinked(this.entityData.get(PREV_ID), this.entityData.get(PREV_UUID), savedPrevUuid);
    }

    @Nullable
    public SteelLeviathanPartEntity resolveNext() {
        return resolveLinked(this.entityData.get(NEXT_ID), this.entityData.get(NEXT_UUID), savedNextUuid);
    }

    @Nullable
    private Entity findEntityByUuid(UUID uuid) {
        if (this.level() instanceof ServerLevel serverLevel) {
            return serverLevel.getEntity(uuid);
        }
        return null;
    }

    @Nullable
    private SteelLeviathanPartEntity resolveLinked(int id, Optional<UUID> uuid, @Nullable UUID saved) {
        if (id != NO_LINK) {
            Entity entity = this.level().getEntity(id);
            if (entity instanceof SteelLeviathanPartEntity part) {
                return part;
            }
            UUID target = uuid.orElse(saved);
            if (target != null) {
                Entity byUuid = findEntityByUuid(target);
                if (byUuid instanceof SteelLeviathanPartEntity part) {
                    return part;
                }
            }
            return null;
        }
        UUID target = uuid.orElse(saved);
        if (target == null) {
            return null;
        }
        Entity byUuid = findEntityByUuid(target);
        if (byUuid instanceof SteelLeviathanPartEntity part) {
            return part;
        }
        if (this.level().isClientSide) {
            for (SteelLeviathanPartEntity candidate : this.level().getEntitiesOfClass(
                    SteelLeviathanPartEntity.class, this.getBoundingBox().inflate(256.0D))) {
                if (candidate.getUUID().equals(target)) {
                    return candidate;
                }
            }
        }
        return null;
    }

    public Vec3 getFacing() {
        return SteelLeviathanSinew.facingFromYawPitch(this.getYRot(), getBodyPitch());
    }

    private void discardOrphanChain() {
        List<SteelLeviathanPartEntity> parts = new ArrayList<>();
        parts.add(this);
        SteelLeviathanPartEntity walk = resolvePrev();
        while (walk != null && parts.size() < 64 && !parts.contains(walk)) {
            parts.add(walk);
            walk = walk.resolvePrev();
        }
        walk = resolveNext();
        while (walk != null && parts.size() < 64 && !parts.contains(walk)) {
            parts.add(walk);
            walk = walk.resolveNext();
        }
        UUID headUuid = getHeadUuid().orElse(savedHeadUuid);
        if (headUuid != null && this.level() instanceof ServerLevel serverLevel) {
            SteelLeviathanChunkTickets.release(serverLevel, headUuid);
        }
        for (SteelLeviathanPartEntity part : parts) {
            if (!part.isRemoved()) {
                part.discard();
            }
        }
    }

    public void followPrevious() {
        SteelLeviathanPartEntity prev = resolvePrev();
        if (prev == null) {
            return;
        }

        Vec3 prevPos = prev.position();
        Vec3 myPos = position();
        Vec3 toParent = prevPos.subtract(myPos);
        if (toParent.lengthSqr() < 1.0E-6D) {

            toParent = SteelLeviathanSinew.facingFromYawPitch(prev.getYRot(), prev.getBodyPitch());
            if (toParent.lengthSqr() < 1.0E-6D) {
                toParent = new Vec3(0.0D, 0.0D, 1.0D);
            }
        }

        double horizontal = Math.sqrt(toParent.x * toParent.x + toParent.z * toParent.z);
        float desiredYaw = horizontal < 1.0E-2D
                ? getYRot()
                : (float) (Mth.atan2(-toParent.x, toParent.z) * Mth.RAD_TO_DEG);
        float desiredPitch = horizontal < 1.0E-4D
                ? prev.getBodyPitch()
                : (float) (-(Mth.atan2(toParent.y, horizontal) * Mth.RAD_TO_DEG));

        float nextYaw = Mth.approachDegrees(getYRot(), desiredYaw, SteelLeviathanConstants.SEGMENT_TURN_RATE);
        float nextPitch = Mth.approachDegrees(getBodyPitch(), desiredPitch, SteelLeviathanConstants.SEGMENT_TURN_RATE);
        float pitchBend = Mth.wrapDegrees(prev.getBodyPitch() - nextPitch);
        if (Math.abs(pitchBend) > SteelLeviathanConstants.MAX_SEGMENT_BEND) {
            float clampedPitch = prev.getBodyPitch() - Math.copySign(SteelLeviathanConstants.MAX_SEGMENT_BEND, pitchBend);
            nextPitch = Mth.approachDegrees(getBodyPitch(), clampedPitch, SteelLeviathanConstants.SEGMENT_TURN_RATE);
        }
        setLookRotation(Mth.wrapDegrees(nextYaw), Mth.wrapDegrees(nextPitch));

        float spacing = prev.getPartKind() == PartKind.HEAD ? 0.0F : SteelLeviathanConstants.SEGMENT_SPACING;
        if (spacing <= 0.0F) {
            setPos(prevPos.x, prevPos.y, prevPos.z);
        } else {
            Vec3 link = toParent;
            if (link.lengthSqr() < 1.0E-6D) {
                link = SteelLeviathanSinew.facingFromYawPitch(getYRot(), getBodyPitch());
                if (link.lengthSqr() < 1.0E-6D) {
                    link = new Vec3(0.0D, 0.0D, 1.0D);
                }
            }
            link = link.normalize().scale(spacing);
            setPos(prevPos.x - link.x, prevPos.y - link.y, prevPos.z - link.z);
        }

        setUnderground(prev.isUnderground());
        SteelLeviathanHeadEntity head = resolveHead();
        if (head != null) {
            setHeatsinksOpen(head.shouldHeatsinksBeOpen());
            setThrustersActive(head.areThrustersActive());
        }
    }

    protected void ensureHeatsinkEntities() {
        if (this.level().isClientSide) {
            return;
        }
        for (int i = 0; i < SteelLeviathanConstants.MAX_HEATSINKS; i++) {
            if (!isHeatsinkSlotActive(i)) {
                if (heatsinkEntities[i] != null && !heatsinkEntities[i].isRemoved()) {
                    heatsinkEntities[i].discard();
                }
                heatsinkEntities[i] = null;
                continue;
            }
            if (heatsinkEntities[i] == null || heatsinkEntities[i].isRemoved()) {
                SteelLeviathanHeatsinkHitboxEntity hitbox =
                        new SteelLeviathanHeatsinkHitboxEntity(EntityRegistry.STEEL_LEVIATHAN_HEATSINK.get(), this.level());
                Vec3 pos = heatsinkWorldPos(i);
                hitbox.init(this, i, pos.x, pos.y, pos.z);
                this.level().addFreshEntity(hitbox);
                heatsinkEntities[i] = hitbox;
            } else {
                Vec3 pos = heatsinkWorldPos(i);
                heatsinkEntities[i].syncColliderPosition(pos.x, pos.y, pos.z);
            }
        }
    }

    public Vec3 heatsinkWorldPos(int index) {
        boolean head = getPartKind() == PartKind.HEAD;
        Vec3 local = SteelLeviathanModelBones.heatsinkLocal(head, index);
        if (head) {
            return localToWorld(local);
        }

        return localToWorld(local).add(bodyHitboxTipwardCorrection());
    }

    private Vec3 bodyHitboxTipwardCorrection() {
        SteelLeviathanPartEntity prev = resolvePrev();
        if (prev != null) {
            Vec3 toPrev = prev.position().subtract(position());
            if (toPrev.lengthSqr() > 1.0E-6D) {
                return toPrev.normalize().scale(-SteelLeviathanConstants.SEGMENT_SPACING);
            }
        }
        SteelLeviathanPartEntity next = resolveNext();
        if (next != null) {
            Vec3 toNext = next.position().subtract(position());
            if (toNext.lengthSqr() > 1.0E-6D) {
                return toNext.normalize().scale(SteelLeviathanConstants.SEGMENT_SPACING);
            }
        }
        return localToWorld(new Vec3(0.0D, 0.0D, -SteelLeviathanConstants.SEGMENT_SPACING))
                .subtract(position());
    }

    public Vec3 localToWorld(Vec3 local) {

        float yaw = this.getYRot() * Mth.DEG_TO_RAD;
        float pitch = getBodyPitch() * Mth.DEG_TO_RAD;
        float cosPitch = Mth.cos(pitch);
        float sinPitch = Mth.sin(pitch);
        double x1 = local.x;
        double y1 = local.y * cosPitch - local.z * sinPitch;
        double z1 = local.y * sinPitch + local.z * cosPitch;
        float cosYaw = Mth.cos(yaw);
        float sinYaw = Mth.sin(yaw);
        double x2 = x1 * cosYaw - z1 * sinYaw;
        double z2 = x1 * sinYaw + z1 * cosYaw;
        return new Vec3(this.getX() + x2, this.getY() + y1, this.getZ() + z2);
    }

    public Vec3 mawDrillWorldPos() {
        return localToWorld(new Vec3(0.0D, 0.0D, 2.5D));
    }

    protected void tickContactDamage() {
        if (this.level().isClientSide || isUnderground()) {
            return;
        }
        if (contactCooldown > 0) {
            contactCooldown--;
            return;
        }
        float damage;
        int interval;
        switch (getPartKind()) {
            case HEAD -> {
                damage = SteelLeviathanConstants.HEAD_CONTACT_DAMAGE;
                interval = SteelLeviathanConstants.HEAD_CONTACT_INTERVAL;
            }
            case TAIL -> {
                damage = SteelLeviathanConstants.TAIL_CONTACT_DAMAGE;
                interval = SteelLeviathanConstants.TAIL_CONTACT_INTERVAL;
            }
            default -> {
                damage = SteelLeviathanConstants.BODY_CONTACT_DAMAGE;
                interval = SteelLeviathanConstants.BODY_CONTACT_INTERVAL;
            }
        }
        AABB box = getBoundingBox().inflate(0.1D);
        DamageSource source = DamageTypeRegistry.getSimpleDamageSource(this.level(), DamageTypeRegistry.STEEL_LEVIATHAN_CONTACT);
        SteelLeviathanHeadEntity head = resolveHead();
        for (LivingEntity entity : this.level().getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
            if (head != null && head.isAlliedPart(entity)) {
                continue;
            }
            entity.hurt(source, damage);
            contactCooldown = interval;
        }
    }

    protected void captureRenderRotation() {
        this.yRotO = this.getYRot();
        this.bodyYawO = this.getBodyYaw();
        this.bodyPitchO = this.getBodyPitch();
    }

    @Override
    public void tick() {
        boolean body = !(this instanceof SteelLeviathanHeadEntity);
        if (!this.level().isClientSide && body) {

            captureRenderRotation();
            float prevYaw = this.yRotO;
            float prevBodyYaw = this.bodyYawO;
            float prevPitch = this.bodyPitchO;
            double prevX = this.getX();
            double prevY = this.getY();
            double prevZ = this.getZ();

            super.tick();
            followPrevious();

            this.yRotO = prevYaw;
            this.bodyYawO = prevBodyYaw;
            this.bodyPitchO = prevPitch;
            this.xo = prevX;
            this.yo = prevY;
            this.zo = prevZ;

            if (this.invulnerableTime > 0) {
                this.invulnerableTime--;
            }
        } else {
            super.tick();
        }

        if (!this.level().isClientSide) {
            refreshChunkHints();
            SteelLeviathanChunkTickets.contribute(this);

            SteelLeviathanHeadEntity head = resolveHead();
            if (head == null && body) {

                missingHeadTicks++;
                if (missingHeadTicks > MISSING_HEAD_GRACE) {
                    discardOrphanChain();
                    return;
                }
            } else {
                missingHeadTicks = 0;
            }
            ensureHeatsinkEntities();
            tickContactDamage();
            tickArmorShed();
            tickMissileRelease();
        } else {
            if (this.lastLookSyncTick != this.tickCount) {
                this.bodyYawO = this.getBodyYaw();
                this.bodyPitchO = this.getBodyPitch();
            }
            tickClientSpin();
        }
        setBoundingBox(makeBoundingBox());
        tickCollisionDisplacement();
    }

    private void tickClientSpin() {
        mawGearSpinAngle += SteelLeviathanConstants.MAW_GEAR_SPIN_PER_TICK;
        if (!spinPosInitialized) {
            lastSpinX = getX();
            lastSpinY = getY();
            lastSpinZ = getZ();
            spinPosInitialized = true;
            return;
        }
        float dist = (float) Math.sqrt(
                Mth.square(getX() - lastSpinX)
                        + Mth.square(getY() - lastSpinY)
                        + Mth.square(getZ() - lastSpinZ));
        lastSpinX = getX();
        lastSpinY = getY();
        lastSpinZ = getZ();
        if (dist <= 1.0E-5F) {
            return;
        }
        bodyGearSpinAngle += dist * SteelLeviathanConstants.BODY_GEAR_SPIN_PER_BLOCK;
        drillSpinAngle += dist * SteelLeviathanConstants.DRILL_SPIN_PER_BLOCK;
    }

    public float getBodyGearSpinAngle() {
        return bodyGearSpinAngle;
    }

    public float getMawGearSpinAngle() {
        return mawGearSpinAngle;
    }

    public float getDrillSpinAngle() {
        return drillSpinAngle;
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        if (this.level().isClientSide && (BODY_YAW.equals(key) || BODY_PITCH.equals(key))) {

            float nextYaw = this.entityData.get(BODY_YAW);
            float nextPitch = this.entityData.get(BODY_PITCH);
            if (!this.clientLookInitialized) {
                this.clientBodyYaw = nextYaw;
                this.clientBodyPitch = nextPitch;
                this.bodyYawO = nextYaw;
                this.bodyPitchO = nextPitch;
                this.clientLookInitialized = true;
                this.lastLookSyncTick = this.tickCount;
            } else if (this.lastLookSyncTick != this.tickCount) {
                this.bodyYawO = this.clientBodyYaw;
                this.bodyPitchO = this.clientBodyPitch;
                this.clientBodyYaw = nextYaw;
                this.clientBodyPitch = nextPitch;
                this.lastLookSyncTick = this.tickCount;
            } else {

                this.clientBodyYaw = nextYaw;
                this.clientBodyPitch = nextPitch;
            }
        }
        super.onSyncedDataUpdated(key);
    }

    public void refreshChunkHints() {
        if (this instanceof SteelLeviathanHeadEntity) {
            hintHeadChunkX = chunkPosition().x;
            hintHeadChunkZ = chunkPosition().z;
        } else {
            SteelLeviathanHeadEntity head = resolveHead();
            if (head != null) {
                hintHeadChunkX = head.chunkPosition().x;
                hintHeadChunkZ = head.chunkPosition().z;
            }
        }
        SteelLeviathanPartEntity prev = resolvePrev();
        if (prev != null) {
            hintPrevChunkX = prev.chunkPosition().x;
            hintPrevChunkZ = prev.chunkPosition().z;
        }
        SteelLeviathanPartEntity next = resolveNext();
        if (next != null) {
            hintNextChunkX = next.chunkPosition().x;
            hintNextChunkZ = next.chunkPosition().z;
        }
    }

    public void seedChunkHints(@Nullable SteelLeviathanHeadEntity head,
                               @Nullable SteelLeviathanPartEntity prev,
                               @Nullable SteelLeviathanPartEntity next) {
        if (head != null) {
            hintHeadChunkX = head.chunkPosition().x;
            hintHeadChunkZ = head.chunkPosition().z;
        }
        if (prev != null) {
            hintPrevChunkX = prev.chunkPosition().x;
            hintPrevChunkZ = prev.chunkPosition().z;
        }
        if (next != null) {
            hintNextChunkX = next.chunkPosition().x;
            hintNextChunkZ = next.chunkPosition().z;
        }
    }

    public void addChunkHints(LongSet out) {
        if (hintHeadChunkX != NO_CHUNK_HINT) {
            out.add(ChunkPos.asLong(hintHeadChunkX, hintHeadChunkZ));
        }
        if (hintPrevChunkX != NO_CHUNK_HINT) {
            out.add(ChunkPos.asLong(hintPrevChunkX, hintPrevChunkZ));
        }
        if (hintNextChunkX != NO_CHUNK_HINT) {
            out.add(ChunkPos.asLong(hintNextChunkX, hintNextChunkZ));
        }
    }

    @Override
    protected AABB makeBoundingBox() {
        float w = switch (getPartKind()) {
            case HEAD -> SteelLeviathanConstants.HEAD_WIDTH;
            case TAIL -> SteelLeviathanConstants.TAIL_WIDTH;
            default -> SteelLeviathanConstants.SEGMENT_WIDTH;
        };
        float h = switch (getPartKind()) {
            case HEAD -> SteelLeviathanConstants.HEAD_HEIGHT;
            case TAIL -> SteelLeviathanConstants.TAIL_HEIGHT;
            default -> SteelLeviathanConstants.SEGMENT_HEIGHT;
        };
        float halfW = w * 0.5F;
        float halfH = h * 0.5F;
        float halfD = getPartKind() == PartKind.SEGMENT
                ? SteelLeviathanConstants.SEGMENT_COLLIDER_HALF_DEPTH
                : halfW;

        double cx = this.getX();
        double cy = this.getY();
        double cz = this.getZ();
        if (getPartKind() == PartKind.SEGMENT) {
            Vec3 center = localToWorld(new Vec3(0.0D, 0.0D, SteelLeviathanConstants.SEGMENT_COLLIDER_FORWARD))
                    .add(bodyHitboxTipwardCorrection());
            cx = center.x;
            cy = center.y;
            cz = center.z;
        }

        double[] extents = BurrowMissileColliders.yawPitchMorphedHalfExtents(
                halfW, halfH, halfD, getYRot(), getBodyPitch());
        return new AABB(
                cx - extents[0], cy - extents[1], cz - extents[2],
                cx + extents[0], cy + extents[1], cz + extents[2]);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.level().isClientSide || isInvulnerableTo(source) || source.is(DamageTypeTags.IS_FALL)) {
            return false;
        }

        if (this.invulnerableTime > 0) {
            return false;
        }

        if (amount > SteelLeviathanConstants.ANTIBUTCHER_THRESHOLD) {
            return false;
        }

        SteelLeviathanHeadEntity head = resolveHead();
        boolean damaged;

        if (!isVulnerable()) {
            Entity attacker = source.getDirectEntity();
            if (attacker == null) {
                attacker = source.getEntity();
            }
            SteelLeviathanHeatsinkHitboxEntity heatsink = findHeatsinkAlongAttackRay(attacker);
            if (heatsink != null) {
                damaged = heatsink.hurt(source, amount);
            } else {
                playSound(SoundRegistry.STEEL_LEVIATHAN_METAL_HIT.get(),
                        SteelLeviathanConstants.SOUND_VOLUME_64, 0.9F + random.nextFloat() * 0.2F);
                if (source.getDirectEntity() instanceof Projectile projectile) {
                    ricochet(projectile);
                }
                if (head != null) {
                    head.onPartAttacked(source);
                }
                damaged = false;
            }
        } else {
            if (head == null) {
                damaged = false;
            } else {
                damaged = head.damageMainHealth(source, amount, this);
            }
        }

        if (damaged && !this.level().isClientSide) {
            this.invulnerableTime = 20;
        }
        return damaged;
    }

    @Nullable
    private SteelLeviathanHeatsinkHitboxEntity findHeatsinkAlongAttackRay(@Nullable Entity attacker) {
        if (attacker == null) {
            return null;
        }
        Vec3 origin;
        Vec3 look;
        if (attacker instanceof LivingEntity living) {
            origin = living.getEyePosition(1.0F);
            look = living.getViewVector(1.0F);
        } else if (attacker instanceof Projectile) {
            origin = attacker.position();
            look = attacker.getDeltaMovement();
            if (look.lengthSqr() < 1.0E-8D) {
                return null;
            }
            look = look.normalize();
        } else {
            return null;
        }
        if (look.lengthSqr() < 1.0E-8D) {
            return null;
        }

        double reach = SteelLeviathanConstants.HEATSINK_HITSCAN_REACH;
        Vec3 rayEnd = origin.add(look.scale(reach));
        AABB hull = getBoundingBox();
        Optional<Vec3> surfaceHit = hull.clip(origin, rayEnd);
        Vec3 pierceStart = surfaceHit.orElseGet(() -> closestPointOnAabb(hull, origin));
        Vec3 pierceEnd = pierceStart.add(look.scale(SteelLeviathanConstants.HEATSINK_HITSCAN_PIERCE));

        SteelLeviathanHeatsinkHitboxEntity best = null;
        double bestDist = Double.MAX_VALUE;
        for (int i = 0; i < SteelLeviathanConstants.MAX_HEATSINKS; i++) {
            SteelLeviathanHeatsinkHitboxEntity heatsink = heatsinkEntities[i];
            if (heatsink == null || heatsink.isRemoved() || !isHeatsinkSlotActive(i)) {
                continue;
            }
            AABB sinkBox = heatsink.getBoundingBox();

            if (!sinkBox.intersects(hull.inflate(0.75D))) {
                continue;
            }
            Optional<Vec3> hit = sinkBox.clip(origin, pierceEnd);
            if (hit.isEmpty()) {
                hit = sinkBox.clip(pierceStart, pierceEnd);
            }
            if (hit.isEmpty()) {
                continue;
            }
            double along = hit.get().subtract(pierceStart).dot(look);
            if (along < -0.25D || along > SteelLeviathanConstants.HEATSINK_HITSCAN_PIERCE + 0.25D) {
                continue;
            }
            double dist = origin.distanceToSqr(hit.get());
            if (dist < bestDist) {
                bestDist = dist;
                best = heatsink;
            }
        }
        return best;
    }

    private static Vec3 closestPointOnAabb(AABB box, Vec3 point) {
        return new Vec3(
                Mth.clamp(point.x, box.minX, box.maxX),
                Mth.clamp(point.y, box.minY, box.maxY),
                Mth.clamp(point.z, box.minZ, box.maxZ));
    }

    protected void ricochet(Projectile projectile) {
        Vec3 motion = projectile.getDeltaMovement();
        Vec3 normal = getFacing().normalize();
        Vec3 reflected = motion.subtract(normal.scale(2.0D * motion.dot(normal)));
        if (reflected.lengthSqr() < 1.0E-4D) {
            reflected = motion.scale(-1.0D);
        }
        reflected = reflected.normalize().scale(motion.length()).add(
                (this.random.nextDouble() - 0.5D) * 0.2D,
                (this.random.nextDouble() - 0.5D) * 0.1D,
                (this.random.nextDouble() - 0.5D) * 0.2D);
        projectile.setDeltaMovement(reflected);
        projectile.hasImpulse = true;
    }

    public boolean damageHeatsink(int index, DamageSource source, float amount) {
        if (amount > SteelLeviathanConstants.ANTIBUTCHER_THRESHOLD) {
            return false;
        }
        if (!isHeatsinkSlotActive(index)) {
            return false;
        }
        SteelLeviathanHeadEntity head = resolveHead();
        if (head != null) {
            head.onPartAttacked(source);
        }
        return true;
    }

    public void onHeatsinkBroken(int index) {
        setHeatsinkDestroyed(index, true);
        if (heatsinkEntities[index] != null) {
            heatsinkEntities[index].discard();
            heatsinkEntities[index] = null;
        }
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
        if (!isUnderground()) {
            return true;
        }
        return isAboveTerrainForCollision();
    }

    private boolean isAboveTerrainForCollision() {
        int groundY = this.level().getHeight(
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Mth.floor(getX()), Mth.floor(getZ()));
        if (groundY <= this.level().getMinBuildHeight()) {
            return false;
        }
        return getY() >= groundY - SteelLeviathanConstants.COLLISION_GROUND_SLACK;
    }

    private void tickCollisionDisplacement() {
        if (!canBeCollidedWith()) {
            return;
        }
        AABB box = getBoundingBox();
        for (Entity entity : this.level().getEntities(this, box, this::shouldDisplaceEntity)) {
            Vec3 push = minimumAabbSeparation(entity.getBoundingBox(), box);
            if (push.lengthSqr() < 1.0E-8D) {
                continue;
            }
            entity.setPos(entity.getX() + push.x, entity.getY() + push.y, entity.getZ() + push.z);
            entity.hasImpulse = true;
        }
    }

    private boolean shouldDisplaceEntity(Entity entity) {
        if (!entity.isAlive() || entity.noPhysics || entity.isSpectator() || entity.isPassenger()) {
            return false;
        }
        if (entity instanceof SteelLeviathanPartEntity || entity instanceof ParentLinkedHitboxEntity) {
            return false;
        }
        if (entity instanceof LivingEntity) {
            return true;
        }
        return entity.getControllingPassenger() instanceof LivingEntity;
    }

    private static Vec3 minimumAabbSeparation(AABB movable, AABB solid) {
        double[][] options = {
                {solid.minX - movable.maxX, 0.0D, 0.0D},
                {solid.maxX - movable.minX, 0.0D, 0.0D},
                {0.0D, solid.minY - movable.maxY, 0.0D},
                {0.0D, solid.maxY - movable.minY, 0.0D},
                {0.0D, 0.0D, solid.minZ - movable.maxZ},
                {0.0D, 0.0D, solid.maxZ - movable.minZ},
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
    public boolean isInvisible() {
        return false;
    }

    @Override
    public boolean isInvisibleTo(net.minecraft.world.entity.player.Player player) {
        return false;
    }

    @Override
    public boolean shouldBeSaved() {
        return true;
    }

    @Override
    public boolean isAttackable() {
        return true;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return true;
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        AABB box = getBoundingBox().inflate(64.0D);
        SteelLeviathanHeadEntity head = resolveHead();
        if (head != null && head != this) {
            box = box.minmax(head.getBoundingBox().inflate(16.0D));
        }
        return box;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("HeadUuid")) {
            savedHeadUuid = tag.getUUID("HeadUuid");
            this.entityData.set(HEAD_UUID, Optional.of(savedHeadUuid));
        }
        if (tag.hasUUID("PrevUuid")) {
            savedPrevUuid = tag.getUUID("PrevUuid");
            this.entityData.set(PREV_UUID, Optional.of(savedPrevUuid));
        }
        if (tag.hasUUID("NextUuid")) {
            savedNextUuid = tag.getUUID("NextUuid");
            this.entityData.set(NEXT_UUID, Optional.of(savedNextUuid));
        }
        setChainIndex(tag.getInt("ChainIndex"));
        setVulnerable(tag.getBoolean("Vulnerable"));
        setHeatsinkCount(tag.getByte("HeatsinkCount"));
        if (tag.contains("HeatsinkPresent")) {
            setHeatsinkPresentMask(tag.getByte("HeatsinkPresent"));
        } else {

            int c = Mth.clamp(getHeatsinkCount(), 0, SteelLeviathanConstants.MAX_HEATSINKS);
            setHeatsinkPresentMask(c <= 0 ? 0 : (byte) ((1 << c) - 1));
        }
        this.entityData.set(HEATSINK_DESTROYED_MASK, tag.getByte("HeatsinkDestroyed"));
        setArmorShedTicks(tag.contains("ArmorShedTicks") ? tag.getInt("ArmorShedTicks") : 0);
        setHeatsinksOpen(tag.getBoolean("HeatsinksOpen"));
        setThrustersActive(tag.getBoolean("ThrustersActive"));
        setUnderground(tag.getBoolean("Underground"));
        if (tag.contains("MissileReleased")) {
            setMissileReleasedMask(tag.getByte("MissileReleased"));
        } else if (tag.getBoolean("ThrustersReleased")) {
            setMissileReleasedMask((byte) ((1 << SteelLeviathanModelBones.TAIL_MISSILE_COUNT) - 1));
        } else {
            setMissileReleasedMask((byte) 0);
        }
        missilePendingMask = tag.contains("MissilePending") ? tag.getByte("MissilePending") : 0;
        missileLaunchCooldown = tag.contains("MissileLaunchCooldown") ? tag.getInt("MissileLaunchCooldown") : 0;
        setLookRotation(tag.getFloat("BodyYaw"), tag.getFloat("BodyPitch"));
        if (tag.contains("HintHeadCX")) {
            hintHeadChunkX = tag.getInt("HintHeadCX");
            hintHeadChunkZ = tag.getInt("HintHeadCZ");
        }
        if (tag.contains("HintPrevCX")) {
            hintPrevChunkX = tag.getInt("HintPrevCX");
            hintPrevChunkZ = tag.getInt("HintPrevCZ");
        }
        if (tag.contains("HintNextCX")) {
            hintNextChunkX = tag.getInt("HintNextCX");
            hintNextChunkZ = tag.getInt("HintNextCZ");
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        getHeadUuid().ifPresent(uuid -> tag.putUUID("HeadUuid", uuid));
        if (savedPrevUuid != null) {
            tag.putUUID("PrevUuid", savedPrevUuid);
        } else {
            this.entityData.get(PREV_UUID).ifPresent(uuid -> tag.putUUID("PrevUuid", uuid));
        }
        if (savedNextUuid != null) {
            tag.putUUID("NextUuid", savedNextUuid);
        } else {
            this.entityData.get(NEXT_UUID).ifPresent(uuid -> tag.putUUID("NextUuid", uuid));
        }
        tag.putInt("ChainIndex", getChainIndex());
        tag.putBoolean("Vulnerable", isVulnerable());
        tag.putByte("HeatsinkCount", (byte) getHeatsinkCount());
        tag.putByte("HeatsinkPresent", getHeatsinkPresentMask());
        tag.putByte("HeatsinkDestroyed", getHeatsinkDestroyedMask());
        tag.putInt("ArmorShedTicks", getArmorShedTicks());
        tag.putBoolean("HeatsinksOpen", areHeatsinksOpen());
        tag.putBoolean("ThrustersActive", areThrustersActive());
        tag.putFloat("BodyPitch", getBodyPitch());
        tag.putBoolean("Underground", isUnderground());
        tag.putByte("MissileReleased", getMissileReleasedMask());
        tag.putByte("MissilePending", missilePendingMask);
        tag.putInt("MissileLaunchCooldown", missileLaunchCooldown);
        tag.putBoolean("ThrustersReleased", getMissileReleasedMask() != 0 && missilePendingMask == 0);
        tag.putFloat("BodyYaw", getBodyYaw());
        if (hintHeadChunkX != NO_CHUNK_HINT) {
            tag.putInt("HintHeadCX", hintHeadChunkX);
            tag.putInt("HintHeadCZ", hintHeadChunkZ);
        }
        if (hintPrevChunkX != NO_CHUNK_HINT) {
            tag.putInt("HintPrevCX", hintPrevChunkX);
            tag.putInt("HintPrevCZ", hintPrevChunkZ);
        }
        if (hintNextChunkX != NO_CHUNK_HINT) {
            tag.putInt("HintNextCX", hintNextChunkX);
            tag.putInt("HintNextCZ", hintNextChunkZ);
        }
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

    public String getHeatsinkBoneName(int index) {
        return SteelLeviathanBones.heatsinkBone(index);
    }
}

