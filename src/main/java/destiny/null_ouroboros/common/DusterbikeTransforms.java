package destiny.null_ouroboros.common;

import destiny.null_ouroboros.common.dusterbike.DusterbikePartTargetType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class DusterbikeTransforms {
    public static final double MODEL_SCALE = 1.0D / 16.0D;

    public static final double MODEL_X_OFFSET = 0.0D;
    public static final double MODEL_Y_OFFSET = -1.51599375D;

    public static final Vec3 FRONT_COLLIDER_CENTER_LOCAL = new Vec3(0.0D, 0.46875D, 1.251575D);
    public static final Vec3 REAR_COLLIDER_CENTER_LOCAL = new Vec3(0.0D, 0.46875D, -1.248425D);

    public static final double RIDER_SEAT_FORWARD_OFFSET = 6.0D * MODEL_SCALE;

    public static final Vec3 DRIVER_LOCAL = deriveDriverLocalFromModelBones().add(0.0D, 0.0D, RIDER_SEAT_FORWARD_OFFSET);

    public static final double RIDER_FEET_OFFSET = 0.875D;

    public static final Vec3 RIDER_FEET_LOCAL = new Vec3(
            DRIVER_LOCAL.x, DRIVER_LOCAL.y - RIDER_FEET_OFFSET, DRIVER_LOCAL.z);

    public static final Vec3 RIDER_PITCH_PIVOT_LOCAL = RIDER_FEET_LOCAL;

    public static final Vec3 FRONT_WHEEL_LOCAL = FRONT_COLLIDER_CENTER_LOCAL;
    public static final Vec3 REAR_WHEEL_LOCAL = REAR_COLLIDER_CENTER_LOCAL;

    public static final double KEY_COLLIDER_HALF_WIDTH =
            DusterbikeModelBones.KEY_INTERACTION_COLLIDER_HALF_X * MODEL_SCALE;
    public static final double KEY_COLLIDER_HALF_HEIGHT =
            DusterbikeModelBones.KEY_INTERACTION_COLLIDER_HALF_Y * MODEL_SCALE;
    public static final double KEY_COLLIDER_HALF_DEPTH =
            DusterbikeModelBones.KEY_INTERACTION_COLLIDER_HALF_Z * MODEL_SCALE;

    public static final Vec3 KEY_COLLIDER_CENTER_LOCAL = DusterbikeModelBones.deriveKeyInteractionColliderCenterEntityLocal();

    public static final double WHEEL_COLLIDER_WIDTH = 0.25D;
    public static final double WHEEL_COLLIDER_HEIGHT = 0.9375D;
    public static final double WHEEL_COLLIDER_DEPTH = 0.9375D;

    public static final double WHEEL_HALF_WIDTH = WHEEL_COLLIDER_WIDTH * 0.5D;
    public static final double WHEEL_HALF_HEIGHT = WHEEL_COLLIDER_HEIGHT * 0.5D;
    public static final double WHEEL_HALF_DEPTH = WHEEL_COLLIDER_DEPTH * 0.5D;
    public static final double WHEEL_RADIUS = (16D / 2.0D) * MODEL_SCALE;

    public static final double BODY_WIDTH = 0.9D;
    public static final double BODY_HEIGHT = 1.25D;
    public static final double BODY_DEPTH = 2.2D;

    public static final float KEY_CRANK_ANGLE_DEGREES = 90.0F;

    public static final double WHEELBASE_LENGTH = FRONT_COLLIDER_CENTER_LOCAL.distanceTo(
            new Vec3(FRONT_COLLIDER_CENTER_LOCAL.x, FRONT_COLLIDER_CENTER_LOCAL.y, REAR_COLLIDER_CENTER_LOCAL.z));
    public static final Vec3 PITCH_PIVOT_LOCAL = FRONT_COLLIDER_CENTER_LOCAL.add(REAR_COLLIDER_CENTER_LOCAL).scale(0.5D);

    public static final double MISSING_WHEEL_HALF_HEIGHT = 0.15;

    private DusterbikeTransforms() {}

    public static Vec3 modelPixelPointToEntityLocal(double x, double y, double z) {
        return new Vec3(
                -x * MODEL_SCALE,
                -y * MODEL_SCALE - MODEL_Y_OFFSET,
                -z * MODEL_SCALE
        );
    }

    public static Vec3 deriveDriverLocalFromModelBones() {
        double localX = DusterbikeModelBones.BODY_X + DusterbikeModelBones.DRIVER_X;
        double localY = DusterbikeModelBones.BODY_Y + DusterbikeModelBones.DRIVER_Y;
        double localZ = DusterbikeModelBones.BODY_Z + DusterbikeModelBones.DRIVER_Z;

        double worldX = DusterbikeModelBones.BIKE_X - localX;
        double worldY = DusterbikeModelBones.BIKE_Y + localY;
        double worldZ = DusterbikeModelBones.BIKE_Z - localZ;

        return modelPixelPointToEntityLocal(worldX, worldY, worldZ);
    }

    public static AABB keyColliderBox(double centerX, double centerY, double centerZ, float yawDegrees) {
        double[] halfExtents = yawMorphedHalfExtents(KEY_COLLIDER_HALF_WIDTH, KEY_COLLIDER_HALF_DEPTH, yawDegrees);
        double halfX = halfExtents[0];
        double halfZ = halfExtents[1];
        return new AABB(
                centerX - halfX, centerY - KEY_COLLIDER_HALF_HEIGHT, centerZ - halfZ,
                centerX + halfX, centerY + KEY_COLLIDER_HALF_HEIGHT, centerZ + halfZ
        );
    }

    public static Vec3 computeKeyWorldCenter(
            Vec3 entityPos, float yRotDegrees, float pitchDegrees, float rollDegrees) {
        return worldPointFromLocal(entityPos, yRotDegrees, pitchDegrees, rollDegrees, KEY_COLLIDER_CENTER_LOCAL);
    }

    public static Vec3 computePartTargetWorldCenter(
            Vec3 entityPos, float yRotDegrees, float pitchDegrees, float rollDegrees, DusterbikePartTargetType targetType) {
        return worldPointFromLocal(entityPos, yRotDegrees, pitchDegrees, rollDegrees, targetType.localCenter());
    }

    public static AABB partTargetColliderBox(
            double centerX, double centerY, double centerZ, float yawDegrees, DusterbikePartTargetType targetType) {
        double[] halfExtents = yawMorphedHalfExtents(targetType.halfWidth(), targetType.halfDepth(), yawDegrees);
        double halfX = halfExtents[0];
        double halfZ = halfExtents[1];
        return new AABB(
                centerX - halfX, centerY - targetType.halfHeight(), centerZ - halfZ,
                centerX + halfX, centerY + targetType.halfHeight(), centerZ + halfZ
        );
    }

    public static Vec3 rotateLocalOffset(Vec3 local, float yRotDegrees) {
        float rad = yRotDegrees * Mth.DEG_TO_RAD;
        float cos = Mth.cos(rad);
        float sin = Mth.sin(rad);
        return new Vec3(
                local.x * cos - local.z * sin,
                local.y,
                local.x * sin + local.z * cos
        );
    }

    public static Vec3 rotateLocalOffsetY(Vec3 local, float steerDegrees) {
        float rad = steerDegrees * Mth.DEG_TO_RAD;
        float cos = Mth.cos(rad);
        float sin = Mth.sin(rad);
        return new Vec3(
                local.x * cos + local.z * sin,
                local.y,
                -local.x * sin + local.z * cos
        );
    }

    public static Vec3 rotateSteeredWheelLocalOffset(Vec3 local, float steerDegrees, float yRotDegrees) {
        return rotateLocalOffset(rotateLocalOffsetY(local, -steerDegrees), yRotDegrees);
    }

    public static double[] yawMorphedHalfExtents(double halfWidth, double halfDepth, float yawDegrees) {
        float yawRad = yawDegrees * Mth.DEG_TO_RAD;
        float localYaw = Mth.abs(Math.floorMod(Mth.floor(Mth.wrapDegrees(yawDegrees)), 90));
        float localRad = localYaw * Mth.DEG_TO_RAD;

        double cos = Mth.abs(Mth.cos(yawRad));
        double sin = Mth.abs(Mth.sin(yawRad));
        double standardHalfX = cos * halfWidth + sin * halfDepth;
        double standardHalfZ = sin * halfWidth + cos * halfDepth;

        double maxHalf = Math.max(halfWidth, halfDepth);
        float sin2 = Mth.sin(localRad * 2.0F);
        double squareBlend = sin2 * sin2;

        double halfX = Mth.lerp(squareBlend, standardHalfX, maxHalf);
        double halfZ = Mth.lerp(squareBlend, standardHalfZ, maxHalf);
        return new double[] {halfX, halfZ};
    }

    public static AABB wheelColliderBox(double centerX, double centerY, double centerZ, float yawDegrees, boolean isInstalled) {
        double[] halfExtents = yawMorphedHalfExtents(WHEEL_HALF_WIDTH, WHEEL_HALF_DEPTH, yawDegrees);
        double halfX = halfExtents[0];
        double halfZ = halfExtents[1];

        if (isInstalled) {
            return new AABB(
                    centerX - halfX, centerY - WHEEL_HALF_HEIGHT, centerZ - halfZ,
                    centerX + halfX, centerY + WHEEL_HALF_HEIGHT, centerZ + halfZ
            );
        } else {
            return new AABB(
                    centerX - halfX, centerY - halfX, centerZ - halfX,
                    centerX + halfX, centerY + halfX, centerZ + halfX
            );
        }
    }

    public static AABB bodyColliderBox(double originX, double originY, double originZ, float yawDegrees) {
        double halfW = BODY_WIDTH * 0.5D;
        double halfD = BODY_DEPTH * 0.5D;
        double[] halfExtents = yawMorphedHalfExtents(halfW, halfD, yawDegrees);
        double halfX = halfExtents[0];
        double halfZ = halfExtents[1];
        return new AABB(
                originX - halfX, originY, originZ - halfZ,
                originX + halfX, originY + BODY_HEIGHT, originZ + halfZ
        );
    }

    public static Vec3 worldPointFromLocal(Vec3 entityPos, float yRotDegrees, float pitchDegrees, Vec3 localPoint) {
        return worldPointFromLocal(entityPos, yRotDegrees, pitchDegrees, 0.0F, localPoint);
    }

    public static Vec3 worldPointFromLocal(
            Vec3 entityPos, float yRotDegrees, float pitchDegrees, float rollDegrees, Vec3 localPoint) {
        Vec3 worldPivot = entityPos.add(rotateLocalOffset(PITCH_PIVOT_LOCAL, yRotDegrees));
        Vec3 worldPoint = entityPos.add(rotateLocalOffset(localPoint, yRotDegrees));
        Vec3 offset = worldPoint.subtract(worldPivot);

        Vec3 right = rotateLocalOffset(new Vec3(1.0D, 0.0D, 0.0D), yRotDegrees);
        Vec3 forward = rotateLocalOffset(new Vec3(0.0D, 0.0D, 1.0D), yRotDegrees);
        Vec3 up = new Vec3(0.0D, 1.0D, 0.0D);

        double rightComp = offset.dot(right);
        double forwardComp = offset.dot(forward);
        double upComp = offset.dot(up);

        float rollRad = rollDegrees * Mth.DEG_TO_RAD;
        float rollCos = Mth.cos(rollRad);
        float rollSin = Mth.sin(rollRad);
        double rolledRight = rightComp * rollCos - upComp * rollSin;
        double rolledUp = rightComp * rollSin + upComp * rollCos;

        float pitchRad = pitchDegrees * Mth.DEG_TO_RAD;
        float pitchCos = Mth.cos(pitchRad);
        float pitchSin = Mth.sin(pitchRad);
        double pitchedUp = rolledUp * pitchCos - forwardComp * pitchSin;
        double pitchedForward = rolledUp * pitchSin + forwardComp * pitchCos;

        return worldPivot.add(right.scale(rolledRight)).add(up.scale(pitchedUp)).add(forward.scale(pitchedForward));
    }
}