package destiny.null_ouroboros.server.capability;

import destiny.null_ouroboros.NullOuroboros;
import destiny.null_ouroboros.client.network.ClientBoundManifoldingPacket;
import destiny.null_ouroboros.client.network.ClientBoundParticlePacket;
import destiny.null_ouroboros.server.block.MechanicalSirenBlock;
import destiny.null_ouroboros.server.block.entity.MechanicalSirenBlockEntity;
import destiny.null_ouroboros.server.block.entity.TemporalSurgeDetectorBlockEntity;
import destiny.null_ouroboros.server.entity.BurrowBeaconEntity;
import destiny.null_ouroboros.server.registry.DamageTypeRegistry;
import destiny.null_ouroboros.server.registry.PacketHandlerRegistry;
import destiny.null_ouroboros.server.registry.ParticleTypeRegistry;
import destiny.null_ouroboros.server.registry.SoundRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
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

    private static final TagKey<Block> IGNORED_BLOCKS = BlockTags.create(ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, "ignored_by_manifolding_wind"));

    private static final int SIREN_RADIUS = 256;
    private static final float WIND_PUSH_FORCE = 05f;
    private static final int DAMAGE_INTERVAL = 20;

    private static final int CLEAR_DELAY_MIN = 1 * 60 * 20;
    private static final int CLEAR_DELAY_MAX = 1 * 60 * 20;
    private static final int PRE_EVENT_MIN = 20 * 20;
    private static final int PRE_EVENT_MAX = 20 * 20;
    private static final int ACTIVE_MIN = 60 * 20;
    private static final int ACTIVE_MAX = 60 * 20;
    private static final int POST_EVENT_MIN = 20 * 20;
    private static final int POST_EVENT_MAX = 20 * 20;
    private static final int THUNDER_DELAY_MIN = 1 * 20;
    private static final int THUNDER_DELAY_MAX = 7 * 20;

    public static final double BEACON_PROTECTION_RANGE = 4;
    private static final float BEACON_PUSH_MULTIPLIER = 0.2f;

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
    private final Map<UUID, Boolean> playerExposed = new HashMap<>();

    public void init(ServerLevel level) {
        if (!level.isClientSide) {
            if (phase == ManifoldingPhase.CLEAR && timeUntilNextEvent <= 0) {
                randomizeNextEventDurations(level);
            }
        }
    }

    public void serverTick(ServerLevel level) {
        if (!level.dimension().location().equals(DIMENSION_ID)) return;
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
            applyWindToAllEntities(level);

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
                windAngle = level.random.nextFloat() * 360;

                activateSirens(level);
                triggerDetectors(level);
                playThunder(level);
            }
            case ACTIVE -> {
            }
            case POST_EVENT -> {
                triggerDetectors(level);
            }
            case CLEAR -> {
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
        for (Player player : level.players()) {
            BlockPos center = player.blockPosition();
            int minX = center.getX() - SIREN_RADIUS;
            int minY = Math.max(level.getMinBuildHeight(), center.getY() - SIREN_RADIUS);
            int minZ = center.getZ() - SIREN_RADIUS;
            int maxX = center.getX() + SIREN_RADIUS;
            int maxY = Math.min(level.getMaxBuildHeight(), center.getY() + SIREN_RADIUS);
            int maxZ = center.getZ() + SIREN_RADIUS;

            for (int x = minX; x <= maxX; x++) {
                for (int y = minY; y <= maxY; y++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        BlockPos pos = new BlockPos(x, y, z);

                        if (level.getBlockState(pos).getBlock() instanceof MechanicalSirenBlock) {
                            if (level.getBlockEntity(pos) instanceof MechanicalSirenBlockEntity siren) {
                                siren.triggerManifolding(level.random.nextInt(1, 4), level.random.nextInt(0, 101));
                            }
                        }
                    }
                }
            }
        }
    }

    public void addDetector(BlockPos pos) {
        loadedDetectors.add(pos);
    }

    public void removeDetector(BlockPos pos) {
        loadedDetectors.remove(pos);
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

    private void applyWindToAllEntities(ServerLevel level) {
        float strength = getWindStrength(level);
        if (strength <= 0) return;

        for (Entity entity : level.getEntities().getAll()) {
            if (!level.isLoaded(entity.blockPosition())) continue;

            if (entity instanceof LivingEntity living) {
                damageIfExposed(living, level);
            }

            if (entity instanceof Player) continue;

            applyWindToEntity(entity, level);
        }
    }

    private final Map<UUID, Long> entityLastDamageTick = new HashMap<>();

    public void damageIfExposed(LivingEntity entity, ServerLevel level) {
        if (phase != ManifoldingPhase.ACTIVE) return;
        if (entity.isSpectator()) return;
        if (entity.isInvulnerableTo(level.damageSources().generic())) return;
        if (entity instanceof BurrowBeaconEntity) return;

        if (isNearBurrowBeacon(entity, level)) return;

        Vec3 checkDirection = Vec3.directionFromRotation(0, windAngle + 180).normalize();
        Vec3 checkOrigin = getEntityCheckOrigin(entity);
        BlockPos entityPos = BlockPos.containing(checkOrigin.x, checkOrigin.y, checkOrigin.z);
        double maxDist = Math.min(level.canSeeSky(entityPos) ? 6 : 256, getLoadedChunkRayDistance(checkOrigin, checkDirection, level));
        BlockHitResult hit = performWindRaycast(checkOrigin, checkDirection, maxDist, level);

        boolean exposed = (hit.getType() == HitResult.Type.MISS);

        if (entity instanceof Player player) {
            playerExposed.put(player.getUUID(), exposed);

            if (player.isCreative()) return;
        }

        if (exposed) {
            long now = level.getGameTime();
            UUID id = entity.getUUID();
            long lastDamage = entityLastDamageTick.getOrDefault(id, 0L);

            if (now - lastDamage >= DAMAGE_INTERVAL) {
                entity.hurt(DamageTypeRegistry.getSimpleDamageSource(level, DamageTypeRegistry.MANIFOLDING_ERASURE), 1f);
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

    public void applyWindToEntity(Entity entity, ServerLevel level) {
        if (!level.dimension().location().equals(DIMENSION_ID)) return;
        if (phase == ManifoldingPhase.CLEAR) return;
        if (entity.isSpectator()) return;

        if (entity instanceof Player player && player.isCreative()) return;

        if (entity instanceof BurrowBeaconEntity) return;

        float strength = getWindStrength(level);
        if (strength <= 0) return;

        double effectivePush = WIND_PUSH_FORCE * strength;
        if (isNearBurrowBeacon(entity, level)) {
            effectivePush *= BEACON_PUSH_MULTIPLIER;
        }

        Vec3 direction = Vec3.directionFromRotation(0, windAngle).normalize();
        Vec3 checkDirection = Vec3.directionFromRotation(0, windAngle + 180).normalize();

        Vec3 checkOrigin = getEntityCheckOrigin(entity);
        BlockPos entityPos = BlockPos.containing(checkOrigin.x, checkOrigin.y, checkOrigin.z);
        double maxDist = Math.min(level.canSeeSky(entityPos) ? 6 : 256, getLoadedChunkRayDistance(checkOrigin, checkDirection, level));
        BlockHitResult hit = performWindRaycast(checkOrigin, checkDirection, maxDist, level);

        if (hit.getType() == HitResult.Type.MISS) {
            Vec3 velocity = entity.getDeltaMovement();
            entity.setDeltaMovement(velocity.x + direction.x * effectivePush, velocity.y, velocity.z + direction.z * effectivePush);
            entity.hurtMarked = true;
        }
    }

    private double getLoadedChunkRayDistance(Vec3 eyePos, Vec3 direction, ServerLevel level) {
        int chunkX = (int) Math.floor(eyePos.x) >> 4;
        int chunkZ = (int) Math.floor(eyePos.z) >> 4;

        int viewDistance = level.getServer().getPlayerList().getViewDistance();

        int minBlockX = (chunkX - viewDistance) * 16;
        int maxBlockX = (chunkX + viewDistance + 1) * 16 - 1;
        int minBlockZ = (chunkZ - viewDistance) * 16;
        int maxBlockZ = (chunkZ + viewDistance + 1) * 16 - 1;

        double tMax = Double.MAX_VALUE;

        if (Math.abs(direction.x) > 1e-6) {
            double t1 = (minBlockX - eyePos.x) / direction.x;
            double t2 = (maxBlockX - eyePos.x) / direction.x;
            if (t1 > 0 && t1 < tMax) tMax = t1;
            if (t2 > 0 && t2 < tMax) tMax = t2;
        }

        if (Math.abs(direction.z) > 1e-6) {
            double t1 = (minBlockZ - eyePos.z) / direction.z;
            double t2 = (maxBlockZ - eyePos.z) / direction.z;
            if (t1 > 0 && t1 < tMax) tMax = t1;
            if (t2 > 0 && t2 < tMax) tMax = t2;
        }

        return tMax;
    }

    private BlockHitResult performWindRaycast(Vec3 from, Vec3 direction, double maxDist, ServerLevel level) {
        Vec3 to = from.add(direction.scale(maxDist));

        double stepSize = 0.3;
        double distance = from.distanceTo(to);
        if (distance < 1e-6) return BlockHitResult.miss(from, Direction.getNearest(direction.x, direction.y, direction.z), BlockPos.containing(from));

        Vec3 step = direction.scale(stepSize);
        Vec3 current = from;

        for (double traveled = 0; traveled <= distance; traveled += stepSize) {
            BlockPos pos = BlockPos.containing(current);
            BlockState state = level.getBlockState(pos);

            if (state.is(IGNORED_BLOCKS)) {
                current = current.add(step);
                continue;
            }

            var shape = state.getCollisionShape(level, pos);
            if (!shape.isEmpty()) {
                BlockHitResult hit = shape.clip(from, to, pos);

                if (hit != null && hit.getType() != HitResult.Type.MISS) {
                    return hit;
                }
            }

            current = current.add(step);
        }

        return BlockHitResult.miss(to, Direction.getNearest(direction.x, direction.y, direction.z), BlockPos.containing(to));
    }

    private boolean isNearBurrowBeacon(Entity entity, ServerLevel level) {
        double radiusSq = BEACON_PROTECTION_RANGE * BEACON_PROTECTION_RANGE;

        for (BurrowBeaconEntity beacon : level.getEntitiesOfClass(BurrowBeaconEntity.class, entity.getBoundingBox().inflate(BEACON_PROTECTION_RANGE))) {
            if (beacon.distanceToSqr(entity) <= radiusSq) {
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

        double offsetX = -windDirX * windStrength * 16;
        double offsetZ = -windDirZ * windStrength * 16;

        for (Player player : level.players()) {
            int count = Math.max(1, (int)(windStrength * 64));

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
                        PacketDistributor.NEAR.with(() -> new PacketDistributor.TargetPoint(x, y, z, 32, level.dimension())),
                        new ClientBoundParticlePacket(ParticleTypeRegistry.ASH.getId(), x, y, z, wx, 0, wz, 1)
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
}