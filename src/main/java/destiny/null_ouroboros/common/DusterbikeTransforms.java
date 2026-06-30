package destiny.null_ouroboros.common;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class DusterbikeTransforms {
    public static final double MODEL_SCALE = 1.0D / 16.0D;

    public static final double MODEL_X_OFFSET = 0.0D;
    public static final double MODEL_Y_OFFSET = -1.51599375D;

    public static final Vec3 FRONT_COLLIDER_CENTER_LOCAL = new Vec3(0.0D, 0.46875D, 1.251575D);
    public static final Vec3 REAR_COLLIDER_CENTER_LOCAL = new Vec3(0.0D, 0.46875D, -1.248425D);
    public static final Vec3 DRIVER_LOCAL = new Vec3(0.0D, 1.067875D, -0.4678625D);

    public static final Vec3 FRONT_WHEEL_LOCAL = FRONT_COLLIDER_CENTER_LOCAL;
    public static final Vec3 REAR_WHEEL_LOCAL = REAR_COLLIDER_CENTER_LOCAL;

    public static final double WHEEL_COLLIDER_WIDTH = 0.25D;
    public static final double WHEEL_COLLIDER_HEIGHT = 0.9375D;
    public static final double WHEEL_COLLIDER_DEPTH = 0.9375D;

    public static final double WHEEL_HALF_WIDTH = WHEEL_COLLIDER_WIDTH * 0.5D;
    public static final double WHEEL_HALF_HEIGHT = WHEEL_COLLIDER_HEIGHT * 0.5D;
    public static final double WHEEL_HALF_DEPTH = WHEEL_COLLIDER_DEPTH * 0.5D;

    public static final double BODY_WIDTH = 0.9D;
    public static final double BODY_HEIGHT = 1.25D;
    public static final double BODY_DEPTH = 2.2D;

    public static final double WHEELBASE_LENGTH = FRONT_COLLIDER_CENTER_LOCAL.distanceTo(
            new Vec3(FRONT_COLLIDER_CENTER_LOCAL.x, FRONT_COLLIDER_CENTER_LOCAL.y, REAR_COLLIDER_CENTER_LOCAL.z));
    public static final Vec3 PITCH_PIVOT_LOCAL = FRONT_COLLIDER_CENTER_LOCAL.add(REAR_COLLIDER_CENTER_LOCAL).scale(0.5D);

    private DusterbikeTransforms() {}

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

    public static AABB wheelColliderBox(double centerX, double centerY, double centerZ) {
        return new AABB(
                centerX - WHEEL_HALF_WIDTH, centerY - WHEEL_HALF_HEIGHT, centerZ - WHEEL_HALF_DEPTH,
                centerX + WHEEL_HALF_WIDTH, centerY + WHEEL_HALF_HEIGHT, centerZ + WHEEL_HALF_DEPTH
        );
    }

    public static AABB bodyColliderBox(double originX, double originY, double originZ) {
        double halfW = BODY_WIDTH * 0.5D;
        double halfD = BODY_DEPTH * 0.5D;
        return new AABB(
                originX - halfW, originY, originZ - halfD,
                originX + halfW, originY + BODY_HEIGHT, originZ + halfD
        );
    }

    public static Vec3 worldPointFromLocal(Vec3 entityPos, float yRotDegrees, float pitchDegrees, Vec3 localPoint) {
        Vec3 worldPivot = entityPos.add(rotateLocalOffset(PITCH_PIVOT_LOCAL, yRotDegrees));
        Vec3 worldPoint = entityPos.add(rotateLocalOffset(localPoint, yRotDegrees));
        Vec3 offset = worldPoint.subtract(worldPivot);

        Vec3 right = rotateLocalOffset(new Vec3(1.0D, 0.0D, 0.0D), yRotDegrees);
        Vec3 forward = rotateLocalOffset(new Vec3(0.0D, 0.0D, 1.0D), yRotDegrees);
        Vec3 up = new Vec3(0.0D, 1.0D, 0.0D);

        double rightComp = offset.dot(right);
        double forwardComp = offset.dot(forward);
        double upComp = offset.dot(up);

        float pitchRad = pitchDegrees * Mth.DEG_TO_RAD;
        float cos = Mth.cos(pitchRad);
        float sin = Mth.sin(pitchRad);
        double pitchedUp = upComp * cos - forwardComp * sin;
        double pitchedForward = upComp * sin + forwardComp * cos;

        return worldPivot.add(right.scale(rightComp)).add(up.scale(pitchedUp)).add(forward.scale(pitchedForward));
    }
}