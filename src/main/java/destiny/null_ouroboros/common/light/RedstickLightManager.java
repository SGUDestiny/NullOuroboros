package destiny.null_ouroboros.common.light;

import destiny.null_ouroboros.server.entity.RedstickEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class RedstickLightManager {
    public static final int LIGHT_RADIUS = 8;
    public static final int LIGHT_LEVEL = 12;

    private static final Map<BlockPos, Set<RedstickEntity>> POS_TO_SOURCES = new HashMap<>();
    private static final Map<RedstickEntity, SourceState> SOURCE_STATES = new HashMap<>();
    private static final Map<RedstickEntity, Long> LAST_UPDATE_TICK = new HashMap<>();

    public static int UPDATE_INTERVAL_TICKS = 2;
    private static final BlockPos.MutableBlockPos MUTABLE_POS = new BlockPos.MutableBlockPos();

    private RedstickLightManager() {}

    public static void register(RedstickEntity redstick) {
        if (!redstick.level().isClientSide) return;
        update(redstick);
    }

    public static void unregister(RedstickEntity redstick) {
        if (!redstick.level().isClientSide) return;
        SourceState previousState = SOURCE_STATES.remove(redstick);
        LAST_UPDATE_TICK.remove(redstick);
        if (previousState != null) {
            BlockPos pos = previousState.pos();
            removeFromPosMap(pos, redstick);
            checkLightRemoval(redstick.level(), pos);
        }
    }

    public static void update(RedstickEntity redstick) {
        if (!redstick.level().isClientSide) return;

        long currentTick = redstick.level().getGameTime();
        int currentLight = redstick.getBlockLightLevel();
        boolean turningOff = currentLight <= 0;

        if (!turningOff) {
            Long lastTick = LAST_UPDATE_TICK.get(redstick);
            if (lastTick != null && currentTick - lastTick < UPDATE_INTERVAL_TICKS) {
                return;
            }
        }
        LAST_UPDATE_TICK.put(redstick, currentTick);

        BlockPos currentPos = redstick.blockPosition().immutable();
        SourceState previousState = SOURCE_STATES.get(redstick);

        if (turningOff) {
            if (previousState != null) {
                SOURCE_STATES.remove(redstick);
                LAST_UPDATE_TICK.remove(redstick);
                removeFromPosMap(previousState.pos(), redstick);
                checkLightRemoval(redstick.level(), previousState.pos());
            }
            return;
        }

        SourceState currentState = new SourceState(currentPos, currentLight);

        if (previousState == null) {
            SOURCE_STATES.put(redstick, currentState);
            addToPosMap(currentPos, redstick);
            checkLightSource(redstick.level(), currentPos);
            return;
        }

        if (!previousState.pos().equals(currentPos)) {
            SOURCE_STATES.put(redstick, currentState);
            removeFromPosMap(previousState.pos(), redstick);
            addToPosMap(currentPos, redstick);
            checkLightRemoval(redstick.level(), previousState.pos());
            checkLightSource(redstick.level(), currentPos);
            return;
        }

        if (previousState.lightLevel() != currentLight) {
            SOURCE_STATES.put(redstick, currentState);
            checkLightSource(redstick.level(), currentPos);
            return;
        }

        checkLightSource(redstick.level(), currentPos);
    }

    public static int getBlockLightContribution(Level level, BlockPos pos) {
        if (!level.isClientSide) return 0;
        Set<RedstickEntity> sources = POS_TO_SOURCES.get(pos);
        if (sources == null) return 0;

        int maxLight = 0;
        for (RedstickEntity source : sources) {
            if (!source.isAlive() || source.level() != level) continue;
            if (!SOURCE_STATES.containsKey(source)) continue;
            maxLight = Math.max(maxLight, source.getBlockLightLevel());
        }
        return maxLight;
    }

    public static void scheduleRecheckSavedBlockLight(Level level, LevelChunk chunk) {
        if (!level.isClientSide) return;
        long chunkKey = chunk.getPos().toLong();
        Minecraft.getInstance().execute(() -> recheckSavedBlockLight(level, chunkKey));
    }

    public static void clearAll() {
        POS_TO_SOURCES.clear();
        SOURCE_STATES.clear();
        LAST_UPDATE_TICK.clear();
    }

    private static void recheckSavedBlockLight(Level level, long chunkKey) {
        if (!level.isClientSide) return;

        int chunkX = ChunkPos.getX(chunkKey);
        int chunkZ = ChunkPos.getZ(chunkKey);
        LevelChunk chunk = level.getChunkSource().getChunkNow(chunkX, chunkZ);
        if (chunk == null || level != chunk.getLevel()) return;

        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;
        boolean anySource = false;
        ChunkPos chunkPos = chunk.getPos();
        int minX = chunkPos.getMinBlockX();
        int minZ = chunkPos.getMinBlockZ();
        int maxX = minX + 15;
        int maxZ = minZ + 15;

        for (Map.Entry<RedstickEntity, SourceState> entry : SOURCE_STATES.entrySet()) {
            RedstickEntity source = entry.getKey();
            if (!source.isAlive() || source.level() != level) continue;
            BlockPos pos = entry.getValue().pos();
            if (pos.getX() < minX - LIGHT_RADIUS || pos.getX() > maxX + LIGHT_RADIUS
                    || pos.getZ() < minZ - LIGHT_RADIUS || pos.getZ() > maxZ + LIGHT_RADIUS) {
                continue;
            }
            anySource = true;
            minY = Math.min(minY, pos.getY() - LIGHT_RADIUS);
            maxY = Math.max(maxY, pos.getY() + LIGHT_RADIUS);
            checkLightSource(level, pos);
        }

        if (!anySource) return;

        minY = Math.max(level.getMinBuildHeight(), minY);
        maxY = Math.min(level.getMaxBuildHeight() - 1, maxY);

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = minY; y <= maxY; y++) {
                    pos.set(minX + x, y, minZ + z);
                    if (level.getBrightness(LightLayer.BLOCK, pos) <= 0) continue;
                    if (level.getBlockState(pos).getLightEmission(level, pos) > 0) continue;
                    level.getLightEngine().checkBlock(pos);
                }
            }
        }
    }

    private static void addToPosMap(BlockPos pos, RedstickEntity entity) {
        POS_TO_SOURCES.computeIfAbsent(pos.immutable(), k -> new HashSet<>()).add(entity);
    }

    private static void removeFromPosMap(BlockPos pos, RedstickEntity entity) {
        Set<RedstickEntity> set = POS_TO_SOURCES.get(pos);
        if (set != null) {
            set.remove(entity);
            if (set.isEmpty()) POS_TO_SOURCES.remove(pos);
        }
    }

    private static void checkLightSource(Level level, BlockPos pos) {
        level.getLightEngine().checkBlock(pos);
        for (Direction direction : Direction.values()) {
            level.getLightEngine().checkBlock(MUTABLE_POS.set(pos).move(direction));
        }
    }

    private static void checkLightRemoval(Level level, BlockPos pos) {
        int radiusSqr = LIGHT_RADIUS * LIGHT_RADIUS;
        for (int x = -LIGHT_RADIUS; x <= LIGHT_RADIUS; x++) {
            for (int y = -LIGHT_RADIUS; y <= LIGHT_RADIUS; y++) {
                for (int z = -LIGHT_RADIUS; z <= LIGHT_RADIUS; z++) {
                    if (x * x + y * y + z * z > radiusSqr) continue;
                    MUTABLE_POS.set(pos).move(x, y, z);
                    level.getLightEngine().checkBlock(MUTABLE_POS);
                }
            }
        }
    }

    private record SourceState(BlockPos pos, int lightLevel) {}
}
