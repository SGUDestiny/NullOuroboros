package destiny.null_ouroboros.common.light;

import destiny.null_ouroboros.server.entity.DusterbikeEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public final class DusterbikeHeadlightManager {
    public static final int LIGHT_RADIUS = 15;
    public static final int LIGHT_LEVEL = 15;

    private static final Map<DusterbikeEntity, BlockPos> BIKE_TO_POS = new HashMap<>();
    private static final Map<BlockPos, DusterbikeEntity> POS_TO_BIKE = new HashMap<>();

    private static final Map<DusterbikeEntity, Vec3> LAST_BIKE_POS = new HashMap<>();
    private static final Map<DusterbikeEntity, Float> LAST_BIKE_YAW = new HashMap<>();
    private static final Map<DusterbikeEntity, Long> LAST_UPDATE_TICK = new HashMap<>();

    public static int UPDATE_INTERVAL_TICKS = 2;

    private static final double POS_CHANGE_THRESHOLD_SQ = 0.15 * 0.15;
    private static final float YAW_CHANGE_THRESHOLD = 1.0f;

    private static final BlockPos.MutableBlockPos MUTABLE_POS = new BlockPos.MutableBlockPos();

    private DusterbikeHeadlightManager() {}

    public static void register(DusterbikeEntity bike) {
        if (!bike.level().isClientSide) return;
        BlockPos pos = computeAndCacheHeadlightPos(bike).immutable();
        BIKE_TO_POS.put(bike, pos);
        POS_TO_BIKE.put(pos, bike);
        checkLightSource(bike.level(), pos);
    }

    public static void unregister(DusterbikeEntity bike) {
        if (!bike.level().isClientSide) return;
        BlockPos oldPos = BIKE_TO_POS.remove(bike);
        LAST_BIKE_POS.remove(bike);
        LAST_BIKE_YAW.remove(bike);
        LAST_UPDATE_TICK.remove(bike);
        if (oldPos != null) {
            POS_TO_BIKE.remove(oldPos);
            checkLightRemoval(bike.level(), oldPos);
        }
    }

    public static int getBlockLightContribution(Level level, BlockPos pos) {
        if (!level.isClientSide) return 0;
        DusterbikeEntity bike = POS_TO_BIKE.get(pos);
        if (bike != null && bike.isAlive() && bike.level() == level && BIKE_TO_POS.containsKey(bike)) {
            return LIGHT_LEVEL;
        }
        return 0;
    }

    public static void update(DusterbikeEntity bike) {
        if (!bike.level().isClientSide || !BIKE_TO_POS.containsKey(bike)) return;

        long currentTick = bike.level().getGameTime();
        Long lastTick = LAST_UPDATE_TICK.get(bike);

        if (lastTick != null && currentTick - lastTick < UPDATE_INTERVAL_TICKS) {
            return;
        }
        LAST_UPDATE_TICK.put(bike, currentTick);

        Vec3 currentPos = bike.position();
        float currentYaw = bike.getYRot();

        Vec3 lastPos = LAST_BIKE_POS.get(bike);
        Float lastYaw = LAST_BIKE_YAW.get(bike);

        BlockPos newPos = bike.computeHeadlightLightPos().immutable();
        BlockPos oldPos = BIKE_TO_POS.get(bike);

        boolean moved = lastPos == null || lastYaw == null
                || currentPos.distanceToSqr(lastPos) >= POS_CHANGE_THRESHOLD_SQ
                || Math.abs(currentYaw - lastYaw) >= YAW_CHANGE_THRESHOLD
                || !newPos.equals(oldPos);

        LAST_BIKE_POS.put(bike, currentPos);
        LAST_BIKE_YAW.put(bike, currentYaw);

        if (!moved) {
            checkLightSource(bike.level(), oldPos);
            return;
        }

        if (!newPos.equals(oldPos)) {
            BIKE_TO_POS.put(bike, newPos);
            if (oldPos != null) {
                POS_TO_BIKE.remove(oldPos);
            }
            POS_TO_BIKE.put(newPos, bike);
            if (oldPos != null) {
                checkLightRemoval(bike.level(), oldPos);
            }
            checkLightSource(bike.level(), newPos);
            return;
        }

        checkLightSource(bike.level(), newPos);
    }

    public static void scheduleRecheckSavedBlockLight(Level level, LevelChunk chunk) {
        if (!level.isClientSide) return;
        long chunkKey = chunk.getPos().toLong();
        Minecraft.getInstance().execute(() -> recheckSavedBlockLight(level, chunkKey));
    }

    public static void clearAll() {
        BIKE_TO_POS.clear();
        POS_TO_BIKE.clear();
        LAST_BIKE_POS.clear();
        LAST_BIKE_YAW.clear();
        LAST_UPDATE_TICK.clear();
    }

    public static void purgeDeadBikes() {
        Iterator<Map.Entry<DusterbikeEntity, BlockPos>> it = BIKE_TO_POS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<DusterbikeEntity, BlockPos> entry = it.next();
            DusterbikeEntity bike = entry.getKey();
            if (!bike.isAlive()) {
                BlockPos pos = entry.getValue();
                if (pos != null) {
                    POS_TO_BIKE.remove(pos);
                    checkLightRemoval(bike.level(), pos);
                }
                LAST_BIKE_POS.remove(bike);
                LAST_BIKE_YAW.remove(bike);
                LAST_UPDATE_TICK.remove(bike);
                it.remove();
            }
        }
    }

    private static BlockPos computeAndCacheHeadlightPos(DusterbikeEntity bike) {
        LAST_BIKE_POS.put(bike, bike.position());
        LAST_BIKE_YAW.put(bike, bike.getYRot());
        return bike.computeHeadlightLightPos();
    }

    private static void recheckSavedBlockLight(Level level, long chunkKey) {
        if (!level.isClientSide || BIKE_TO_POS.isEmpty()) return;

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

        for (Map.Entry<DusterbikeEntity, BlockPos> entry : BIKE_TO_POS.entrySet()) {
            DusterbikeEntity bike = entry.getKey();
            if (!bike.isAlive() || bike.level() != level) continue;
            BlockPos pos = entry.getValue();
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

    private static void checkLightSource(Level level, BlockPos pos) {
        if (pos == null) return;
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
}
