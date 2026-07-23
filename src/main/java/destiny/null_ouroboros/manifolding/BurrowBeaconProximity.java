package destiny.null_ouroboros.manifolding;

import destiny.null_ouroboros.server.capability.ManifoldingCapability;
import destiny.null_ouroboros.server.entity.BurrowBeaconEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;

public final class BurrowBeaconProximity {
    private static final double RANGE = ManifoldingCapability.BEACON_PROTECTION_RANGE;
    private static final double RANGE_SQ = RANGE * RANGE;
    private static final double QUERY_PADDING = 1.0D;

    private static Level cachedLevel;
    private static long cachedTime = Long.MIN_VALUE;
    private static final List<BurrowBeaconEntity> protecting = new ArrayList<>();

    private BurrowBeaconProximity() {
    }

    public static boolean isNear(Level level, Entity entity) {
        return isNear(level, entity.getX(), entity.getY() + entity.getBbHeight() * 0.5D, entity.getZ());
    }

    public static boolean isNear(Level level, double x, double y, double z) {
        List<BurrowBeaconEntity> beacons = protectingBeacons(level, x, y, z);
        if (beacons.isEmpty()) {
            return false;
        }
        for (int i = 0, size = beacons.size(); i < size; i++) {
            BurrowBeaconEntity beacon = beacons.get(i);
            if (beacon.isRemoved()) {
                continue;
            }
            if (beacon.distanceToSqr(x, y, z) <= RANGE_SQ) {
                return true;
            }
        }
        return false;
    }

    private static List<BurrowBeaconEntity> protectingBeacons(Level level, double x, double y, double z) {
        if (level instanceof ServerLevel serverLevel) {
            long time = level.getGameTime();
            if (cachedLevel == level && cachedTime == time) {
                return protecting;
            }
            cachedLevel = level;
            cachedTime = time;
            protecting.clear();
            for (Entity entity : serverLevel.getAllEntities()) {
                if (entity instanceof BurrowBeaconEntity beacon && beacon.isProvidingProtection()) {
                    protecting.add(beacon);
                }
            }
            return protecting;
        }

        double r = RANGE + QUERY_PADDING;
        return level.getEntitiesOfClass(
                BurrowBeaconEntity.class,
                new AABB(x - r, y - r, z - r, x + r, y + r, z + r),
                BurrowBeaconEntity::isProvidingProtection);
    }
}
