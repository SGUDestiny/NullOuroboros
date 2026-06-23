package destiny.null_ouroboros.server.manifolding;

import destiny.null_ouroboros.server.capability.ManifoldingCapability;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;

public final class ManifoldingWindScan {
    public static final int BUDGET = 128;
    public static final int COST_ENCLOSED = 1;
    public static final int COST_SKY = 16;
    private static final double STEP_SIZE = 0.3;

    private ManifoldingWindScan() {}

    public static boolean isExposedToWind(Level level, Vec3 from, Vec3 direction) {
        return isExposedToWind(level, from, direction, null);
    }

    public static boolean isExposedToWind(Level level, Vec3 from, Vec3 direction, @Nullable BlockPos skipPos) {
        if (direction.lengthSqr() < 1e-12) return false;

        direction = direction.normalize();
        double maxDist = BUDGET * (double) COST_ENCLOSED;
        if (level instanceof ServerLevel serverLevel) {
            maxDist = Math.min(maxDist, getLoadedChunkRayDistance(from, direction, serverLevel));
        }

        Vec3 to = from.add(direction.scale(maxDist));
        double distance = from.distanceTo(to);
        if (distance < 1e-6) return false;

        Vec3 step = direction.scale(STEP_SIZE);
        Vec3 current = from;
        int budgetUsed = 0;
        Set<BlockPos> budgeted = new HashSet<>();

        for (double traveled = 0; traveled <= distance; traveled += STEP_SIZE) {
            BlockPos pos = BlockPos.containing(current);

            if (skipPos != null && pos.equals(skipPos)) {
                current = current.add(step);
                continue;
            }

            if (level instanceof ServerLevel serverLevel && !serverLevel.hasChunk(pos.getX() >> 4, pos.getZ() >> 4)) {
                return true;
            }

            BlockState state = level.getBlockState(pos);

            if (!state.is(ManifoldingCapability.DOESNT_PROTECT_FROM_MANIFOLDING)) {
                var shape = state.getCollisionShape(level, pos);
                if (!shape.isEmpty()) {
                    BlockHitResult hit = shape.clip(from, to, pos);
                    if (hit != null && hit.getType() != HitResult.Type.MISS) {
                        return false;
                    }
                }
            }

            if (!budgeted.contains(pos)) {
                int cost = level.canSeeSky(pos) ? COST_SKY : COST_ENCLOSED;
                if (budgetUsed + cost > BUDGET) {
                    return true;
                }
                budgetUsed += cost;
                budgeted.add(pos);
            }

            current = current.add(step);
        }

        return true;
    }

    private static double getLoadedChunkRayDistance(Vec3 eyePos, Vec3 direction, ServerLevel level) {
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
}
