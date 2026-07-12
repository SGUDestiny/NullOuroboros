package destiny.null_ouroboros.common.light;

import destiny.null_ouroboros.server.entity.DusterbikeEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.*;

public final class DusterbikeHeadlightManager {
    private static final Map<DusterbikeEntity, BlockPos> BIKE_TO_POS = new HashMap<>();
    private static final Map<BlockPos, DusterbikeEntity> POS_TO_BIKE = new HashMap<>();

    private static final int LIGHT_LEVEL = 15;

    private static final Map<DusterbikeEntity, Vec3> LAST_BIKE_POS = new HashMap<>();
    private static final Map<DusterbikeEntity, Float> LAST_BIKE_YAW = new HashMap<>();
    private static final Map<DusterbikeEntity, Long> LAST_UPDATE_TICK = new HashMap<>();

    public static int UPDATE_INTERVAL_TICKS = 2;

    private static final double POS_CHANGE_THRESHOLD_SQ = 0.15 * 0.15;
    private static final float YAW_CHANGE_THRESHOLD = 1.0f;

    private static final BlockPos.MutableBlockPos MUTABLE_POS = new BlockPos.MutableBlockPos();

    public static void register(DusterbikeEntity bike) {
        if (!bike.level().isClientSide) return;
        BlockPos pos = computeAndCacheHeadlightPos(bike);
        BIKE_TO_POS.put(bike, pos);
        POS_TO_BIKE.put(pos, bike);
        updateLightEngine(bike.level(), null, pos);
    }

    public static void unregister(DusterbikeEntity bike) {
        if (!bike.level().isClientSide) return;
        BlockPos oldPos = BIKE_TO_POS.remove(bike);
        LAST_BIKE_POS.remove(bike);
        LAST_BIKE_YAW.remove(bike);
        LAST_UPDATE_TICK.remove(bike);
        if (oldPos != null) {
            POS_TO_BIKE.remove(oldPos);
            updateLightEngine(bike.level(), oldPos, null);
        }
    }

    public static int getBlockLightContribution(Level level, BlockPos pos) {
        if (!level.isClientSide) return 0;
        DusterbikeEntity bike = POS_TO_BIKE.get(pos);
        if (bike != null && bike.isAlive() && bike.level() == level) {
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

        if (lastPos != null && lastYaw != null &&
                currentPos.distanceToSqr(lastPos) < POS_CHANGE_THRESHOLD_SQ &&
                Math.abs(currentYaw - lastYaw) < YAW_CHANGE_THRESHOLD) {
            return;
        }

        LAST_BIKE_POS.put(bike, currentPos);
        LAST_BIKE_YAW.put(bike, currentYaw);

        BlockPos newPos = bike.computeHeadlightLightPos();
        BlockPos oldPos = BIKE_TO_POS.get(bike);

        if (newPos.equals(oldPos)) return;

        BIKE_TO_POS.put(bike, newPos);
        if (oldPos != null) POS_TO_BIKE.remove(oldPos);
        POS_TO_BIKE.put(newPos, bike);

        updateLightEngine(bike.level(), oldPos, newPos);
    }

    private static BlockPos computeAndCacheHeadlightPos(DusterbikeEntity bike) {
        LAST_BIKE_POS.put(bike, bike.position());
        LAST_BIKE_YAW.put(bike, bike.getYRot());
        return bike.computeHeadlightLightPos();
    }

    private static void updateLightEngine(Level level, @Nullable BlockPos oldPos, @Nullable BlockPos newPos) {
        Set<BlockPos> toUpdate = new HashSet<>();
        if (oldPos != null) {
            toUpdate.add(oldPos.immutable());
            for (Direction dir : Direction.values()) {
                toUpdate.add(MUTABLE_POS.set(oldPos).move(dir).immutable());
            }
        }
        if (newPos != null) {
            toUpdate.add(newPos.immutable());
            for (Direction dir : Direction.values()) {
                toUpdate.add(MUTABLE_POS.set(newPos).move(dir).immutable());
            }
        }
        for (BlockPos pos : toUpdate) {
            level.getLightEngine().checkBlock(pos);
        }
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
                if (pos != null) POS_TO_BIKE.remove(pos);
                LAST_BIKE_POS.remove(bike);
                LAST_BIKE_YAW.remove(bike);
                LAST_UPDATE_TICK.remove(bike);
                it.remove();
            }
        }
    }
}