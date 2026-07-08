package destiny.null_ouroboros.common.light;

import destiny.null_ouroboros.server.entity.DusterbikeEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class DusterbikeHeadlightManager {
    private static final Set<DusterbikeEntity> SOURCES = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private static final Map<DusterbikeEntity, BlockPos> LIGHT_POSITIONS = new ConcurrentHashMap<>();

    public static void register(DusterbikeEntity bike) {
        if (!bike.level().isClientSide) return;
        SOURCES.add(bike);
    }

    public static void unregister(DusterbikeEntity bike) {
        if (!bike.level().isClientSide) return;
        SOURCES.remove(bike);
        BlockPos oldPos = LIGHT_POSITIONS.remove(bike);
        if (oldPos != null) {
            bike.level().getLightEngine().checkBlock(oldPos);
            for (Direction dir : Direction.values()) {
                bike.level().getLightEngine().checkBlock(oldPos.relative(dir));
            }
        }
    }

    public static int getBlockLightContribution(Level level, BlockPos pos) {
        if (!level.isClientSide) return 0;
        if (SOURCES.isEmpty()) return 0;

        for (DusterbikeEntity bike : SOURCES) {
            if (!bike.isAlive() || bike.level() != level) continue;
            BlockPos lightPos = LIGHT_POSITIONS.get(bike);
            if (lightPos != null && lightPos.equals(pos)) {
                return 15;
            }
        }
        return 0;
    }

    public static void update(DusterbikeEntity bike) {
        if (!bike.level().isClientSide) return;
        if (!SOURCES.contains(bike)) return;

        BlockPos newPos = bike.computeHeadlightLightPos();
        BlockPos oldPos = LIGHT_POSITIONS.get(bike);

        if (newPos.equals(oldPos)) return;

        if (oldPos != null) {
            bike.level().getLightEngine().checkBlock(oldPos);
            for (Direction dir : Direction.values()) {
                bike.level().getLightEngine().checkBlock(oldPos.relative(dir));
            }
        }

        LIGHT_POSITIONS.put(bike, newPos);
        bike.level().getLightEngine().checkBlock(newPos);
        for (Direction dir : Direction.values()) {
            bike.level().getLightEngine().checkBlock(newPos.relative(dir));
        }
    }

    public static void clearAll() {
        SOURCES.clear();
        LIGHT_POSITIONS.clear();
    }
}