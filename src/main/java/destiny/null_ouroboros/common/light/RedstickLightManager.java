package destiny.null_ouroboros.common.light;

import destiny.null_ouroboros.server.entity.RedstickEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class RedstickLightManager {
    public static final int LIGHT_RADIUS = 8;
    public static final int LIGHT_LEVEL = 12;
    private static final Set<RedstickEntity> SOURCES = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private static final Map<RedstickEntity, SourceState> SOURCE_STATES = new ConcurrentHashMap<>();

    private RedstickLightManager() {}

    public static void register(RedstickEntity redstick) {
        if (!redstick.level().isClientSide) return;
        SOURCES.add(redstick);
        update(redstick);
    }

    public static void unregister(RedstickEntity redstick) {
        if (!redstick.level().isClientSide) return;
        SOURCES.remove(redstick);
        SourceState previousState = SOURCE_STATES.remove(redstick);
        if (previousState != null) {
            checkLightRemoval(redstick.level(), previousState.pos());
        }
    }

    public static void update(RedstickEntity redstick) {
        if (!redstick.level().isClientSide) return;
        if (!SOURCES.contains(redstick)) return;

        BlockPos currentPosition = redstick.blockPosition();
        int currentLight = redstick.getBlockLightLevel();
        SourceState previousState = SOURCE_STATES.get(redstick);

        if (currentLight <= 0) {
            if (previousState != null) {
                SOURCE_STATES.remove(redstick);
                checkLightRemoval(redstick.level(), previousState.pos());
            }
            return;
        }

        SourceState currentState = new SourceState(currentPosition, currentLight);
        if (previousState == null) {
            SOURCE_STATES.put(redstick, currentState);
            checkLightSource(redstick.level(), currentPosition);
            return;
        }

        if (!previousState.equals(currentState)) {
            SOURCE_STATES.put(redstick, currentState);
            checkLightSource(redstick.level(), previousState.pos());
            checkLightSource(redstick.level(), currentPosition);
        }
    }

    public static int getBlockLightContribution(Level level, BlockPos pos) {
        if (!level.isClientSide) return 0;
        if (SOURCES.isEmpty()) return 0;

        int maxLight = 0;

        for (RedstickEntity source : SOURCES) {
            if (!source.isAlive() || source.level() != level) continue;
            SourceState state = SOURCE_STATES.get(source);
            if (state != null && state.pos().equals(pos)) {
                maxLight = Math.max(maxLight, state.lightLevel());
            }
        }

        return maxLight;
    }

    public static void scheduleRecheckSavedBlockLight(Level level, LevelChunk chunk) {
        if (!level.isClientSide) return;

        net.minecraft.client.Minecraft.getInstance().execute(() -> recheckSavedBlockLight(level, chunk));
    }

    public static void clearAll() {
        SOURCES.clear();
        SOURCE_STATES.clear();
    }

    private static void recheckSavedBlockLight(Level level, LevelChunk chunk) {
        if (!level.isClientSide || level != chunk.getLevel()) return;

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = level.getMinBuildHeight(); y < level.getMaxBuildHeight(); y++) {
                    pos.set(chunk.getPos().getMinBlockX() + x, y, chunk.getPos().getMinBlockZ() + z);
                    if (level.getBrightness(LightLayer.BLOCK, pos) <= 0) continue;
                    if (level.getBlockState(pos).getLightEmission(level, pos) > 0) continue;
                    level.getLightEngine().checkBlock(pos);
                }
            }
        }
    }

    private static void checkLightSource(Level level, BlockPos pos) {
        level.getLightEngine().checkBlock(pos);
        for (Direction direction : Direction.values()) {
            level.getLightEngine().checkBlock(pos.relative(direction));
        }
    }

    private static void checkLightRemoval(Level level, BlockPos pos) {
        int radiusSqr = LIGHT_RADIUS * LIGHT_RADIUS;

        for (int x = -LIGHT_RADIUS; x <= LIGHT_RADIUS; x++) {
            for (int y = -LIGHT_RADIUS; y <= LIGHT_RADIUS; y++) {
                for (int z = -LIGHT_RADIUS; z <= LIGHT_RADIUS; z++) {
                    if (x * x + y * y + z * z > radiusSqr) continue;
                    level.getLightEngine().checkBlock(pos.offset(x, y, z));
                }
            }
        }
    }

    private record SourceState(BlockPos pos, int lightLevel) {}
}
