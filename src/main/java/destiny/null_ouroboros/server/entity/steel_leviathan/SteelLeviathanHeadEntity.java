package destiny.null_ouroboros.server.entity.steel_leviathan;

import destiny.null_ouroboros.common.steel_leviathan.SteelLeviathanConstants;
import destiny.null_ouroboros.common.steel_leviathan.SteelLeviathanModelBones;
import destiny.null_ouroboros.common.steel_leviathan.SteelLeviathanSinew;
import destiny.null_ouroboros.server.registry.DamageTypeRegistry;
import destiny.null_ouroboros.server.registry.EntityRegistry;
import destiny.null_ouroboros.server.registry.ParticleTypeRegistry;
import destiny.null_ouroboros.server.registry.SoundRegistry;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class SteelLeviathanHeadEntity extends SteelLeviathanPartEntity {
    public static final TagKey<Item> DESIRED_ITEMS = TagKey.create(net.minecraft.core.registries.Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(destiny.null_ouroboros.NullOuroboros.MODID, "steel_leviathan_desired"));
    public static final TagKey<Item> GIFT_ITEMS = TagKey.create(net.minecraft.core.registries.Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(destiny.null_ouroboros.NullOuroboros.MODID, "steel_leviathan_gifts"));

    private static final EntityDataAccessor<Float> MAIN_HEALTH =
            SynchedEntityData.defineId(SteelLeviathanHeadEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> BEHAVIOR =
            SynchedEntityData.defineId(SteelLeviathanHeadEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> MOVE =
            SynchedEntityData.defineId(SteelLeviathanHeadEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> PHASE_TWO =
            SynchedEntityData.defineId(SteelLeviathanHeadEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> BOSS_BAR_VISIBLE =
            SynchedEntityData.defineId(SteelLeviathanHeadEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<String> HOLOGRAM_ITEM =
            SynchedEntityData.defineId(SteelLeviathanHeadEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> CHAIN_LENGTH =
            SynchedEntityData.defineId(SteelLeviathanHeadEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> CHAIN_SPAWNED =
            SynchedEntityData.defineId(SteelLeviathanHeadEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DYING =
            SynchedEntityData.defineId(SteelLeviathanHeadEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> BLINKER_TELEGRAPH =
            SynchedEntityData.defineId(SteelLeviathanHeadEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> INTEREST_TARGET_ID =
            SynchedEntityData.defineId(SteelLeviathanHeadEntity.class, EntityDataSerializers.INT);

    private final SteelLeviathanReputation reputation = new SteelLeviathanReputation();
    private final List<UUID> bodyUuids = new ArrayList<>();

    private final LongOpenHashSet chainChunkKeys = new LongOpenHashSet();

    private final SteelLeviathanBossCombat bossCombat = new SteelLeviathanBossCombat(this);

    int moveTicks;
    private int stateTicks;
    private int feedCount;
    int hitCooldown;
    private boolean chainInitialized;
    boolean heatsinksForcedOpen;

    @Nullable UUID combatTargetUuid;
    @Nullable private UUID interestPlayerUuid;
    @Nullable private UUID trackItemUuid;
    @Nullable private ItemStack desiredStack = ItemStack.EMPTY;
    @Nullable Vec3 lockedLungePos;
    @Nullable Vec3 standAnchor;

    @Nullable Vec3 breachAnchor;
    boolean interestBreachArrived;

    boolean interestDiveDone;
    int interestDiveTicks;
    int interestCruiseTicks;
    int interestRiseTicks;
    private int deathTicks;
    @Nullable
    private UUID deathTargetUuid;
    @Nullable
    private LivingEntity deathTarget;
    private float bobPhase;

    double smoothedGroundY = Double.NaN;
    private int interestGraceTicks;
    private int departTicks;

    @Nullable Vec3 consumeTarget;

    private float clientDrillFlare;

    private float desiredYaw;
    private int headingTicks;
    double verticalVel;
    double burrowSpringDepth = SteelLeviathanConstants.BURROW_GROUND_DEPTH;
    private float departDesiredYaw;
    private boolean naturalSpawn;

    private enum MotionMode {
        SURFACE,
        BURROW,
        AIR,
        DIVE
    }

    public SteelLeviathanHeadEntity(EntityType<?> type, Level level) {
        super(type, level);
    }

    public boolean isNaturalSpawn() {
        return naturalSpawn;
    }

    public void setNaturalSpawn(boolean naturalSpawn) {
        this.naturalSpawn = naturalSpawn;
    }

    public static int countNaturalOnVerge(ServerLevel level) {
        AABB bounds = new AABB(
                level.getWorldBorder().getMinX(),
                level.getMinBuildHeight(),
                level.getWorldBorder().getMinZ(),
                level.getWorldBorder().getMaxX(),
                level.getMaxBuildHeight(),
                level.getWorldBorder().getMaxZ());
        return level.getEntitiesOfClass(SteelLeviathanHeadEntity.class, bounds, SteelLeviathanHeadEntity::isNaturalSpawn).size();
    }

    public static boolean canNaturalSpawn(ServerLevel level) {
        return countNaturalOnVerge(level) < SteelLeviathanConstants.NATURAL_SPAWN_CAP;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(MAIN_HEALTH, SteelLeviathanConstants.MAX_HEALTH);
        this.entityData.define(BEHAVIOR, SteelLeviathanBehaviorState.PASSIVE.ordinal());
        this.entityData.define(MOVE, SteelLeviathanMove.NONE.ordinal());
        this.entityData.define(PHASE_TWO, false);
        this.entityData.define(BOSS_BAR_VISIBLE, false);
        this.entityData.define(HOLOGRAM_ITEM, "");
        this.entityData.define(CHAIN_LENGTH, 0);
        this.entityData.define(CHAIN_SPAWNED, false);
        this.entityData.define(DYING, false);
        this.entityData.define(BLINKER_TELEGRAPH, 0.0F);
        this.entityData.define(INTEREST_TARGET_ID, -1);
    }

    @Override
    public PartKind getPartKind() {
        return PartKind.HEAD;
    }

    public float getMainHealth() {
        return this.entityData.get(MAIN_HEALTH);
    }

    public void setMainHealth(float health) {
        this.entityData.set(MAIN_HEALTH, Mth.clamp(health, 0.0F, SteelLeviathanConstants.MAX_HEALTH));
    }

    public SteelLeviathanBehaviorState getBehaviorState() {
        return SteelLeviathanBehaviorState.values()[Mth.clamp(this.entityData.get(BEHAVIOR), 0, SteelLeviathanBehaviorState.values().length - 1)];
    }

    public void setBehaviorState(SteelLeviathanBehaviorState state) {
        this.entityData.set(BEHAVIOR, state.ordinal());
        stateTicks = 0;
        if (state != SteelLeviathanBehaviorState.INTEREST_WAIT) {
            setBlinkerTelegraph(0.0F);
        }
    }

    public SteelLeviathanMove getCurrentMove() {
        return SteelLeviathanMove.values()[Mth.clamp(this.entityData.get(MOVE), 0, SteelLeviathanMove.values().length - 1)];
    }

    public void setCurrentMove(SteelLeviathanMove move) {
        this.entityData.set(MOVE, move.ordinal());
        moveTicks = 0;
    }

    public float getBlinkerTelegraph() {
        return this.entityData.get(BLINKER_TELEGRAPH);
    }

    public void setBlinkerTelegraph(float value) {
        this.entityData.set(BLINKER_TELEGRAPH, Mth.clamp(value, 0.0F, 1.0F));
    }

    public int getInterestTargetId() {
        return this.entityData.get(INTEREST_TARGET_ID);
    }

    private void setInterestTarget(@Nullable Player player) {
        if (player == null) {
            interestPlayerUuid = null;
            this.entityData.set(INTEREST_TARGET_ID, -1);
            return;
        }
        interestPlayerUuid = player.getUUID();
        this.entityData.set(INTEREST_TARGET_ID, player.getId());
    }

    public float getClientDrillFlare() {
        return clientDrillFlare;
    }

    private void tickClientDrillFlare() {
        float target = getBehaviorState() == SteelLeviathanBehaviorState.INTEREST_SCAN ? 1.0F : 0.0F;
        clientDrillFlare = Mth.approach(clientDrillFlare, target, SteelLeviathanConstants.SCAN_DRILL_FLARE_RATE);
    }

    public boolean isPhaseTwo() {
        return this.entityData.get(PHASE_TWO);
    }

    public boolean isBossBarVisible() {
        return this.entityData.get(BOSS_BAR_VISIBLE);
    }

    public boolean isDying() {
        return this.entityData.get(DYING);
    }

    public boolean shouldHeatsinksBeOpen() {
        return bossCombat.shouldHeatsinksBeOpen();
    }

    public int getTimeScale() {
        return isPhaseTwo() ? 2 : 1;
    }

    public int scaled(int ticks) {
        return Math.max(1, ticks / getTimeScale());
    }

    @Nullable
    public LivingEntity getCombatTarget() {
        if (combatTargetUuid == null) {
            return null;
        }
        double range = SteelLeviathanConstants.BOSS_ESCAPE_RANGE;
        for (LivingEntity entity : this.level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(range))) {
            if (entity.getUUID().equals(combatTargetUuid) && entity.isAlive()
                    && (!(entity instanceof Player player) || !player.isSpectator())) {
                return entity;
            }
        }
        return null;
    }

    public boolean isAlliedPart(Entity entity) {
        return entity instanceof SteelLeviathanPartEntity part
                && part.resolveHead() == this;
    }

    @Nullable
    public ItemStack getHologramStack() {
        String id = this.entityData.get(HOLOGRAM_ITEM);
        if (id == null || id.isEmpty()) {
            return ItemStack.EMPTY;
        }
        Item item = ForgeRegistries.ITEMS.getValue(ResourceLocation.parse(id));
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }

    private void setHologram(@Nullable ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            this.entityData.set(HOLOGRAM_ITEM, "");
            desiredStack = ItemStack.EMPTY;
            return;
        }
        desiredStack = stack.copyWithCount(1);
        ResourceLocation key = ForgeRegistries.ITEMS.getKey(stack.getItem());
        this.entityData.set(HOLOGRAM_ITEM, key == null ? "" : key.toString());
    }

    private void setHologramDisplayed(boolean displayed) {
        if (!displayed) {
            this.entityData.set(HOLOGRAM_ITEM, "");
            return;
        }
        if (desiredStack == null || desiredStack.isEmpty()) {
            this.entityData.set(HOLOGRAM_ITEM, "");
            return;
        }
        ResourceLocation key = ForgeRegistries.ITEMS.getKey(desiredStack.getItem());
        this.entityData.set(HOLOGRAM_ITEM, key == null ? "" : key.toString());
    }

    @Override
    public void tick() {
        if (!this.level().isClientSide && !chainInitialized && !this.entityData.get(CHAIN_SPAWNED)) {
            spawnChain();
        }
        chainInitialized = true;

        if (!this.level().isClientSide) {

            captureRenderRotation();
            float prevYaw = this.yRotO;
            float prevBodyYaw = getBodyYawO();
            float prevPitch = getBodyPitchO();
            double prevX = this.getX();
            double prevY = this.getY();
            double prevZ = this.getZ();

            reputation.tickCooldowns();
            if (hitCooldown > 0) {
                hitCooldown--;
            }
            setHeatsinksOpen(shouldHeatsinksBeOpen());
            if (!isDying()) {
                tickBehavior();
            } else {
                tickDeath();
            }

            tickMawDrills();

            super.tick();
            this.yRotO = prevYaw;
            setBodyYawO(prevBodyYaw);
            setBodyPitchO(prevPitch);
            this.xo = prevX;
            this.yo = prevY;
            this.zo = prevZ;

            if (this.invulnerableTime > 0) {
                this.invulnerableTime--;
            }
        } else {
            tickClientDrillFlare();
            super.tick();
        }
    }

    private void spawnChain() {
        int total = SteelLeviathanConstants.MIN_SEGMENTS
                + this.random.nextInt(SteelLeviathanConstants.MAX_SEGMENTS - SteelLeviathanConstants.MIN_SEGMENTS + 1);
        this.entityData.set(CHAIN_LENGTH, total);
        this.entityData.set(CHAIN_SPAWNED, true);
        setChainIndex(0);
        setHeadRef(this);
        rollHeatsinks(this.random);
        setUnderground(true);
        setBodyPitch(0.0F);
        snapToGround(true);

        float baseYaw = getYRot();
        float curvePhase = 0.0F;
        if (naturalSpawn) {
            curvePhase = this.random.nextFloat() * ((float) Math.PI * 2.0F);
            bobPhase = curvePhase;
            float headPitch = -Mth.cos(bobPhase) * SteelLeviathanConstants.NATURAL_SPAWN_PITCH_AMPLITUDE;
            setBodyPitch(headPitch);
            desiredYaw = baseYaw + SteelLeviathanConstants.NATURAL_SPAWN_YAW_AMPLITUDE
                    * Mth.sin(SteelLeviathanConstants.NATURAL_SPAWN_CURVE_FREQUENCY + curvePhase);
        }

        SteelLeviathanPartEntity previous = this;
        bodyUuids.clear();
        ArrayList<SteelLeviathanPartEntity> chain = new ArrayList<>(total);
        chain.add(this);
        for (int i = 1; i < total; i++) {
            boolean tail = i == total - 1;
            SteelLeviathanPartEntity part = tail
                    ? new SteelLeviathanTailEntity(EntityRegistry.STEEL_LEVIATHAN_TAIL.get(), this.level())
                    : new SteelLeviathanSegmentEntity(EntityRegistry.STEEL_LEVIATHAN_SEGMENT.get(), this.level());

            part.setChainIndex(i);
            part.rollHeatsinks(this.random);
            part.setUnderground(true);

            if (naturalSpawn) {
                float t = i * SteelLeviathanConstants.NATURAL_SPAWN_CURVE_FREQUENCY + curvePhase;
                float yaw = baseYaw + SteelLeviathanConstants.NATURAL_SPAWN_YAW_AMPLITUDE * Mth.sin(t);
                float pitch = -SteelLeviathanConstants.NATURAL_SPAWN_PITCH_AMPLITUDE * Mth.cos(t);
                float pitchBend = Mth.wrapDegrees(previous.getBodyPitch() - pitch);
                if (Math.abs(pitchBend) > SteelLeviathanConstants.MAX_SEGMENT_BEND) {
                    pitch = previous.getBodyPitch() - Math.copySign(SteelLeviathanConstants.MAX_SEGMENT_BEND, pitchBend);
                }
                part.setYRot(yaw);
                part.setBodyPitch(pitch);

                float spacing = previous.getPartKind() == PartKind.HEAD ? 0.0F : SteelLeviathanConstants.SEGMENT_SPACING;
                if (spacing <= 0.0F) {
                    part.setPos(previous.getX(), previous.getY(), previous.getZ());
                } else {
                    Vec3 facing = SteelLeviathanSinew.facingFromYawPitch(previous.getYRot(), previous.getBodyPitch());
                    if (facing.lengthSqr() < 1.0E-6D) {
                        facing = new Vec3(0.0D, 0.0D, 1.0D);
                    } else {
                        facing = facing.normalize();
                    }
                    Vec3 pos = previous.position().subtract(facing.scale(spacing));
                    part.setPos(pos.x, pos.y, pos.z);
                }
            } else {
                Vec3 attach = previous.getBackConnectionWorld();
                part.setPos(attach.x, attach.y, attach.z);
                part.setYRot(previous.getYRot());
                part.setBodyPitch(previous.getBodyPitch());
            }

            this.level().addFreshEntity(part);
            bodyUuids.add(part.getUUID());
            part.setHeadRef(this);
            part.setPrevRef(previous);
            previous.setNextRef(part);
            chain.add(part);
            previous = part;
        }
        for (int i = 0; i < chain.size(); i++) {
            SteelLeviathanPartEntity part = chain.get(i);
            part.seedChunkHints(this,
                    i > 0 ? chain.get(i - 1) : null,
                    i + 1 < chain.size() ? chain.get(i + 1) : null);
        }
        SteelLeviathanChunkTickets.contributeChain(this, chain);
        if (!naturalSpawn) {
            desiredYaw = getYRot();
        }
        headingTicks = 0;
    }

    private void tickBehavior() {
        stateTicks++;
        moveTicks++;
        switch (getBehaviorState()) {
            case PASSIVE -> tickPassive();
            case INTEREST_APPROACH -> tickInterestApproach();
            case INTEREST_SCAN -> tickInterestScan();
            case INTEREST_WAIT -> tickInterestWait();
            case INTEREST_CONSUME -> tickInterestConsume();
            case INTEREST_DEPART -> tickInterestDepart();
            case GIFT_APPROACH -> tickGiftApproach();
            case BOSSFIGHT -> tickBossfight();
            case DEAD -> {}
        }
    }

    private void tickPassive() {
        setBossBarVisible(false);
        burrowWander();
        maybeStartInterest();
        maybeStartGift();
    }

    private void burrowWander() {
        setThrustersActive(false);
        bobPhase += SteelLeviathanConstants.BOB_SPEED;
        float bob = Mth.sin(bobPhase);

        tickHeadingCruise();

        float bobPitch = -Mth.cos(bobPhase) * SteelLeviathanConstants.BOB_PITCH_AMPLITUDE;
        approachPitch(bobPitch, SteelLeviathanConstants.HEAD_TURN_RATE_PASSIVE);

        Vec3 flat = SteelLeviathanSinew.facingFromYawPitch(getYRot(), 0.0F);
        Vec3 horiz = new Vec3(flat.x, 0.0D, flat.z);
        if (horiz.lengthSqr() < 1.0E-6D) {
            horiz = new Vec3(0.0D, 0.0D, 1.0D);
        } else {
            horiz = horiz.normalize();
        }
        double speed = SteelLeviathanConstants.CRUISE_SPEED;
        double nextX = getX() + horiz.x * speed;
        double nextZ = getZ() + horiz.z * speed;
        updateGroundGuide(nextX, nextZ);
        double deepY = smoothedGroundY - SteelLeviathanConstants.BOB_BURROW_DEEP;
        double peekY = smoothedGroundY + SteelLeviathanConstants.BOB_SURFACE_PEEK;
        double t = (bob + 1.0F) * 0.5F;
        double bobY = deepY + (peekY - deepY) * t;
        double nextY = getY() + (bobY - getY()) * SteelLeviathanConstants.BOB_Y_FOLLOW;
        setPos(nextX, nextY, nextZ);
        verticalVel = 0.0D;
        burrowSpringDepth = SteelLeviathanConstants.BOB_BURROW_DEEP;

        setUnderground(nextY < smoothedGroundY);
    }

    private void tickHeadingCruise() {
        if (headingTicks <= 0) {
            float jitter = SteelLeviathanConstants.WANDER_YAW_JITTER_DEGREES;
            desiredYaw = getYRot() + (this.random.nextFloat() * 2.0F - 1.0F) * jitter;
            int span = SteelLeviathanConstants.WANDER_HEADING_INTERVAL_MAX_TICKS
                    - SteelLeviathanConstants.WANDER_HEADING_INTERVAL_MIN_TICKS;
            headingTicks = SteelLeviathanConstants.WANDER_HEADING_INTERVAL_MIN_TICKS
                    + (span > 0 ? this.random.nextInt(span + 1) : 0);
        }
        headingTicks--;
        setYRot(Mth.approachDegrees(getYRot(), desiredYaw, SteelLeviathanConstants.HEAD_TURN_RATE_PASSIVE));
    }

    double horizontalDistanceSqr(Vec3 target) {
        double dx = target.x - getX();
        double dz = target.z - getZ();
        return dx * dx + dz * dz;
    }

    private double findGroundY(double x, double z) {
        int ix = Mth.floor(x);
        int iz = Mth.floor(z);
        int top = this.level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ix, iz);
        if (top <= this.level().getMinBuildHeight()) {
            BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos(ix, Mth.floor(getY()), iz);
            while (cursor.getY() > this.level().getMinBuildHeight() && this.level().getBlockState(cursor).isAir()) {
                cursor.move(0, -1, 0);
            }
            return cursor.getY() + 1.0D;
        }
        return top;
    }

    void updateGroundGuide(double x, double z) {
        double raw = sampleGroundAverage(x, z);
        if (Double.isNaN(smoothedGroundY)) {
            smoothedGroundY = raw;
            return;
        }
        double step = SteelLeviathanConstants.GROUND_GUIDE_MAX_STEP;
        smoothedGroundY += Mth.clamp(raw - smoothedGroundY, -step, step);
    }

    private double sampleGroundAverage(double x, double z) {
        double sum = 0.0D;
        int count = 0;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                sum += findGroundY(x + dx, z + dz);
                count++;
            }
        }
        return sum / count;
    }

    private boolean isAerialMove() {
        SteelLeviathanMove move = getCurrentMove();
        return move == SteelLeviathanMove.LAUNCH
                || move == SteelLeviathanMove.CIRCLE
                || (move == SteelLeviathanMove.LUNGE && areThrustersActive());
    }

    private boolean isHeadInAir() {
        AABB box = getBoundingBox().deflate(0.25D);
        return this.level().noCollision(this, box);
    }

    private void maybeStartInterest() {
        for (Player player : this.level().getEntitiesOfClass(Player.class,
                getBoundingBox().inflate(SteelLeviathanConstants.INTEREST_DETECT_RANGE))) {
            if (reputation.isOnCooldown(player.getUUID())) {
                continue;
            }
            if (!canSeePlayer(player)) {
                continue;
            }
            if (!this.random.nextBoolean()) {
                reputation.startCooldown(player.getUUID(), this.random);
                continue;
            }
            setInterestTarget(player);
            breachAnchor = computeBreachAnchor(player);
            interestBreachArrived = false;
            interestDiveDone = false;
            interestDiveTicks = 0;
            interestCruiseTicks = 0;
            interestRiseTicks = 0;
            setHologram(ItemStack.EMPTY);
            reputation.startCooldown(player.getUUID(), this.random);
            setBehaviorState(SteelLeviathanBehaviorState.INTEREST_APPROACH);
            return;
        }
    }

    private Vec3 computeBreachAnchor(Player player) {
        Vec3 away = player.getLookAngle();
        Vec3 flatAway = new Vec3(away.x, 0.0D, away.z);
        if (flatAway.lengthSqr() < 1.0E-6D) {
            flatAway = SteelLeviathanSinew.facingFromYawPitch(player.getYRot(), 0.0F);
            flatAway = new Vec3(flatAway.x, 0.0D, flatAway.z);
        }
        flatAway = flatAway.normalize();
        Vec3 pos = player.position().add(flatAway.scale(SteelLeviathanConstants.INTEREST_STAND_DISTANCE));
        return new Vec3(pos.x, 0.0D, pos.z);
    }

    private void maybeStartGift() {
        if (getBehaviorState() != SteelLeviathanBehaviorState.PASSIVE) {
            return;
        }
        for (Player player : this.level().getEntitiesOfClass(Player.class,
                getBoundingBox().inflate(SteelLeviathanConstants.INTEREST_DETECT_RANGE))) {
            if (reputation.isOnCooldown(player.getUUID())) {
                continue;
            }
            if (reputation.getScore(player.getUUID()) < SteelLeviathanConstants.REP_GIFT_THRESHOLD) {
                continue;
            }
            if (!canSeePlayer(player)) {
                continue;
            }
            if (!this.random.nextBoolean()) {
                reputation.startCooldown(player.getUUID(), this.random);
                continue;
            }
            if (this.random.nextInt(200) != 0) {
                continue;
            }
            setInterestTarget(player);
            reputation.startCooldown(player.getUUID(), this.random);
            setBehaviorState(SteelLeviathanBehaviorState.GIFT_APPROACH);
            return;
        }
    }

    private boolean canSeePlayer(Player player) {
        if (distanceToSqr(player) > SteelLeviathanConstants.INTEREST_DETECT_RANGE
                * SteelLeviathanConstants.INTEREST_DETECT_RANGE) {
            return false;
        }
        if (!this.level().canSeeSky(player.blockPosition())) {
            return false;
        }
        Vec3 facing = SteelLeviathanSinew.facingFromYawPitch(getYRot(), getBodyPitch());
        Vec3 toPlayer = player.getEyePosition().subtract(getEyePosition());
        double dist = toPlayer.length();
        if (dist < 1.0E-4D) {
            return true;
        }
        toPlayer = toPlayer.scale(1.0D / dist);
        float halfFov = SteelLeviathanConstants.INTEREST_DETECT_FOV_DEG * 0.5F;
        if (facing.dot(toPlayer) < Mth.cos(halfFov * Mth.DEG_TO_RAD)) {
            return false;
        }
        Vec3 from = getEyePosition();
        Vec3 to = player.getEyePosition();
        return this.level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this))
                .getType() == HitResult.Type.MISS;
    }

    private ItemStack findDesiredInInventory(Player player) {
        ItemStack main = player.getMainHandItem();
        if (!main.isEmpty() && main.is(DESIRED_ITEMS)) {
            return main.copyWithCount(1);
        }
        ItemStack off = player.getOffhandItem();
        if (!off.isEmpty() && off.is(DESIRED_ITEMS)) {
            return off.copyWithCount(1);
        }
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.is(DESIRED_ITEMS)) {
                return stack.copyWithCount(1);
            }
        }
        return ItemStack.EMPTY;
    }

    private void tickInterestApproach() {
        Player player = resolvePlayer(interestPlayerUuid);
        if (player == null) {
            clearInterest();
            setBehaviorState(SteelLeviathanBehaviorState.PASSIVE);
            return;
        }
        if (breachAnchor == null) {
            breachAnchor = computeBreachAnchor(player);
        }
        if (tickSurfaceApproach(player, breachAnchor, 1.0F)) {
            setBehaviorState(SteelLeviathanBehaviorState.INTEREST_SCAN);
            setCurrentMove(SteelLeviathanMove.STAND);
            standAnchor = position();
            updateGroundGuide(standAnchor.x, standAnchor.z);
        }
    }

    private void tickInterestScan() {
        Player player = resolvePlayer(interestPlayerUuid);
        if (player == null) {
            failInterest(false);
            return;
        }
        if (player.distanceToSqr(this) > SteelLeviathanConstants.INTEREST_WALKAWAY_BLOCKS
                * SteelLeviathanConstants.INTEREST_WALKAWAY_BLOCKS) {
            failInterest(true);
            return;
        }
        tickElevatedHold(stableLookPoint(player));
        if (stateTicks >= SteelLeviathanConstants.INTEREST_SCAN_TICKS) {
            ItemStack wanted = findDesiredInInventory(player);
            if (wanted.isEmpty()) {
                beginDepart();
                return;
            }
            setHologram(wanted);
            setBehaviorState(SteelLeviathanBehaviorState.INTEREST_WAIT);
        }
    }

    void resetSurfaceApproach() {
        interestBreachArrived = false;
        interestDiveDone = false;
        interestDiveTicks = 0;
        interestCruiseTicks = 0;
        interestRiseTicks = 0;
    }

    boolean tickSurfaceApproach(LivingEntity lookTarget, Vec3 breach, float timeScale) {
        return tickSurfaceApproach(lookTarget, breach, timeScale, 1.0F);
    }

    boolean tickSurfaceApproach(LivingEntity lookTarget, Vec3 breach, float timeScale, float motionScale) {
        setThrustersActive(false);
        float move = Math.max(0.1F, motionScale);
        float pitchRate = SteelLeviathanConstants.INTEREST_PITCH_RATE * Math.max(1.0F, move);
        double arrivalSq = SteelLeviathanConstants.INTEREST_BREACH_ARRIVAL
                * SteelLeviathanConstants.INTEREST_BREACH_ARRIVAL;
        float scale = Mth.clamp(timeScale, 0.1F, 1.0F);
        int diveMin = Math.max(1, Math.round(SteelLeviathanConstants.INTEREST_APPROACH_DIVE_MIN_TICKS * scale));
        int diveMax = Math.max(diveMin, Math.round(SteelLeviathanConstants.INTEREST_APPROACH_DIVE_TICKS * scale));

        int diveHardTimeout = Math.max(diveMax * 3, SteelLeviathanConstants.INTEREST_APPROACH_DIVE_TICKS);
        int emergeTicks = Math.max(1, Math.round(SteelLeviathanConstants.INTEREST_APPROACH_EMERGE_TICKS * scale));
        int arcTicks = Math.max(1, Math.round(SteelLeviathanConstants.INTEREST_APPROACH_ARC_TICKS * scale));
        float cruiseMove = Math.min(move, 1.25F);
        double cruiseSpeed = SteelLeviathanConstants.INTEREST_BURROW_CRUISE_SPEED * cruiseMove;
        float diveSpring = SteelLeviathanConstants.INTEREST_DIVE_SPRING_GAIN * move;
        float cruiseYSpring = move > 1.01F ? 0.35F : 0.2F;
        float yawRate = move > 1.01F
                ? SteelLeviathanConstants.BOSS_UNDERGROUND_YAW_RATE
                : SteelLeviathanConstants.INTEREST_YAW_RATE;

        int cruiseFailsafe = SteelLeviathanConstants.STAND_APPROACH_CRUISE_FAILSAFE_TICKS;
        double burrowDepth = move > 1.01F
                ? SteelLeviathanConstants.BOSS_BURROW_DEPTH
                : SteelLeviathanConstants.INTEREST_APPROACH_BURROW_DEPTH;

        if (!interestBreachArrived) {
            setUnderground(true);
            burrowSpringDepth = burrowDepth;
            updateGroundGuide(getX(), getZ());
            double deepY = smoothedGroundY - burrowDepth;

            if (!interestDiveDone) {
                interestDiveTicks++;
                approachPitch(SteelLeviathanConstants.INTEREST_BURROW_DIVE_PITCH, pitchRate);
                double nextY = getY() + (deepY - getY()) * Math.min(0.55F, diveSpring);
                setPos(getX(), nextY, getZ());
                verticalVel = 0.0D;
                boolean deepEnough = getY() <= deepY + 0.35D;
                if ((deepEnough && interestDiveTicks >= diveMin)
                        || interestDiveTicks >= diveHardTimeout) {
                    interestDiveDone = true;
                    interestCruiseTicks = 0;
                }
                return false;
            }

            interestCruiseTicks++;
            boolean deepEnough = getY() <= deepY + 0.35D;
            boolean lateCruise = horizontalDistanceSqr(breach) <= 64.0D;
            if (lateCruise) {
                approachPitch(SteelLeviathanConstants.INTEREST_SURFACE_NOSE_UP_PITCH, pitchRate);
                burrowCruiseToward(breach.x, breach.z,
                        cruiseSpeed,
                        burrowDepth,
                        false,
                        yawRate,
                        cruiseYSpring);
                faceEntityHorizontal(lookTarget, yawRate);
            } else {
                approachPitch(SteelLeviathanConstants.INTEREST_BURROW_CRUISE_PITCH, pitchRate);
                burrowCruiseToward(breach.x, breach.z,
                        cruiseSpeed,
                        burrowDepth,
                        true,
                        yawRate,
                        cruiseYSpring);
            }

            boolean arrived = horizontalDistanceSqr(breach) <= arrivalSq && deepEnough;
            boolean failsafe = interestCruiseTicks >= cruiseFailsafe;
            if (arrived || failsafe) {
                interestBreachArrived = true;
                interestRiseTicks = 0;
                if (failsafe) {
                    setPos(breach.x, deepY, breach.z);
                }
            }
            return false;
        }

        interestRiseTicks++;
        double emergeX = Mth.lerp(0.15D, getX(), breach.x);
        double emergeZ = Mth.lerp(0.15D, getZ(), breach.z);
        updateGroundGuide(emergeX, emergeZ);
        double deepY = smoothedGroundY - burrowDepth;
        double surfaceY = smoothedGroundY + 1.0D;
        verticalVel = 0.0D;

        Vec3 toTarget = new Vec3(lookTarget.getX() - breach.x, 0.0D, lookTarget.getZ() - breach.z);
        if (toTarget.lengthSqr() < 1.0E-6D) {
            toTarget = SteelLeviathanSinew.facingFromYawPitch(getYRot(), 0.0F);
            toTarget = new Vec3(toTarget.x, 0.0D, toTarget.z);
        }
        if (toTarget.lengthSqr() < 1.0E-6D) {
            toTarget = new Vec3(0.0D, 0.0D, 1.0D);
        } else {
            toTarget = toTarget.normalize();
        }

        double R = SteelLeviathanConstants.INTEREST_ARC_RADIUS;

        if (interestRiseTicks <= emergeTicks) {
            float t = Mth.clamp(interestRiseTicks / (float) emergeTicks, 0.0F, 1.0F);
            float ease = (float) Math.sin(t * (float) (Math.PI * 0.5D));
            double y = deepY + (surfaceY - deepY) * ease;
            setUnderground(y < smoothedGroundY);
            setPos(emergeX, y, emergeZ);
            approachPitch(SteelLeviathanConstants.INTEREST_SURFACE_NOSE_UP_PITCH, pitchRate);
            faceEntityHorizontal(lookTarget, yawRate);
            return false;
        }

        setUnderground(false);
        float t = Mth.clamp((interestRiseTicks - emergeTicks) / (float) arcTicks, 0.0F, 1.0F);
        float alpha = t * (float) (Math.PI * 0.5D);
        double sinA = Math.sin(alpha);
        double cosA = Math.cos(alpha);
        double x = breach.x + toTarget.x * R * (1.0D - cosA);
        double z = breach.z + toTarget.z * R * (1.0D - cosA);
        double y = surfaceY + R * sinA;
        setPos(x, y, z);

        float tangentPitch = (float) (Mth.atan2(-cosA, Math.max(sinA, 0.05D)) * Mth.RAD_TO_DEG);
        tangentPitch = Mth.clamp(tangentPitch, -85.0F, SteelLeviathanConstants.INTEREST_LOOK_PITCH_MAX);
        approachPitch(tangentPitch, pitchRate);
        if (t > 0.65F) {
            faceEntityHorizontal(lookTarget, yawRate);
        }
        if (t >= 1.0F) {
            standAnchor = position();
            updateGroundGuide(standAnchor.x, standAnchor.z);
            return true;
        }
        return false;
    }

    void tickElevatedHold(LivingEntity lookTarget) {
        tickElevatedHold(stableLookPoint(lookTarget));
    }

    void tickElevatedHold(Vec3 lookPoint) {
        setThrustersActive(false);
        setUnderground(false);
        Vec3 anchor = standAnchor != null ? standAnchor
                : (breachAnchor != null ? breachAnchor : position());
        updateGroundGuide(anchor.x, anchor.z);
        double holdY = smoothedGroundY + SteelLeviathanConstants.INTEREST_HOLD_HEIGHT;
        double dy = holdY - getY();
        setPos(anchor.x, getY() + dy * SteelLeviathanConstants.INTEREST_HOLD_LIFT_GAIN, anchor.z);
        verticalVel = 0.0D;
        interestLookAt(lookPoint, SteelLeviathanConstants.INTEREST_LOOK_PITCH_MAX);
    }

    void burrowCruiseToward(double targetX, double targetZ, double speed, double depth, boolean steerYaw) {
        burrowCruiseToward(targetX, targetZ, speed, depth, steerYaw, SteelLeviathanConstants.INTEREST_YAW_RATE, 0.2F);
    }

    void burrowCruiseToward(double targetX, double targetZ, double speed, double depth, boolean steerYaw, float yawRate) {
        burrowCruiseToward(targetX, targetZ, speed, depth, steerYaw, yawRate, 0.2F);
    }

    void burrowCruiseToward(double targetX, double targetZ, double speed, double depth, boolean steerYaw,
                            float yawRate, float ySpring) {
        double dx = targetX - getX();
        double dz = targetZ - getZ();
        Vec3 horiz;
        if (dx * dx + dz * dz > 1.0E-4D) {
            double len = Math.sqrt(dx * dx + dz * dz);
            horiz = new Vec3(dx / len, 0.0D, dz / len);
            if (steerYaw) {
                float yaw = (float) (Mth.atan2(-dx, dz) * Mth.RAD_TO_DEG);
                setYRot(Mth.approachDegrees(getYRot(), yaw, yawRate));
            }
        } else {
            horiz = Vec3.ZERO;
        }
        double nextX = getX() + horiz.x * speed;
        double nextZ = getZ() + horiz.z * speed;
        updateGroundGuide(nextX, nextZ);
        double deepY = smoothedGroundY - depth;
        double nextY = getY() + (deepY - getY()) * Mth.clamp(ySpring, 0.05F, 0.55F);
        setPos(nextX, nextY, nextZ);
        verticalVel = 0.0D;
        setUnderground(true);
    }

    void faceEntityHorizontal(LivingEntity entity) {
        faceEntityHorizontal(entity, SteelLeviathanConstants.INTEREST_YAW_RATE);
    }

    void faceEntityHorizontal(LivingEntity entity, float yawRate) {
        double dx = entity.getX() - getX();
        double dz = entity.getZ() - getZ();
        if (dx * dx + dz * dz < 1.0E-4D) {
            return;
        }
        float yaw = (float) (Mth.atan2(-dx, dz) * Mth.RAD_TO_DEG);
        setYRot(Mth.approachDegrees(getYRot(), yaw, yawRate));
    }

    private void tickInterestWait() {
        Player player = resolvePlayer(interestPlayerUuid);
        if (player == null) {
            failInterest(false);
            return;
        }
        setBlinkerTelegraph(stateTicks / (float) SteelLeviathanConstants.INTEREST_WAIT_TICKS);
        if (player.distanceToSqr(this) > SteelLeviathanConstants.INTEREST_WALKAWAY_BLOCKS * SteelLeviathanConstants.INTEREST_WALKAWAY_BLOCKS) {
            failInterest(true);
            return;
        }

        if (trackItemUuid == null) {
            ItemEntity groundItem = findTrackedDesiredItem(player);
            if (groundItem != null) {
                trackItemUuid = groundItem.getUUID();
                interestGraceTicks = SteelLeviathanConstants.INTEREST_GRACE_TICKS;
            }
        }

        ItemEntity tracked = resolveItem(trackItemUuid);
        if (tracked == null && trackItemUuid != null) {
            trackItemUuid = null;
            ItemEntity again = findTrackedDesiredItem(player);
            if (again != null) {
                trackItemUuid = again.getUUID();
                interestGraceTicks = SteelLeviathanConstants.INTEREST_GRACE_TICKS;
                tracked = again;
            }
        }

        Vec3 lookPoint = tracked != null
                ? tracked.position().add(0.0D, 0.25D, 0.0D)
                : stableLookPoint(player);
        tickElevatedHold(lookPoint);

        setHologramDisplayed(tracked == null);

        if (interestGraceTicks > 0) {
            interestGraceTicks--;
            if (interestGraceTicks == 0 && resolveItem(trackItemUuid) != null) {
                setBehaviorState(SteelLeviathanBehaviorState.INTEREST_CONSUME);
                return;
            }
        }

        if (stateTicks >= SteelLeviathanConstants.INTEREST_WAIT_TICKS) {
            failInterest(true);
        }
    }

    private void tickInterestConsume() {
        ItemEntity item = resolveItem(trackItemUuid);
        if (item == null && consumeTarget == null) {
            beginDepart();
            return;
        }
        if (consumeTarget == null && item != null) {
            consumeTarget = item.position();
        }
        if (consumeTarget == null) {
            beginDepart();
            return;
        }

        setThrustersActive(false);
        setUnderground(false);

        diveTowardPoint(consumeTarget, SteelLeviathanConstants.INTEREST_BURROW_CRUISE_SPEED);

        if (distanceToSqr(consumeTarget) < 4.0D) {
            if (item != null) {
                onSuccessfulFeed(item);
            } else {
                beginDepart();
            }
            return;
        }
        if (stateTicks >= SteelLeviathanConstants.INTEREST_CONSUME_TIMEOUT_TICKS) {
            beginDepart();
        }
    }

    void diveTowardPoint(Vec3 target, double speed) {
        diveTowardPoint(target, speed, SteelLeviathanConstants.INTEREST_YAW_RATE);
    }

    void diveTowardPoint(Vec3 target, double speed, float yawRate) {
        Vec3 delta = target.subtract(position());
        if (delta.lengthSqr() > 1.0E-6D) {
            float yaw = (float) (Mth.atan2(-delta.x, delta.z) * Mth.RAD_TO_DEG);
            setYRot(Mth.approachDegrees(getYRot(), yaw, yawRate));
            double horiz = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
            float pitch = (float) (Mth.atan2(-delta.y, Math.max(horiz, 0.1D)) * Mth.RAD_TO_DEG);
            pitch = Mth.clamp(pitch, -20.0F, SteelLeviathanConstants.INTEREST_BURROW_DIVE_PITCH);
            approachPitch(pitch, SteelLeviathanConstants.INTEREST_PITCH_RATE);
        }
        Vec3 facing = SteelLeviathanSinew.facingFromYawPitch(getYRot(), getBodyPitch());
        if (facing.lengthSqr() < 1.0E-6D) {
            facing = new Vec3(0.0D, -1.0D, 0.0D);
        } else {
            facing = facing.normalize();
        }
        setPos(getX() + facing.x * speed, getY() + facing.y * speed, getZ() + facing.z * speed);
        verticalVel = 0.0D;
    }

    private void tickInterestDepart() {
        departTicks--;
        setThrustersActive(false);
        departCruiseStep(departDesiredYaw,
                SteelLeviathanConstants.INTEREST_DEPART_BURROW_DEPTH,
                SteelLeviathanConstants.INTEREST_DEPART_SPRING_GAIN,
                SteelLeviathanConstants.INTEREST_MOVE_SPEED);
        if (departTicks <= 0) {
            endDepartIntoWander();
        }
    }

    private void endDepartIntoWander() {
        desiredYaw = getYRot();
        headingTicks = 1;

        bobPhase = (float) (Math.PI * 1.5D);
        float bobPitch = -Mth.cos(bobPhase) * SteelLeviathanConstants.BOB_PITCH_AMPLITUDE;
        setBodyPitch(bobPitch);
        burrowSpringDepth = SteelLeviathanConstants.BOB_BURROW_DEEP;
        setUnderground(true);
        setBehaviorState(SteelLeviathanBehaviorState.PASSIVE);
        burrowWander();
    }

    private void beginDepart() {

        float keepYaw = getYRot();
        clearInterest();
        setCurrentMove(SteelLeviathanMove.NONE);
        departDesiredYaw = keepYaw;
        desiredYaw = keepYaw;
        departTicks = SteelLeviathanConstants.INTEREST_POST_FEED_BURROW_TICKS;
        verticalVel = 0.0D;
        setUnderground(true);
        setBehaviorState(SteelLeviathanBehaviorState.INTEREST_DEPART);

        tickInterestDepart();
    }

    void approachPitch(float target, float rate) {
        setLookRotation(getYRot(), Mth.approachDegrees(getBodyPitch(), target, rate));
    }

    Vec3 stableLookPoint(LivingEntity entity) {
        return entity.position().add(0.0D, entity.getBbHeight() * 0.85D, 0.0D);
    }

    private Vec3 stablePlayerLookPoint(Player player) {
        return stableLookPoint(player);
    }

    void departCruiseStep(float yaw, double depth, float springGain, double speed) {
        departCruiseStep(yaw, depth, springGain, speed, SteelLeviathanConstants.INTEREST_YAW_RATE);
    }

    void departCruiseStep(float yaw, double depth, float springGain, double speed, float yawRate) {
        setUnderground(true);
        setYRot(Mth.approachDegrees(getYRot(), yaw, yawRate));
        approachPitch(SteelLeviathanConstants.INTEREST_BURROW_DIVE_PITCH,
                SteelLeviathanConstants.INTEREST_PITCH_RATE);
        burrowSpringDepth = depth;
        Vec3 flat = SteelLeviathanSinew.facingFromYawPitch(getYRot(), 0.0F);
        Vec3 horiz = new Vec3(flat.x, 0.0D, flat.z);
        if (horiz.lengthSqr() < 1.0E-6D) {
            horiz = new Vec3(0.0D, 0.0D, 1.0D);
        } else {
            horiz = horiz.normalize();
        }
        double nextX = getX() + horiz.x * speed;
        double nextZ = getZ() + horiz.z * speed;
        updateGroundGuide(nextX, nextZ);
        double deepY = smoothedGroundY - depth;
        double nextY = getY() + (deepY - getY()) * springGain;
        setPos(nextX, nextY, nextZ);
        verticalVel = 0.0D;
    }

    void interestLookAt(Vec3 desired, float maxPitch) {
        Vec3 delta = desired.subtract(position());
        if (delta.lengthSqr() < 1.0E-6D) {
            return;
        }
        float rawYaw = (float) (Mth.atan2(-delta.x, delta.z) * Mth.RAD_TO_DEG);
        double horiz = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
        float rawPitch = (float) (Mth.atan2(-delta.y, Math.max(horiz, 0.1D)) * Mth.RAD_TO_DEG);
        rawPitch = Mth.clamp(rawPitch, -65.0F, maxPitch);
        float nextYaw = Mth.approachDegrees(getYRot(), rawYaw, SteelLeviathanConstants.LOOK_TRACK_YAW_RATE);
        float nextPitch = Mth.approachDegrees(getBodyPitch(), rawPitch, SteelLeviathanConstants.LOOK_TRACK_PITCH_RATE);
        setLookRotation(nextYaw, nextPitch);
    }

    private void chasePoint(Vec3 target, double speed, MotionMode mode) {
        double dx = target.x - getX();
        double dz = target.z - getZ();
        if (dx * dx + dz * dz > 1.0E-4D) {
            float yaw = (float) (Mth.atan2(-dx, dz) * Mth.RAD_TO_DEG);
            setYRot(Mth.approachDegrees(getYRot(), yaw, headTurnRate()));
        }
        integrateMotion(Math.max(speed, SteelLeviathanConstants.INTEREST_CONSUME_MIN_SPEED), mode);
    }

    private void tickGiftApproach() {
        Player player = resolvePlayer(interestPlayerUuid);
        if (player == null) {
            setBehaviorState(SteelLeviathanBehaviorState.PASSIVE);
            return;
        }
        setUnderground(false);
        moveToward(player.position().add(0, 1, 0), 0.4D, false);
        lookAt(player.position());
        if (distanceToSqr(player) < 36.0D || stateTicks > 100) {
            dropGiftNear(player);
            leavePlayerAlone();
        }
    }

    private void dropGiftNear(Player player) {
        List<Item> gifts = new ArrayList<>();
        var tag = ForgeRegistries.ITEMS.tags();
        if (tag != null) {
            for (Item item : tag.getTag(GIFT_ITEMS)) {
                gifts.add(item);
            }
        }
        if (gifts.isEmpty()) {
            return;
        }
        Item item = gifts.get(this.random.nextInt(gifts.size()));
        int max = Math.min(16, item.getMaxStackSize());
        int count = 1 + (max > 1 ? this.random.nextInt(max) : 0);
        ItemStack stack = new ItemStack(item, count);
        ItemEntity entity = new ItemEntity(this.level(), player.getX(), player.getY() + 1.0D, player.getZ(), stack);
        this.level().addFreshEntity(entity);
    }

    private void failInterest(boolean startBossfight) {
        Player player = resolvePlayer(interestPlayerUuid);
        clearInterest();
        if (startBossfight && player != null) {
            int score = reputation.getScore(player.getUUID());
            if (score >= SteelLeviathanConstants.REP_PACIFIST_THRESHOLD) {
                setBehaviorState(SteelLeviathanBehaviorState.PASSIVE);
                return;
            }
            beginBossfight(player, false);
        } else {
            setBehaviorState(SteelLeviathanBehaviorState.PASSIVE);
        }
    }

    private void leavePlayerAlone() {
        beginDepart();
    }

    private void clearInterest() {
        setInterestTarget(null);
        trackItemUuid = null;
        interestGraceTicks = 0;
        breachAnchor = null;
        interestBreachArrived = false;
        interestDiveDone = false;
        interestDiveTicks = 0;
        interestCruiseTicks = 0;
        interestRiseTicks = 0;
        consumeTarget = null;
        setHologram(ItemStack.EMPTY);
    }

    private boolean isInterestBehavior() {
        return switch (getBehaviorState()) {
            case INTEREST_APPROACH, INTEREST_SCAN, INTEREST_WAIT, INTEREST_CONSUME, INTEREST_DEPART -> true;
            default -> false;
        };
    }

    public void beginBossfight(LivingEntity target, boolean fromAttack) {
        bossCombat.begin(target, fromAttack);
    }

    private void tickBossfight() {
        bossCombat.tick();
    }

    void setBossBarVisible(boolean visible) {
        this.entityData.set(BOSS_BAR_VISIBLE, visible);
    }

    void setPhaseTwo(boolean phaseTwo) {
        boolean entered = phaseTwo && !isPhaseTwo();
        this.entityData.set(PHASE_TWO, phaseTwo);
        if (entered && !this.level().isClientSide) {
            for (SteelLeviathanPartEntity part : collectParts()) {
                if (part.getPartKind() == PartKind.TAIL
                        && part.missilePendingMask == 0
                        && part.getMissileReleasedMask() == 0) {
                    part.beginTailMissileRelease();
                }
            }
        }
    }

    SteelLeviathanReputation getReputation() {
        return reputation;
    }

    net.minecraft.util.RandomSource getBossRandom() {
        return this.random;
    }

    void tryHeadHit(LivingEntity target) {
        if (hitCooldown > 0 || target == null) {
            return;
        }
        if (target.getBoundingBox().intersects(getBoundingBox().inflate(0.5D))) {
            target.hurt(DamageTypeRegistry.getSimpleDamageSource(this.level(), DamageTypeRegistry.STEEL_LEVIATHAN_LUNGE),
                    SteelLeviathanConstants.MOVE_HIT_DAMAGE);
            hitCooldown = SteelLeviathanConstants.MOVE_HIT_COOLDOWN_TICKS;
        }
    }

    void startDeathPublic(LivingEntity target) {
        startDeath(target);
    }

    private void startDeath(LivingEntity target) {
        setBehaviorState(SteelLeviathanBehaviorState.DEAD);
        this.entityData.set(DYING, true);
        deathTicks = 0;
        setCurrentMove(SteelLeviathanMove.DEATH);
        setThrustersActive(true);
        setDeltaMovement(Vec3.ZERO);
        deathTarget = target;
        deathTargetUuid = target != null ? target.getUUID() : null;
        if (!this.level().isClientSide) {
            clearMissileReleaseState();
            missilePendingMask = (byte) ((1 << SteelLeviathanModelBones.MAW_MISSILE_COUNT) - 1);
            missileLaunchCooldown = 0;
        }
    }

    private void tickDeath() {
        deathTicks++;
        setDeltaMovement(Vec3.ZERO);
        if (!this.level().isClientSide) {
            tickDeathMissileRelease();
        }
    }

    private void tickDeathMissileRelease() {
        if (missilePendingMask == 0) {
            return;
        }
        if (missileLaunchCooldown > 0) {
            missileLaunchCooldown--;
            return;
        }
        List<Integer> pending = new ArrayList<>();
        for (int i = 0; i < SteelLeviathanModelBones.MAW_MISSILE_COUNT; i++) {
            if ((missilePendingMask & (1 << i)) != 0) {
                pending.add(i);
            }
        }
        if (pending.isEmpty()) {
            return;
        }
        int slot = pending.get(this.random.nextInt(pending.size()));
        spawnMawMissile(slot);
        missilePendingMask &= (byte) ~(1 << slot);
        setMissileReleasedMask((byte) (getMissileReleasedMask() | (1 << slot)));
        if (missilePendingMask == 0) {
            explodeChain();
            return;
        }
        missileLaunchCooldown = SteelLeviathanConstants.MISSILE_LAUNCH_INTERVAL;
    }

    private void spawnMawMissile(int slot) {
        Vec3 pos = localToWorld(SteelLeviathanModelBones.mawMissileLocal(slot));
        BurrowMissileEntity missile = new BurrowMissileEntity(EntityRegistry.BURROW_MISSILE.get(), this.level());
        missile.setPos(pos.x, pos.y, pos.z);
        missile.setOwner(this);
        missile.setTarget(resolveDeathTarget());
        Vec3 dir = localToWorldDir(SteelLeviathanModelBones.mawMissileLaunchLocal(slot));
        if (dir.lengthSqr() < 1.0E-6D) {
            dir = SteelLeviathanSinew.facingFromYawPitch(getYRot(), getBodyPitch());
        }
        missile.launchInDirection(dir);
        this.level().addFreshEntity(missile);
        this.level().playSound(null, pos.x, pos.y, pos.z,
                SoundRegistry.STEEL_LEVIATHAN_MISSILE_LAUNCH.get(), SoundSource.HOSTILE, SteelLeviathanConstants.SOUND_VOLUME_64, 1.0F);
    }

    @Nullable
    private LivingEntity resolveDeathTarget() {
        if (deathTarget != null && deathTarget.isAlive()) {
            return deathTarget;
        }
        if (deathTargetUuid == null) {
            return getCombatTarget();
        }
        for (LivingEntity entity : this.level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(128.0D))) {
            if (entity.getUUID().equals(deathTargetUuid)) {
                deathTarget = entity;
                return entity;
            }
        }
        return getCombatTarget();
    }

    private void explodeChain() {
        if (this.level() instanceof ServerLevel server) {
            server.playSound(null, getX(), getY(), getZ(),
                    SoundRegistry.STEEL_LEVIATHAN_DEATH.get(), SoundSource.HOSTILE, SteelLeviathanConstants.SOUND_VOLUME_128, 1.0F);
            List<SteelLeviathanPartEntity> parts = collectParts();
            for (SteelLeviathanPartEntity part : parts) {
                spawnBloodBurst(server, part.position());
            }
            spawnDeathLoot(server);
            for (SteelLeviathanPartEntity part : parts) {
                if (part != this) {
                    part.discard();
                }
            }
            ResourceLocation advId = ResourceLocation.fromNamespaceAndPath(
                    destiny.null_ouroboros.NullOuroboros.MODID, "steel_leviathan_defeat");
            Advancement advancement = server.getServer().getAdvancements().getAdvancement(advId);
            if (advancement != null) {
                for (ServerPlayer player : server.getEntitiesOfClass(ServerPlayer.class, getBoundingBox().inflate(128.0D))) {
                    AdvancementProgress progress = player.getAdvancements().getOrStartProgress(advancement);
                    if (!progress.isDone()) {
                        for (String criterion : progress.getRemainingCriteria()) {
                            player.getAdvancements().award(advancement, criterion);
                        }
                    }
                }
            }
        }
        discard();
    }

    private void spawnDeathLoot(ServerLevel server) {
        LivingEntity killer = resolveDeathTarget();
        for (SteelLeviathanPartEntity part : collectParts()) {
            ResourceLocation tableId = deathLootTableFor(part);
            Vec3 dropPos = lootDropPos(server, part);
            LootParams.Builder builder = new LootParams.Builder(server)
                    .withParameter(LootContextParams.ORIGIN, dropPos)
                    .withParameter(LootContextParams.THIS_ENTITY, part)
                    .withParameter(LootContextParams.DAMAGE_SOURCE, server.damageSources().generic())
                    .withOptionalParameter(LootContextParams.KILLER_ENTITY, killer);
            if (killer instanceof Player player) {
                builder.withOptionalParameter(LootContextParams.LAST_DAMAGE_PLAYER, player);
            }
            LootParams params = builder.create(LootContextParamSets.ENTITY);
            LootTable table = server.getServer().getLootData().getLootTable(tableId);
            boolean clampToGround = part.isUnderground();
            for (ItemStack stack : table.getRandomItems(params)) {
                if (stack.isEmpty()) {
                    continue;
                }
                while (!stack.isEmpty()) {
                    int pieceCount = Math.min(stack.getCount(), 1 + random.nextInt(3));
                    ItemStack piece = stack.split(pieceCount);
                    spawnLootItem(server, dropPos, piece, clampToGround);
                }
            }
        }
    }

    private static ResourceLocation deathLootTableFor(SteelLeviathanPartEntity part) {
        return switch (part.getPartKind()) {
            case HEAD -> SteelLeviathanConstants.DEATH_LOOT_TABLE_HEAD;
            case SEGMENT -> SteelLeviathanConstants.DEATH_LOOT_TABLE_SEGMENT;
            case TAIL -> SteelLeviathanConstants.DEATH_LOOT_TABLE_TAIL;
        };
    }

    private Vec3 lootDropPos(ServerLevel server, SteelLeviathanPartEntity part) {
        Vec3 pos = part.position();
        if (!part.isUnderground()) {
            return pos;
        }
        int groundY = server.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Mth.floor(pos.x), Mth.floor(pos.z));
        if (groundY <= server.getMinBuildHeight()) {
            return pos;
        }
        return new Vec3(pos.x, groundY + 0.25D, pos.z);
    }

    private void spawnLootItem(ServerLevel server, Vec3 pos, ItemStack stack, boolean clampToGround) {
        double spread = SteelLeviathanConstants.DEATH_LOOT_SPREAD;
        double speed = SteelLeviathanConstants.DEATH_LOOT_SPEED;
        double ox = (random.nextDouble() * 2.0D - 1.0D) * spread;
        double oy = (random.nextDouble() * 2.0D - 1.0D) * spread;
        double oz = (random.nextDouble() * 2.0D - 1.0D) * spread;
        double vx;
        double vy;
        double vz;
        do {
            vx = random.nextGaussian();
            vy = random.nextGaussian();
            vz = random.nextGaussian();
        } while (vx * vx + vy * vy + vz * vz < 1.0E-6D);
        double invLen = speed / Math.sqrt(vx * vx + vy * vy + vz * vz);
        vx *= invLen;
        vy *= invLen;
        vz *= invLen;
        double x = pos.x + ox;
        double y = pos.y + oy;
        double z = pos.z + oz;
        if (clampToGround) {
            int groundY = server.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Mth.floor(x), Mth.floor(z));
            if (groundY > server.getMinBuildHeight()) {
                y = Math.max(y, groundY + 0.25D);
            }
        }
        ItemEntity entity = new ItemEntity(server, x, y, z, stack);
        entity.setDeltaMovement(vx, vy, vz);
        entity.setDefaultPickUpDelay();
        server.addFreshEntity(entity);
    }

    private void spawnBloodBurst(ServerLevel server, Vec3 pos) {
        double spread = SteelLeviathanConstants.DEATH_BLOOD_SPREAD;
        double speed = SteelLeviathanConstants.DEATH_BLOOD_SPEED;
        for (int i = 0; i < SteelLeviathanConstants.DEATH_BLOOD_COUNT; i++) {
            double ox = (random.nextDouble() * 2.0D - 1.0D) * spread;
            double oy = (random.nextDouble() * 2.0D - 1.0D) * spread;
            double oz = (random.nextDouble() * 2.0D - 1.0D) * spread;
            double vx;
            double vy;
            double vz;
            do {
                vx = random.nextGaussian();
                vy = random.nextGaussian();
                vz = random.nextGaussian();
            } while (vx * vx + vy * vy + vz * vz < 1.0E-6D);
            double invLen = speed / Math.sqrt(vx * vx + vy * vy + vz * vz);
            vx *= invLen;
            vy *= invLen;
            vz *= invLen;
            double x = pos.x + ox;
            double y = pos.y + oy;
            double z = pos.z + oz;
            double rangeSq = 64.0D * 64.0D;
            for (ServerPlayer player : server.players()) {
                if (player.distanceToSqr(x, y, z) > rangeSq) {
                    continue;
                }
                server.sendParticles(player, ParticleTypeRegistry.BLOOD.get(), true,
                        x, y, z, 0, vx, vy, vz, 1.0);
            }
        }
    }

    private void tickMawDrills() {
        Vec3 maw = mawDrillWorldPos();
        double r = getBehaviorState() == SteelLeviathanBehaviorState.INTEREST_CONSUME ? 2.0D : 1.2D;
        AABB box = new AABB(maw.x - r, maw.y - r, maw.z - r, maw.x + r, maw.y + r, maw.z + r);
        for (ItemEntity item : this.level().getEntitiesOfClass(ItemEntity.class, box)) {
            ItemStack stack = item.getItem();
            boolean desired = !stack.isEmpty() && stack.is(DESIRED_ITEMS)
                    && (desiredStack == null || desiredStack.isEmpty() || ItemStack.isSameItem(stack, desiredStack));

            boolean inConsume = getBehaviorState() == SteelLeviathanBehaviorState.INTEREST_CONSUME;
            if (desired && inConsume) {
                onSuccessfulFeed(item);
            }
            item.discard();
        }
    }

    private void onSuccessfulFeed(ItemEntity item) {
        Player player = resolvePlayer(interestPlayerUuid);
        if (player != null) {
            reputation.addScore(player.getUUID(), 1);
        }
        setMainHealth(getMainHealth() + SteelLeviathanConstants.MAX_HEALTH * 0.1F);
        repairRandomBrokenHeatsink();
        feedCount++;
        if (feedCount % 4 == 0) {
            restoreRandomVulnerableSegment();
        }
        item.discard();
        leavePlayerAlone();
    }

    private void repairRandomBrokenHeatsink() {
        List<SteelLeviathanPartEntity> parts = collectParts();
        List<SteelLeviathanPartEntity> candidates = new ArrayList<>();
        for (SteelLeviathanPartEntity part : parts) {
            if (!part.isVulnerable() && part.getHeatsinkDestroyedMask() != 0) {
                candidates.add(part);
            }
        }
        if (candidates.isEmpty()) {
            return;
        }
        candidates.get(this.random.nextInt(candidates.size())).repairRandomHeatsink(this.random);
    }

    private void restoreRandomVulnerableSegment() {
        List<SteelLeviathanPartEntity> vulnerable = new ArrayList<>();
        for (SteelLeviathanPartEntity part : collectParts()) {
            if (part.isVulnerable()) {
                vulnerable.add(part);
            }
        }
        if (vulnerable.isEmpty()) {
            return;
        }
        vulnerable.get(this.random.nextInt(vulnerable.size())).restoreArmor();
    }

    public List<SteelLeviathanPartEntity> collectParts() {
        List<SteelLeviathanPartEntity> parts = new ArrayList<>();
        parts.add(this);
        SteelLeviathanPartEntity current = resolveNext();
        int guard = 0;
        while (current != null && guard++ < 64) {
            parts.add(current);
            current = current.resolveNext();
        }
        return parts;
    }

    public void rememberChainChunk(long chunkKey) {
        chainChunkKeys.add(chunkKey);
    }

    public LongSet getChainChunkKeys() {
        return chainChunkKeys;
    }

    LongOpenHashSet getChainChunkKeysMutable() {
        return chainChunkKeys;
    }

    public void refreshChainChunksFromLoadedParts() {
        chainChunkKeys.clear();
        for (SteelLeviathanPartEntity part : collectParts()) {
            chainChunkKeys.add(ChunkPos.asLong(part.chunkPosition().x, part.chunkPosition().z));
            part.addChunkHints(chainChunkKeys);
        }
    }

    public boolean damageMainHealth(DamageSource source, float amount, SteelLeviathanPartEntity from) {
        if (amount > SteelLeviathanConstants.ANTIBUTCHER_THRESHOLD) {
            return false;
        }
        if (!from.isVulnerable()) {
            return false;
        }
        onPartAttacked(source);
        float next = getMainHealth() - amount;
        setMainHealth(next);
        if (next <= 0.0F) {
            LivingEntity attacker = null;
            if (source.getEntity() instanceof LivingEntity living) {
                attacker = living;
            }
            startDeath(attacker != null ? attacker : getCombatTarget());
        }
        return true;
    }

    public void onPartAttacked(DamageSource source) {
        if (!(source.getEntity() instanceof LivingEntity living)) {
            return;
        }
        if (getBehaviorState() == SteelLeviathanBehaviorState.BOSSFIGHT
                || getBehaviorState() == SteelLeviathanBehaviorState.DEAD) {
            return;
        }
        if (living instanceof Player player) {
            int score = reputation.getScore(player.getUUID());
            if (score >= SteelLeviathanConstants.REP_PACIFIST_THRESHOLD) {

                reputation.addScore(player.getUUID(), -1);
                if (reputation.getScore(player.getUUID()) >= SteelLeviathanConstants.REP_PACIFIST_THRESHOLD) {
                    return;
                }
            }
        }
        clearInterest();
        beginBossfight(living, true);
    }

    private void integrateMotion(double speed, MotionMode mode) {
        Vec3 facing = SteelLeviathanSinew.facingFromYawPitch(getYRot(), getBodyPitch());
        if (facing.lengthSqr() < 1.0E-6D) {
            facing = new Vec3(0.0D, 0.0D, 1.0D);
        } else {
            facing = facing.normalize();
        }

        double nextX = getX() + facing.x * speed;
        double nextY = getY() + facing.y * speed;
        double nextZ = getZ() + facing.z * speed;

        boolean inAir = isHeadInAir();
        boolean thrusters = areThrustersActive() || isAerialMove();
        boolean cresting = mode == MotionMode.AIR && getBodyPitch() < -10.0F;

        boolean freeFall = inAir && !thrusters && !cresting
                && mode != MotionMode.BURROW && mode != MotionMode.DIVE
                && getBehaviorState() != SteelLeviathanBehaviorState.INTEREST_CONSUME
                && getBehaviorState() != SteelLeviathanBehaviorState.INTEREST_DEPART;

        if (freeFall) {
            verticalVel = Math.min(verticalVel + SteelLeviathanConstants.GRAVITY_ACCEL,
                    SteelLeviathanConstants.GRAVITY_MAX_FALL);
            nextY -= verticalVel;
            setBodyPitch(Mth.approachDegrees(getBodyPitch(),
                    SteelLeviathanConstants.GRAVITY_NOSE_DOWN_PITCH,
                    SteelLeviathanConstants.GRAVITY_PITCH_RATE));
        } else if (!inAir) {
            verticalVel = 0.0D;
        }

        if (mode == MotionMode.BURROW || mode == MotionMode.DIVE) {
            updateGroundGuide(nextX, nextZ);
            double targetBand = smoothedGroundY - burrowSpringDepth;
            double dy = targetBand - nextY;
            if (Math.abs(dy) > SteelLeviathanConstants.BURROW_SPRING_DEADZONE) {
                nextY += dy * SteelLeviathanConstants.BURROW_SPRING_GAIN;
            }
            setUnderground(true);
        } else if (mode == MotionMode.SURFACE) {
            setUnderground(false);
            if (!inAir) {
                updateGroundGuide(nextX, nextZ);
                double surfaceBand = smoothedGroundY + SteelLeviathanConstants.SURFACE_GROUND_OFFSET;
                nextY += (surfaceBand - nextY) * SteelLeviathanConstants.SURFACE_SPRING_GAIN;
            }
        } else {
            setUnderground(false);
        }

        setPos(nextX, nextY, nextZ);
    }

    private void steerTowardPoint(Vec3 target, double speed, MotionMode mode,
                                  double arrivalRadius, boolean steerPitch) {
        if (isAerialMove()) {
            Vec3 delta = target.subtract(position());
            if (delta.lengthSqr() > 1.0E-4D) {
                steerYawToward(target, true);
                Vec3 step = delta.normalize().scale(speed);
                setPos(getX() + step.x, getY() + step.y, getZ() + step.z);
            }
            setUnderground(false);
            return;
        }

        double dx = target.x - getX();
        double dz = target.z - getZ();
        double horizSq = dx * dx + dz * dz;
        double arrivalSq = arrivalRadius * arrivalRadius;
        if (horizSq > arrivalSq) {
            float yaw = (float) (Mth.atan2(-dx, dz) * Mth.RAD_TO_DEG);
            setYRot(Mth.approachDegrees(getYRot(), yaw, headTurnRate()));
            if (steerPitch) {
                double horiz = Math.sqrt(horizSq);
                float pitch = (float) (Mth.atan2(-(target.y - getY()), Math.max(horiz, 0.1D)) * Mth.RAD_TO_DEG);
                pitch = Mth.clamp(pitch, -65.0F, 70.0F);
                float pitchRate = isInterestBehavior()
                        ? SteelLeviathanConstants.INTEREST_PITCH_RATE
                        : 3.0F;
                setBodyPitch(Mth.approachDegrees(getBodyPitch(), pitch, pitchRate));
            }
            integrateMotion(speed, mode);
        } else {
            integrateMotion(0.0D, mode);
        }
    }

    private void moveToward(Vec3 target, double speed, boolean undergroundMotion) {
        burrowSpringDepth = SteelLeviathanConstants.BURROW_GROUND_DEPTH;
        MotionMode mode = undergroundMotion ? MotionMode.BURROW : MotionMode.SURFACE;
        if (undergroundMotion) {
            setUnderground(true);
        }
        steerTowardPoint(target, speed, mode, 2.0D, false);
    }

    private void snapToGround(boolean burrowing) {
        if (isAerialMove()) {
            return;
        }
        smoothedGroundY = findGroundY(getX(), getZ());
        double y = burrowing
                ? smoothedGroundY - SteelLeviathanConstants.BURROW_GROUND_DEPTH
                : smoothedGroundY + SteelLeviathanConstants.SURFACE_GROUND_OFFSET;
        setPos(getX(), y, getZ());
        verticalVel = 0.0D;
    }

    private float headTurnRate() {
        if (isInterestBehavior()) {
            return SteelLeviathanConstants.INTEREST_YAW_RATE;
        }
        return getBehaviorState() == SteelLeviathanBehaviorState.PASSIVE
                ? SteelLeviathanConstants.HEAD_TURN_RATE_PASSIVE
                : SteelLeviathanConstants.HEAD_TURN_RATE_COMBAT;
    }

    private void steerYawToward(Vec3 target, boolean allowPitch) {
        Vec3 delta = target.subtract(position());
        if (delta.lengthSqr() < 1.0E-6D) {
            return;
        }
        float desiredYaw = (float) (Mth.atan2(-delta.x, delta.z) * Mth.RAD_TO_DEG);
        float nextYaw = Mth.approachDegrees(getYRot(), desiredYaw, headTurnRate());
        if (allowPitch) {
            float pitch = (float) (Mth.atan2(-delta.y, Math.sqrt(delta.x * delta.x + delta.z * delta.z)) * Mth.RAD_TO_DEG);
            setLookRotation(nextYaw, pitch);
        } else {
            setYRot(nextYaw);
        }
    }

    private void lookAt(Vec3 target) {
        lookAt(target, true);
    }

    private void lookAt(Vec3 target, boolean allowPitch) {
        steerYawToward(target, allowPitch);
    }

    @Nullable
    private Player resolvePlayer(@Nullable UUID uuid) {
        if (uuid == null) {
            return null;
        }
        Player player = this.level().getPlayerByUUID(uuid);
        return player != null && player.isAlive() ? player : null;
    }

    @Nullable
    private ItemEntity resolveItem(@Nullable UUID uuid) {
        if (uuid == null) {
            return null;
        }
        for (ItemEntity item : this.level().getEntitiesOfClass(ItemEntity.class, getBoundingBox().inflate(64.0D))) {
            if (item.getUUID().equals(uuid) && item.isAlive()) {
                return item;
            }
        }
        return null;
    }

    @Nullable
    private ItemEntity findTrackedDesiredItem(@Nullable Player player) {
        AABB box = player != null ? player.getBoundingBox().inflate(32.0D) : getBoundingBox().inflate(32.0D);
        ItemEntity bestMatch = null;
        ItemEntity bestAny = null;
        double bestMatchDist = Double.MAX_VALUE;
        double bestAnyDist = Double.MAX_VALUE;
        UUID playerId = player != null ? player.getUUID() : null;
        Vec3 origin = player != null ? player.position() : position();

        for (ItemEntity item : this.level().getEntitiesOfClass(ItemEntity.class, box)) {
            ItemStack stack = item.getItem();
            if (stack.isEmpty() || !stack.is(DESIRED_ITEMS) || !item.isAlive()) {
                continue;
            }
            double dist = item.distanceToSqr(origin.x, origin.y, origin.z);
            boolean ownerMatch = playerId != null && playerId.equals(item.getOwner());

            boolean hologramMatch = desiredStack != null && !desiredStack.isEmpty()
                    && ItemStack.isSameItem(stack, desiredStack);
            if (hologramMatch) {
                double score = ownerMatch ? dist * 0.25D : dist;
                if (score < bestMatchDist) {
                    bestMatchDist = score;
                    bestMatch = item;
                }
            } else {
                double score = ownerMatch ? dist * 0.25D : dist;
                if (score < bestAnyDist) {
                    bestAnyDist = score;
                    bestAny = item;
                }
            }
        }
        return bestMatch != null ? bestMatch : bestAny;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!this.level().isClientSide()) {
            return false;
        }

        if (this.invulnerableTime > 0) {
            return false;
        }

        if (amount > SteelLeviathanConstants.ANTIBUTCHER_THRESHOLD) {
            return false;
        }

        if (!isVulnerable()) {
            return super.hurt(source, amount);
        }

        boolean damaged = damageMainHealth(source, amount, this);

        if (damaged) {
            this.invulnerableTime = 20;
        }

        return damaged;
    }

    @Override
    public void remove(RemovalReason reason) {

        if (!this.level().isClientSide
                && this.level() instanceof net.minecraft.server.level.ServerLevel serverLevel
                && (reason == RemovalReason.KILLED || reason == RemovalReason.DISCARDED)) {
            SteelLeviathanChunkTickets.release(serverLevel, getUUID());
            for (UUID bodyId : bodyUuids) {
                Entity entity = serverLevel.getEntity(bodyId);
                if (entity instanceof SteelLeviathanPartEntity part && part != this && !part.isRemoved()) {
                    part.discard();
                }
            }
            for (SteelLeviathanPartEntity part : collectParts()) {
                if (part != this && !part.isRemoved()) {
                    part.discard();
                }
            }
        }
        super.remove(reason);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setMainHealth(tag.contains("MainHealth") ? tag.getFloat("MainHealth") : SteelLeviathanConstants.MAX_HEALTH);
        setBehaviorState(SteelLeviathanBehaviorState.values()[Mth.clamp(tag.getInt("Behavior"), 0, SteelLeviathanBehaviorState.values().length - 1)]);
        setCurrentMove(SteelLeviathanMove.values()[Mth.clamp(tag.getInt("Move"), 0, SteelLeviathanMove.values().length - 1)]);
        this.entityData.set(PHASE_TWO, tag.getBoolean("PhaseTwo"));
        this.entityData.set(BOSS_BAR_VISIBLE, tag.getBoolean("BossBar"));
        this.entityData.set(CHAIN_LENGTH, tag.getInt("ChainLength"));
        this.entityData.set(CHAIN_SPAWNED, tag.getBoolean("ChainSpawned"));
        this.entityData.set(DYING, tag.getBoolean("Dying"));
        bossCombat.load(tag);
        feedCount = tag.getInt("FeedCount");
        chainInitialized = this.entityData.get(CHAIN_SPAWNED);
        reputation.load(tag);
        bodyUuids.clear();
        if (tag.contains("BodyUuids")) {
            CompoundTag bodies = tag.getCompound("BodyUuids");
            for (String key : bodies.getAllKeys()) {
                bodyUuids.add(bodies.getUUID(key));
            }
        }
        if (tag.hasUUID("CombatTarget")) {
            combatTargetUuid = tag.getUUID("CombatTarget");
        }
        if (tag.hasUUID("DeathTarget")) {
            deathTargetUuid = tag.getUUID("DeathTarget");
        }
        if (tag.contains("HologramItem")) {
            this.entityData.set(HOLOGRAM_ITEM, tag.getString("HologramItem"));
        }
        chainChunkKeys.clear();
        naturalSpawn = tag.getBoolean("NaturalSpawn");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("MainHealth", getMainHealth());
        tag.putInt("Behavior", getBehaviorState().ordinal());
        tag.putInt("Move", getCurrentMove().ordinal());
        tag.putBoolean("PhaseTwo", isPhaseTwo());
        tag.putBoolean("BossBar", isBossBarVisible());
        tag.putInt("ChainLength", this.entityData.get(CHAIN_LENGTH));
        tag.putBoolean("ChainSpawned", this.entityData.get(CHAIN_SPAWNED));
        tag.putBoolean("Dying", isDying());
        bossCombat.save(tag);
        tag.putInt("FeedCount", feedCount);
        reputation.save(tag);
        CompoundTag bodies = new CompoundTag();
        for (int i = 0; i < bodyUuids.size(); i++) {
            bodies.putUUID(Integer.toString(i), bodyUuids.get(i));
        }
        tag.put("BodyUuids", bodies);
        if (combatTargetUuid != null) {
            tag.putUUID("CombatTarget", combatTargetUuid);
        }
        if (deathTargetUuid != null) {
            tag.putUUID("DeathTarget", deathTargetUuid);
        }
        tag.putString("HologramItem", this.entityData.get(HOLOGRAM_ITEM));
        tag.putLongArray("ChainChunks", chainChunkKeys.toLongArray());
        tag.putBoolean("NaturalSpawn", naturalSpawn);
    }

}

