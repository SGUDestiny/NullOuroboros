package destiny.null_ouroboros.server.entity;

import destiny.null_ouroboros.common.DusterbikeGear;
import destiny.null_ouroboros.common.DusterbikeGearConstants;
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

    public static final float MAX_FORWARD_SPEED = DusterbikeGearConstants.MAX_GEAR_3_SPEED;
    public static final float BRAKE_DECEL = 0.015F;
    public static final float COAST_DRAG = 0.003F;
    public static final float HANDBRAKE_DECEL = 0.025F;
    public static final double AIR_WHEEL_DRAG = 0.002D;
    public static final double AIR_REAR_DRIVE_TORQUE = 0.015D;
    public static final float SPEED_EPSILON = 1.0E-4F;

    private DusterbikePhysics() {}

    public record WheelContactResult(double contactY, boolean blocked, boolean grounded) {}

    public record WheelStepResult(double contactY, boolean blocked, boolean supported) {}

    public record BodyUpdateResult(float pitchDegrees, boolean movementBlocked) {}

    public static WheelContactResult probeGround(Level level, double x, double z, double referenceCenterY, float yawDegrees) {
        double scanTop = referenceCenterY + PROBE_ABOVE;
        double scanBottom = referenceCenterY - PROBE_BELOW;

        SurfaceResult surface = findBestSurface(level, x, z, scanBottom, scanTop, yawDegrees);
        if (!surface.supported()) {
            return new WheelContactResult(referenceCenterY - MAX_DROP_HEIGHT, false, false);
        }

        return resolveSurfaceContact(referenceCenterY, surface.surfaceY());
    }

    public static WheelContactResult probeGround(Level level, double x, double z,
                                                 double referenceCenterY, double restCenterY, float yawDegrees) {
        double scanTop = Math.max(referenceCenterY + PROBE_ABOVE, restCenterY + MAX_STEP_HEIGHT + SURFACE_EPSILON);
        double scanBottom = Math.min(referenceCenterY, restCenterY) - PROBE_BELOW;

        SurfaceResult surface = findBestSurface(level, x, z, scanBottom, scanTop, yawDegrees);
        if (!surface.supported()) {
            return new WheelContactResult(referenceCenterY - MAX_DROP_HEIGHT, false, false);
        }

        double targetCenterY = surface.surfaceY() + DusterbikeTransforms.WHEEL_HALF_HEIGHT;
        if (targetCenterY > referenceCenterY + MAX_STEP_HEIGHT + SURFACE_EPSILON
                && targetCenterY <= restCenterY + MAX_STEP_HEIGHT + SURFACE_EPSILON) {
            double steppedY = clampWheelTravelAsymmetric(targetCenterY, restCenterY, referenceCenterY);
            return new WheelContactResult(steppedY, false, true);
        }

        return resolveSurfaceContact(referenceCenterY, surface.surfaceY());
    }

    public static WheelStepResult probeStepSurface(Level level, double x, double z, double referenceCenterY, float yawDegrees) {
        double scanTop = referenceCenterY + MAX_STEP_HEIGHT + SURFACE_EPSILON;
        double scanBottom = referenceCenterY - PROBE_BELOW;

        SurfaceResult surface = findBestSurface(level, x, z, scanBottom, scanTop, yawDegrees);
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
            return new WheelContactResult(referenceCenterY, true, false);
        }

        double drop = referenceCenterY - targetCenterY;
        if (drop > MAX_DROP_HEIGHT + SURFACE_EPSILON) {
            double steppedY = referenceCenterY - MAX_DROP_HEIGHT;
            if (steppedY < targetCenterY) {
                steppedY = targetCenterY;
            }
            boolean grounded = Math.abs(steppedY - targetCenterY) <= SURFACE_EPSILON;
            return new WheelContactResult(steppedY, false, grounded);
        }

        return new WheelContactResult(targetCenterY, false, true);
    }

    private static SurfaceResult findBestSurface(Level level, double x, double z, double scanBottom, double scanTop, float yawDegrees) {
        double[] halfExtents = DusterbikeTransforms.yawMorphedHalfExtents(
                DusterbikeTransforms.WHEEL_HALF_WIDTH, DusterbikeTransforms.WHEEL_HALF_DEPTH, yawDegrees);
        double halfX = halfExtents[0];
        double halfZ = halfExtents[1];

        double bestSurface = Double.NEGATIVE_INFINITY;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int minX = Mth.floor(x - halfX);
        int maxX = Mth.floor(x + halfX);
        int minY = Mth.floor(scanBottom);
        int maxY = Mth.floor(scanTop);
        int minZ = Mth.floor(z - halfZ);
        int maxZ = Mth.floor(z + halfZ);

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
                        if (worldBox.maxX <= x - halfX || worldBox.minX >= x + halfX
                                || worldBox.maxZ <= z - halfZ || worldBox.minZ >= z + halfZ) {
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

    public static double linearSpeedToAngular(float forwardSpeed) {
        return -forwardSpeed / DusterbikeTransforms.WHEEL_RADIUS;
    }

    public static float angularToLinearSpeed(double angularVelocity) {
        return (float) (-angularVelocity * DusterbikeTransforms.WHEEL_RADIUS);
    }

    public static float applyCoastDrag(float speed, float drag) {
        if (Math.abs(speed) <= SPEED_EPSILON) {
            return 0.0F;
        }
        float sign = Math.signum(speed);
        float magnitude = Math.abs(speed) - drag;
        return magnitude <= 0.0F ? 0.0F : sign * magnitude;
    }

    public static float approachSpeed(float current, float target, float step) {
        if (current < target) {
            return Math.min(current + step, target);
        }
        return Math.max(current - step, target);
    }

    public static float clampSpeed(float speed, float max) {
        return Mth.clamp(speed, -max, max);
    }

    public static float priorGearMaxSpeed(DusterbikeGear gear) {
        return switch (gear) {
            case GEAR_2 -> DusterbikeGearConstants.MAX_GEAR_1_SPEED;
            case GEAR_3 -> DusterbikeGearConstants.MAX_GEAR_2_SPEED;
            default -> 0.0F;
        };
    }

    public static float computeForwardThrottleAcceleration(float currentSpeed, DusterbikeGear gear) {
        if (!gear.allowsForwardThrottle()) {
            return 0.0F;
        }

        float speedTowardMax = Math.max(0.0F, currentSpeed);
        float maxSpeed = gear.maxSpeed();
        if (speedTowardMax >= maxSpeed - SPEED_EPSILON) {
            return 0.0F;
        }

        float priorMax = priorGearMaxSpeed(gear);
        float timeSeconds = effectiveForwardTimeToMax(gear, speedTowardMax);
        float ticks = timeSeconds * DusterbikeGearConstants.TICKS_PER_SECOND;

        if (priorMax > SPEED_EPSILON && speedTowardMax >= priorMax - SPEED_EPSILON) {
            return (maxSpeed - priorMax) / ticks;
        }

        return (maxSpeed - speedTowardMax) / ticks;
    }

    public static float computeReverseThrottleAcceleration(float currentSpeed) {
        float reverseMagnitude = Math.max(0.0F, -currentSpeed);
        if (reverseMagnitude >= DusterbikeGearConstants.MAX_REVERSE_SPEED - SPEED_EPSILON) {
            return 0.0F;
        }

        return DusterbikeGearConstants.MAX_REVERSE_SPEED
                / (DusterbikeGearConstants.SECONDS_TO_MAX_REVERSE * DusterbikeGearConstants.TICKS_PER_SECOND);
    }

    private static float effectiveForwardTimeToMax(DusterbikeGear gear, float currentForwardSpeed) {
        return switch (gear) {
            case GEAR_1 -> DusterbikeGearConstants.SECONDS_TO_MAX_GEAR_1;
            case GEAR_2 -> {
                float entryRatio = Mth.clamp(
                        currentForwardSpeed / DusterbikeGearConstants.MAX_GEAR_1_SPEED, 0.0F, 1.0F);
                yield Mth.lerp(
                        entryRatio,
                        DusterbikeGearConstants.SECONDS_TO_MAX_GEAR_2_FROM_STANDSTILL,
                        DusterbikeGearConstants.SECONDS_TO_MAX_GEAR_2);
            }
            case GEAR_3 -> {
                float entryRatio = Mth.clamp(
                        currentForwardSpeed / DusterbikeGearConstants.MAX_GEAR_2_SPEED, 0.0F, 1.0F);
                yield Mth.lerp(
                        entryRatio,
                        DusterbikeGearConstants.SECONDS_TO_MAX_GEAR_3_FROM_STANDSTILL,
                        DusterbikeGearConstants.SECONDS_TO_MAX_GEAR_3);
            }
            default -> DusterbikeGearConstants.SECONDS_TO_MAX_GEAR_1;
        };
    }

    public static float clampSpeedForGear(float speed, DusterbikeGear gear) {
        return switch (gear) {
            case R -> Mth.clamp(speed, -DusterbikeGearConstants.MAX_REVERSE_SPEED, DusterbikeGearConstants.MAX_REVERSE_SPEED);
            case N -> speed;
            case GEAR_1 -> Mth.clamp(speed, 0.0F, DusterbikeGearConstants.MAX_GEAR_1_SPEED);
            case GEAR_2 -> Mth.clamp(speed, 0.0F, DusterbikeGearConstants.MAX_GEAR_2_SPEED);
            case GEAR_3 -> Mth.clamp(speed, 0.0F, DusterbikeGearConstants.MAX_GEAR_3_SPEED);
        };
    }

    public static float gearMaxSpeedMagnitude(DusterbikeGear gear) {
        return switch (gear) {
            case R -> DusterbikeGearConstants.MAX_REVERSE_SPEED;
            case N -> 0.0F;
            case GEAR_1 -> DusterbikeGearConstants.MAX_GEAR_1_SPEED;
            case GEAR_2 -> DusterbikeGearConstants.MAX_GEAR_2_SPEED;
            case GEAR_3 -> DusterbikeGearConstants.MAX_GEAR_3_SPEED;
        };
    }
}
