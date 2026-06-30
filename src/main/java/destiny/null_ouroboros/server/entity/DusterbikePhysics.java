package destiny.null_ouroboros.server.entity;

import destiny.null_ouroboros.common.DusterbikeTransforms;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class DusterbikePhysics {
    public static final double MAX_STEP_HEIGHT = 1.0D;
    public static final double MAX_DROP_HEIGHT = 1.0D;
    public static final double PROBE_ABOVE = 2.0D;
    public static final double PROBE_BELOW = 4.0D;
    public static final double SURFACE_EPSILON = 0.0625D;
    public static final double HEIGHT_SMOOTHING = 0.35D;

    private DusterbikePhysics() {}

    public record WheelContactResult(double contactY, boolean blocked) {}

    public record WheelStepResult(double contactY, boolean blocked, boolean supported) {}

    public record BodyUpdateResult(float pitchDegrees, boolean movementBlocked) {}

    /**
     * Finds the axle Y for a wheel at the given X/Z column.
     * {@code referenceCenterY} is the current collider-center height used for step-climb checks.
     */
    public static WheelContactResult probeGround(Level level, double x, double z, double referenceCenterY) {
        double scanTop = referenceCenterY + PROBE_ABOVE;
        double scanBottom = referenceCenterY - PROBE_BELOW;

        SurfaceResult surface = findBestSurface(level, x, z, scanBottom, scanTop);
        if (!surface.supported()) {
            return new WheelContactResult(referenceCenterY - MAX_DROP_HEIGHT, false);
        }

        return resolveSurfaceContact(referenceCenterY, surface.surfaceY());
    }

    public static WheelContactResult probeGround(Level level, double x, double z,
                                                 double referenceCenterY, double restCenterY) {
        double scanTop = Math.max(referenceCenterY + PROBE_ABOVE, restCenterY + MAX_STEP_HEIGHT + SURFACE_EPSILON);
        double scanBottom = Math.min(referenceCenterY, restCenterY) - PROBE_BELOW;

        SurfaceResult surface = findBestSurface(level, x, z, scanBottom, scanTop);
        if (!surface.supported()) {
            return new WheelContactResult(referenceCenterY - MAX_DROP_HEIGHT, false);
        }

        double targetCenterY = surface.surfaceY() + DusterbikeTransforms.WHEEL_HALF_HEIGHT;
        if (targetCenterY > referenceCenterY + MAX_STEP_HEIGHT + SURFACE_EPSILON
                && targetCenterY <= restCenterY + MAX_STEP_HEIGHT + SURFACE_EPSILON) {
            return new WheelContactResult(clampWheelTravelAsymmetric(targetCenterY, restCenterY, referenceCenterY), false);
        }

        return resolveSurfaceContact(referenceCenterY, surface.surfaceY());
    }

    public static WheelStepResult probeStepSurface(Level level, double x, double z, double referenceCenterY) {
        double scanTop = referenceCenterY + MAX_STEP_HEIGHT + SURFACE_EPSILON;
        double scanBottom = referenceCenterY - PROBE_BELOW;

        SurfaceResult surface = findBestSurface(level, x, z, scanBottom, scanTop);
        if (!surface.supported()) {
            return new WheelStepResult(referenceCenterY, false, false);
        }

        WheelContactResult contact = resolveSurfaceContact(referenceCenterY, surface.surfaceY());
        return new WheelStepResult(contact.contactY(), contact.blocked(), true);
    }

    private static WheelContactResult resolveSurfaceContact(double referenceCenterY, double surfaceY) {
        double targetCenterY = surfaceY + DusterbikeTransforms.WHEEL_HALF_HEIGHT;
        double rise = targetCenterY - referenceCenterY;
        if (rise > MAX_STEP_HEIGHT + SURFACE_EPSILON) {
            return new WheelContactResult(referenceCenterY, true);
        }

        double drop = referenceCenterY - targetCenterY;
        if (drop > MAX_DROP_HEIGHT + SURFACE_EPSILON) {
            double steppedY = referenceCenterY - MAX_DROP_HEIGHT;
            if (steppedY < targetCenterY) {
                steppedY = targetCenterY;
            }
            return new WheelContactResult(steppedY, false);
        }

        return new WheelContactResult(targetCenterY, false);
    }

    private static SurfaceResult findBestSurface(Level level, double x, double z, double scanBottom, double scanTop) {
        double halfW = DusterbikeTransforms.WHEEL_HALF_WIDTH;
        double halfD = DusterbikeTransforms.WHEEL_HALF_DEPTH;

        double bestSurface = Double.NEGATIVE_INFINITY;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int minX = Mth.floor(x - halfW);
        int maxX = Mth.floor(x + halfW);
        int minY = Mth.floor(scanBottom);
        int maxY = Mth.floor(scanTop);
        int minZ = Mth.floor(z - halfD);
        int maxZ = Mth.floor(z + halfD);

        for (int bx = minX; bx <= maxX; bx++) {
            for (int by = minY; by <= maxY; by++) {
                for (int bz = minZ; bz <= maxZ; bz++) {
                    pos.set(bx, by, bz);
                    BlockState state = level.getBlockState(pos);
                    VoxelShape shape = state.getCollisionShape(level, pos);
                    if (shape.isEmpty()) {
                        continue;
                    }

                    for (AABB part : shape.toAabbs()) {
                        AABB worldBox = part.move(bx, by, bz);
                        if (worldBox.maxX <= x - halfW || worldBox.minX >= x + halfW
                                || worldBox.maxZ <= z - halfD || worldBox.minZ >= z + halfD) {
                            continue;
                        }

                        if (worldBox.maxY > bestSurface) {
                            bestSurface = worldBox.maxY;
                        }
                    }
                }
            }
        }

        if (bestSurface == Double.NEGATIVE_INFINITY) {
            return new SurfaceResult(Double.NEGATIVE_INFINITY, false);
        }

        return new SurfaceResult(bestSurface, true);
    }

    private record SurfaceResult(double surfaceY, boolean supported) {}

    public static double smoothHeight(double current, double target) {
        if (Math.abs(target - current) < 1.0E-4D) {
            return target;
        }
        return Mth.lerp(HEIGHT_SMOOTHING, current, target);
    }

    /**
     * Resolves independent wheel ground probes. When only one wheel's terrain height changes,
     * the other wheel keeps its current world Y so the bike pivots instead of sinking as a unit.
     */
    public static double[] resolveAnchoredWheelHeights(double frontCurrentY, double frontProbedY, double frontRestY,
                                                       double rearCurrentY, double rearProbedY, double rearRestY) {
        double frontDelta = frontProbedY - frontCurrentY;
        double rearDelta = rearProbedY - rearCurrentY;
        boolean frontChanged = Math.abs(frontDelta) > SURFACE_EPSILON;
        boolean rearChanged = Math.abs(rearDelta) > SURFACE_EPSILON;

        double frontY = frontProbedY;
        double rearY = rearProbedY;

        if (frontChanged && !rearChanged) {
            rearY = rearCurrentY;
        } else if (rearChanged && !frontChanged) {
            frontY = frontCurrentY;
        }

        frontY = clampWheelTravelAsymmetric(frontY, frontRestY, frontCurrentY);
        rearY = clampWheelTravelAsymmetric(rearY, rearRestY, rearCurrentY);
        return new double[] {frontY, rearY};
    }

    /**
     * Drops are limited from the nominal rest height so a wheel cannot hang more than one block below
     * horizontal suspension. Climbs are limited from the higher of rest/current so continuous stairs
     * do not stall while the body origin catches up.
     */
    private static double clampWheelTravelAsymmetric(double targetY, double restY, double currentY) {
        double minY = restY - MAX_DROP_HEIGHT;
        double maxY = Math.max(restY, currentY) + MAX_STEP_HEIGHT;
        return Mth.clamp(targetY, minY, maxY);
    }

    public static BodyUpdateResult computePitch(double frontContactY, double rearContactY) {
        double deltaY = rearContactY - frontContactY;
        float pitch = (float) Math.toDegrees(Math.atan2(deltaY, DusterbikeTransforms.WHEELBASE_LENGTH));
        return new BodyUpdateResult(pitch, false);
    }
}
