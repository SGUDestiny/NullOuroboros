package destiny.null_ouroboros.server.capability;

import destiny.null_ouroboros.NullOuroboros;
import destiny.null_ouroboros.client.network.ClientBoundManifoldingPacket;
import destiny.null_ouroboros.client.network.ClientBoundParticlePacket;
import destiny.null_ouroboros.server.block.MechanicalSirenBlock;
import destiny.null_ouroboros.server.block.entity.MechanicalSirenBlockEntity;
import destiny.null_ouroboros.server.block.entity.TemporalSurgeDetectorBlockEntity;
import destiny.null_ouroboros.server.entity.BurrowBeaconEntity;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import destiny.null_ouroboros.server.manifolding.ManifoldingWindScan;
import destiny.null_ouroboros.server.registry.DamageTypeRegistry;
import destiny.null_ouroboros.server.registry.PacketHandlerRegistry;
import destiny.null_ouroboros.server.registry.ParticleTypeRegistry;
import destiny.null_ouroboros.server.registry.SoundRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.network.PacketDistributor;

import java.util.*;

public class ManifoldingCapability implements INBTSerializable<CompoundTag> {
    public static final ResourceLocation DIMENSION_ID = ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, "verge_of_reality");

    private static final String PHASE = "phase";
    private static final String TIME_UNTIL_NEXT_EVENT = "time_until_next_event";
    private static final String START_TIME = "start_time";
    private static final String PRE_EVENT_DURATION = "pre_event_duration";
    private static final String ACTIVE_DURATION = "active_duration";
    private static final String POST_EVENT_DURATION = "post_event_duration";
    private static final String WIND_ANGLE = "wind_angle";
    private static final String SIREN_POSITIONS = "siren_positions";
    private static final String PENDING_SIREN_TRIGGERS = "pending_siren_triggers";
    private static final String PENDING_LOOPS = "loops";
    private static final String PENDING_DELAY = "delay";
    private static final String PENDING_POS = "pos";

    public static final TagKey<Block> DOESNT_PROTECT_FROM_MANIFOLDING = BlockTags.create(ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, "doesnt_protect_from_manifolding"));
    public static final TagKey<Block> ISNT_CONVERTED_BY_MANIFOLDING = BlockTags.create(ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, "isnt_converted_by_manifolding"));

    public static final int SIREN_RADIUS = 256;
    public static final float WIND_PUSH_FORCE = 0.5f;
    public static final int DAMAGE_INTERVAL = 20;

    public static final int CLEAR_DELAY_MIN = 1 * 60 * 20;
    public static final int CLEAR_DELAY_MAX = 1 * 60 * 20;
    public static final int PRE_EVENT_MIN = 20 * 20;
    public static final int PRE_EVENT_MAX = 20 * 20;
    public static final int ACTIVE_MIN = 60 * 20;
    public static final int ACTIVE_MAX = 60 * 20;
    public static final int POST_EVENT_MIN = 20 * 20;
    public static final int POST_EVENT_MAX = 20 * 20;
    public static final int THUNDER_DELAY_MIN = 1 * 20;
    public static final int THUNDER_DELAY_MAX = 7 * 20;

    public static final double BEACON_PROTECTION_RANGE = 8;
    public static final float BEACON_PUSH_MULTIPLIER = 0.05f;
    public static final int MANIFOLDING_ASH_LIFETIME = 60;

    private ManifoldingPhase phase = ManifoldingPhase.CLEAR;
    private int timeUntilNextEvent = 0;
    private long startTime = 0;
    private int preEventDuration = 0;
    private int activeDuration = 0;
    private int postEventDuration = 0;
    private float windAngle = 0;

    private float thunderPulse = 0f;
    private int thunderTimer = 0;
    private int pulseTicks = -1;
    private int riftTicks = -1;

    private final Set<BlockPos> loadedDetectors = new HashSet<>();
    private final Set<BlockPos> sirenPositions = new HashSet<>();
    private final Map<BlockPos, PendingSirenTrigger> pendingSirenTriggers = new HashMap<>();
    private final Map<UUID, Boolean> playerExposed = new HashMap<>();
    private final LongOpenHashSet erodedChunksThisEvent = new LongOpenHashSet();

    private record PendingSirenTrigger(int loops, int delay) {}

    private boolean pendingSirenRevalidation = false;

    public void scheduleSirenRevalidation() {
        pendingSirenRevalidation = true;
    }

    public void revalidateSirenPositions(ServerLevel level) {
        List<BlockPos> stale = new ArrayList<>();
        for (BlockPos pos : sirenPositions) {
            if (!level.isLoaded(pos)) {
                level.getChunk(pos);
            }
            if (!(level.getBlockState(pos).getBlock() instanceof MechanicalSirenBlock)) {
                stale.add(pos);
            }
        }
        for (BlockPos pos : stale) {
            removeSiren(pos);
        }
        pendingSirenTriggers.keySet().removeIf(pos -> !sirenPositions.contains(pos));
    }

    public void init(ServerLevel level) {
        if (!level.isClientSide) {
            if (phase == ManifoldingPhase.CLEAR && timeUntilNextEvent <= 0) {
                randomizeNextEventDurations(level);
            }
        }
    }

    public void serverTick(ServerLevel level) {
        if (!level.dimension().location().equals(DIMENSION_ID)) return;

        if (pendingSirenRevalidation) {
            pendingSirenRevalidation = false;
            revalidateSirenPositions(level);
        }

        init(level);

        long now = level.getGameTime();
        switch (phase) {
            case CLEAR -> {
                if (timeUntilNextEvent > 0) {
                    timeUntilNextEvent--;
                    if (timeUntilNextEvent <= 0) {
                        transitionPhase(ManifoldingPhase.PRE_EVENT, level, now);
                    }
                }
            }
            case PRE_EVENT -> {
                long elapsed = now - startTime;
                if (elapsed >= preEventDuration) {
                    transitionPhase(ManifoldingPhase.ACTIVE, level, now);
                }
            }
            case ACTIVE -> {
                long elapsed = now - startTime;
                if (elapsed >= activeDuration) {
                    transitionPhase(ManifoldingPhase.POST_EVENT, level, now);
                }
            }
            case POST_EVENT -> {
                long elapsed = now - startTime;
                if (elapsed >= postEventDuration) {
                    transitionPhase(ManifoldingPhase.CLEAR, level, now);
                }
            }
        }

        if (phase != ManifoldingPhase.CLEAR) {
            spawnWindParticles(level);
            applyDamageToAllEntities(level);

            if (phase != ManifoldingPhase.POST_EVENT) {
                if (thunderTimer > 0) {
                    thunderTimer--;
                }
                if (thunderTimer <= 0) {
                    playThunder(level);
                }
            }

            if (pulseTicks >= 0) {
                pulseTicks++;
                if (pulseTicks <= 5) {
                    thunderPulse = pulseTicks / 5f;
                } else if (pulseTicks <= 10) {
                    thunderPulse = (10 - pulseTicks) / 5f;
                } else {
                    thunderPulse = 0;
                    pulseTicks = -1;
                }
            }
        } else {
            thunderPulse = 0;
            pulseTicks = -1;
            thunderTimer = 0;
        }

        if (riftTicks >= 0) {
            riftTicks++;
            if (riftTicks > 20) {
                riftTicks = -1;
            }
        }

        syncToClients(level);
    }

    private void transitionPhase(ManifoldingPhase newPhase, ServerLevel level, long now) {
        phase = newPhase;
        startTime = now;

        switch (newPhase) {
            case PRE_EVENT -> {
                erodedChunksThisEvent.clear();
                windAngle = level.random.nextFloat() * 360;

                activateSirens(level);
                playThunder(level);
            }
            case ACTIVE -> {
                triggerDetectors(level);
            }
            case POST_EVENT -> {
                triggerDetectors(level);
            }
            case CLEAR -> {
                erodedChunksThisEvent.clear();
                randomizeNextEventDurations(level);
            }
        }
    }

    private void playThunder(ServerLevel level) {
        for (Player player : level.players()) {
            BlockPos playerPos = player.blockPosition();

            double angle = level.random.nextDouble() * Math.PI * 2;
            double dist  = level.random.nextDouble() * 256;

            int x = (int) (playerPos.getX() + Math.cos(angle) * dist);
            int z = (int) (playerPos.getZ() + Math.sin(angle) * dist);
            int y = playerPos.getY() + level.random.nextInt(10);

            BlockPos soundPos = new BlockPos(x, y, z);

            level.playSound(null, soundPos, SoundRegistry.MANIFOLDING_THUNDER.get(), SoundSource.WEATHER, 10000, 0.8f + level.random.nextFloat() * 0.4f);
        }

        thunderTimer = level.random.nextInt(THUNDER_DELAY_MIN, THUNDER_DELAY_MAX + 1);
        pulseTicks = 0;
        riftTicks = 0;
    }

    private void randomizeNextEventDurations(ServerLevel level) {
        preEventDuration = level.random.nextInt(PRE_EVENT_MIN, PRE_EVENT_MAX + 1);
        activeDuration = level.random.nextInt(ACTIVE_MIN, ACTIVE_MAX + 1);
        postEventDuration = level.random.nextInt(POST_EVENT_MIN, POST_EVENT_MAX + 1);
        timeUntilNextEvent = level.random.nextInt(CLEAR_DELAY_MIN, CLEAR_DELAY_MAX + 1);
    }

    private void activateSirens(ServerLevel level) {
        for (BlockPos pos : sirenPositions) {
            if (!isWithinSirenRadiusOfAnyPlayer(level, pos)) continue;

            int loops = level.random.nextInt(1, 4);
            int delay = level.random.nextInt(0, 101);
            triggerSirenAt(level, pos, loops, delay);
        }
    }

    private void triggerSirenAt(ServerLevel level, BlockPos pos, int loops, int delay) {
        if (!level.isLoaded(pos)) {
            level.getChunk(pos);
        }

        if (level.getBlockEntity(pos) instanceof MechanicalSirenBlockEntity siren) {
            siren.triggerManifolding(loops, delay);
            pendingSirenTriggers.remove(pos);
            return;
        }

        if (level.getBlockState(pos).getBlock() instanceof MechanicalSirenBlock) {
            pendingSirenTriggers.put(pos, new PendingSirenTrigger(loops, delay));
            return;
        }

        removeSiren(pos);
    }

    public void applyPendingSirenTrigger(MechanicalSirenBlockEntity siren) {
        PendingSirenTrigger pending = pendingSirenTriggers.remove(siren.getBlockPos());
        if (pending != null) {
            siren.triggerManifolding(pending.loops(), pending.delay());
        }
    }

    private boolean isWithinSirenRadiusOfAnyPlayer(ServerLevel level, BlockPos pos) {
        for (Player player : level.players()) {
            BlockPos center = player.blockPosition();
            int minY = Math.max(level.getMinBuildHeight(), center.getY() - SIREN_RADIUS);
            int maxY = Math.min(level.getMaxBuildHeight(), center.getY() + SIREN_RADIUS);

            if (Math.abs(pos.getX() - center.getX()) <= SIREN_RADIUS
                    && Math.abs(pos.getZ() - center.getZ()) <= SIREN_RADIUS
                    && pos.getY() >= minY && pos.getY() <= maxY) {
                return true;
            }
        }
        return false;
    }

    public void addDetector(BlockPos pos) {
        loadedDetectors.add(pos);
    }

    public void removeDetector(BlockPos pos) {
        loadedDetectors.remove(pos);
    }

    public void addSiren(BlockPos pos) {
        sirenPositions.add(pos.immutable());
    }

    public void removeSiren(BlockPos pos) {
        sirenPositions.remove(pos);
        pendingSirenTriggers.remove(pos);
    }

    private void triggerDetectors(ServerLevel level) {
        for (BlockPos pos : loadedDetectors) {
            if (level.isLoaded(pos)) {
                if (level.getBlockEntity(pos) instanceof TemporalSurgeDetectorBlockEntity detector) {
                    detector.pulse();
                }
            }
        }
    }

    public void applyWindToAllEntities(ServerLevel level) {
        if (phase == ManifoldingPhase.CLEAR) return;

        float strength = getWindStrength(level);
        if (strength <= 0) return;

        for (Entity entity : level.getEntities().getAll()) {
            if (entity == null) continue;
            if (!level.isLoaded(entity.blockPosition())) continue;

            applyWindToEntity(entity, level);
        }
    }

    private void applyDamageToAllEntities(ServerLevel level) {
        for (Entity entity : level.getEntities().getAll()) {
            if (entity == null) continue;
            if (!level.isLoaded(entity.blockPosition())) continue;

            if (entity instanceof LivingEntity living) {
                damageIfExposed(living, level);
            }
        }
    }

    private final Map<UUID, Long> entityLastDamageTick = new HashMap<>();

    public void damageIfExposed(LivingEntity entity, ServerLevel level) {
        if (phase != ManifoldingPhase.ACTIVE) return;
        if (entity.isInvulnerableTo(level.damageSources().generic())) return;
        if (entity instanceof BurrowBeaconEntity) return;

        if (isNearBurrowBeacon(entity, level)) return;

        Vec3 checkDirection = Vec3.directionFromRotation(0, windAngle + 180).normalize();
        Vec3 checkOrigin = getEntityCheckOrigin(entity);
        boolean exposed = ManifoldingWindScan.isExposedToWind(level, checkOrigin, checkDirection);

        if (entity instanceof Player player) {
            playerExposed.put(player.getUUID(), exposed);

            if (player.isCreative()) return;
        }

        if (entity.isSpectator()) {
            playerExposed.put(entity.getUUID(), exposed);

            return;
        }

        if (exposed) {
            long now = level.getGameTime();
            UUID id = entity.getUUID();
            long lastDamage = entityLastDamageTick.getOrDefault(id, 0L);

            if (now - lastDamage >= DAMAGE_INTERVAL) {
                entity.hurt(DamageTypeRegistry.getSimpleDamageSource(level, DamageTypeRegistry.MANIFOLDING_ERASURE), 2f);
                entityLastDamageTick.put(id, now);
            }
        }
    }

    private static Vec3 getEntityCheckOrigin(Entity entity) {
        if (entity instanceof LivingEntity living) {
            return living.getEyePosition();
        }
        return entity.position().add(0, entity.getBbHeight() / 2, 0);
    }

    public static Vec3 computeWindOffset(float strength, float windAngle, double pushMultiplier) {
        double push = WIND_PUSH_FORCE * strength * pushMultiplier;
        Vec3 dir = Vec3.directionFromRotation(0, windAngle).normalize();
        return new Vec3(dir.x * push, 0, dir.z * push);
    }

    public void applyWindToEntity(Entity entity, ServerLevel level) {
        if (!level.dimension().location().equals(DIMENSION_ID)) return;
        if (phase == ManifoldingPhase.CLEAR) return;
        if (entity.isSpectator()) return;

        if (entity instanceof BurrowBeaconEntity) return;

        float strength = getWindStrength(level);
        if (strength <= 0) return;

        double pushMultiplier = isNearBurrowBeacon(entity, level) ? BEACON_PUSH_MULTIPLIER : 1.0;

        Vec3 checkDirection = Vec3.directionFromRotation(0, windAngle + 180).normalize();

        Vec3 checkOrigin = getEntityCheckOrigin(entity);
        boolean exposed = ManifoldingWindScan.isExposedToWind(level, checkOrigin, checkDirection);

        if (entity instanceof Player player) {
            playerExposed.put(player.getUUID(), exposed);
            if (player.isCreative()) return;
        }

        if (exposed) {
            boolean wasOnGround = entity.onGround();
            entity.move(MoverType.SELF, computeWindOffset(strength, windAngle, pushMultiplier));
            entity.setOnGround(wasOnGround);
        }
    }

    public static boolean isBlockExposedToWind(ServerLevel level, BlockPos pos, float windAngle) {
        Vec3 checkDirection = Vec3.directionFromRotation(0, windAngle + 180).normalize();
        Vec3 checkOrigin = new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        return ManifoldingWindScan.isExposedToWind(level, checkOrigin, checkDirection, pos);
    }

    private boolean isNearBurrowBeacon(Entity entity, ServerLevel level) {
        double radiusSq = BEACON_PROTECTION_RANGE * BEACON_PROTECTION_RANGE;

        for (BurrowBeaconEntity beacon : level.getEntitiesOfClass(BurrowBeaconEntity.class, entity.getBoundingBox().inflate(BEACON_PROTECTION_RANGE))) {
            if (beacon.isProvidingProtection() && beacon.distanceToSqr(entity) <= radiusSq) {
                return true;
            }
        }

        return false;
    }

    private void syncToClients(ServerLevel level) {
        for (Player player : level.players()) {
            boolean exposed = playerExposed.getOrDefault(player.getUUID(), false);

            ClientBoundManifoldingPacket packet = new ClientBoundManifoldingPacket(phase, getWindStrength(level), windAngle, thunderPulse, pulseTicks,
                    riftTicks, getLightDim(level), startTime, preEventDuration, activeDuration, postEventDuration, exposed);

            if (player instanceof ServerPlayer serverPlayer) {
                PacketHandlerRegistry.INSTANCE.send(PacketDistributor.PLAYER.with(() -> serverPlayer), packet);
            }
        }
    }

    private void spawnWindParticles(ServerLevel level) {
        float windStrength = getWindStrength(level);
        if (windStrength <= 0) return;

        double windRad = Math.toRadians(windAngle);
        double windDirX = -Math.sin(windRad);
        double windDirZ = Math.cos(windRad);

        double offsetX = -windDirX * windStrength * 16.0;
        double offsetZ = -windDirZ * windStrength * 16.0;

        for (Player player : level.players()) {
            int count = Math.max(1, (int)(windStrength * 32));

            for (int i = 0; i < count; i++) {
                double angle = level.random.nextDouble() * 2 * Math.PI;
                double dist = level.random.nextDouble() * 16;
                double x = player.getX() + offsetX + Math.cos(angle) * dist;
                double y = player.getY() + level.random.nextDouble() * 16 - 8;
                double z = player.getZ() + offsetZ + Math.sin(angle) * dist;

                if (!level.canSeeSky(BlockPos.containing(x, y, z))) continue;
                if (!level.getBlockState(BlockPos.containing(x, y, z)).isAir()) continue;

                double wx = windDirX * windStrength * 0.25;
                double wz = windDirZ * windStrength * 0.25;

                PacketHandlerRegistry.INSTANCE.send(
                        PacketDistributor.NEAR.with(() -> new PacketDistributor.TargetPoint(x, y, z, 32.0, level.dimension())),
                        new ClientBoundParticlePacket(ParticleTypeRegistry.ASH.getId(), x, y, z, wx, MANIFOLDING_ASH_LIFETIME, wz, 1)
                );
            }
        }
    }

    public float getLightDim(Level level) {
        if (phase == ManifoldingPhase.CLEAR) return 0;

        long elapsed = level.getGameTime() - startTime;

        return switch (phase) {
            case PRE_EVENT -> Math.min(1, (float) elapsed / preEventDuration);
            case ACTIVE -> 1;
            case POST_EVENT -> 1 - Math.min(1, (float) elapsed / postEventDuration);
            default -> 0;
        };
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();

        tag.putString(PHASE, phase.name());
        tag.putInt(TIME_UNTIL_NEXT_EVENT, timeUntilNextEvent);
        tag.putLong(START_TIME, startTime);
        tag.putInt(PRE_EVENT_DURATION, preEventDuration);
        tag.putInt(ACTIVE_DURATION, activeDuration);
        tag.putInt(POST_EVENT_DURATION, postEventDuration);
        tag.putFloat(WIND_ANGLE, windAngle);
        tag.put(SIREN_POSITIONS, new LongArrayTag(sirenPositions.stream().mapToLong(BlockPos::asLong).toArray()));

        ListTag pending = new ListTag();
        for (Map.Entry<BlockPos, PendingSirenTrigger> entry : pendingSirenTriggers.entrySet()) {
            CompoundTag triggerTag = new CompoundTag();
            triggerTag.putLong(PENDING_POS, entry.getKey().asLong());
            triggerTag.putInt(PENDING_LOOPS, entry.getValue().loops());
            triggerTag.putInt(PENDING_DELAY, entry.getValue().delay());
            pending.add(triggerTag);
        }
        tag.put(PENDING_SIREN_TRIGGERS, pending);

        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        phase = ManifoldingPhase.valueOf(tag.getString(PHASE));
        timeUntilNextEvent = tag.getInt(TIME_UNTIL_NEXT_EVENT);
        startTime = tag.getLong(START_TIME);
        preEventDuration = tag.getInt(PRE_EVENT_DURATION);
        activeDuration = tag.getInt(ACTIVE_DURATION);
        postEventDuration = tag.getInt(POST_EVENT_DURATION);
        windAngle = tag.getFloat(WIND_ANGLE);

        sirenPositions.clear();
        if (tag.contains(SIREN_POSITIONS, Tag.TAG_LONG_ARRAY)) {
            for (long encoded : tag.getLongArray(SIREN_POSITIONS)) {
                sirenPositions.add(BlockPos.of(encoded));
            }
        }

        pendingSirenTriggers.clear();
        if (tag.contains(PENDING_SIREN_TRIGGERS, Tag.TAG_LIST)) {
            ListTag pending = tag.getList(PENDING_SIREN_TRIGGERS, Tag.TAG_COMPOUND);
            for (Tag element : pending) {
                CompoundTag triggerTag = (CompoundTag) element;
                pendingSirenTriggers.put(
                        BlockPos.of(triggerTag.getLong(PENDING_POS)),
                        new PendingSirenTrigger(triggerTag.getInt(PENDING_LOOPS), triggerTag.getInt(PENDING_DELAY))
                );
            }
        }
    }

    public float getWindStrength(Level level) {
        if (phase == ManifoldingPhase.CLEAR) return 0;

        long elapsed = level.getGameTime() - startTime;
        long duration = switch (phase) {
            case PRE_EVENT -> preEventDuration;
            case ACTIVE -> activeDuration;
            case POST_EVENT -> postEventDuration;
            default -> 1;
        };

        float progress = Math.min(1f, (float) elapsed / duration);

        return switch (phase) {
            case PRE_EVENT -> progress;
            case ACTIVE -> 1f;
            case POST_EVENT -> 1f - progress;
            default -> 0;
        };
    }

    public ManifoldingPhase getPhase() {
        return phase;
    }

    public float getWindDirectionYaw() {
        return windAngle;
    }

    public long getPhaseStartTime() {
        return startTime;
    }

    public int getActiveDuration() {
        return activeDuration;
    }

    public boolean isChunkEroded(long chunkPos) {
        return erodedChunksThisEvent.contains(chunkPos);
    }

    public void markChunkEroded(long chunkPos) {
        erodedChunksThisEvent.add(chunkPos);
    }
}