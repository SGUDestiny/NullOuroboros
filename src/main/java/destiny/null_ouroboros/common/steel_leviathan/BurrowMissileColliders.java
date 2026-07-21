package destiny.null_ouroboros.common.steel_leviathan;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class BurrowMissileColliders {
    private BurrowMissileColliders() {}

    public static final Vec3 BODY_CENTER_LOCAL = new Vec3(0.0D, 0.0D, 1.95D);
    public static final double BODY_HALF_W = 0.75D;
    public static final double BODY_HALF_H = 0.75D;
    public static final double BODY_HALF_D = 2.25D;

    public static final Vec3 DRILL_CENTER_LOCAL = new Vec3(0.0D, 0.0D, -4.0D);
    public static final double DRILL_HALF_W = 0.625D;
    public static final double DRILL_HALF_H = 0.625D;
    public static final double DRILL_HALF_D = 0.75D;

    public static AABB bodyColliderBox(Vec3 origin, float yawDegrees, float pitchDegrees) {
        return colliderBox(origin, yawDegrees, pitchDegrees, BODY_CENTER_LOCAL, BODY_HALF_W, BODY_HALF_H, BODY_HALF_D);
    }

    public static AABB drillColliderBox(Vec3 origin, float yawDegrees, float pitchDegrees) {
        return colliderBox(origin, yawDegrees, pitchDegrees, DRILL_CENTER_LOCAL, DRILL_HALF_W, DRILL_HALF_H, DRILL_HALF_D);
    }

    public static AABB drillColliderBoxAtCenter(Vec3 center, float yawDegrees, float pitchDegrees) {
        double[] extents = yawPitchMorphedHalfExtents(DRILL_HALF_W, DRILL_HALF_H, DRILL_HALF_D, yawDegrees, pitchDegrees);
        return new AABB(
                center.x - extents[0], center.y - extents[1], center.z - extents[2],
                center.x + extents[0], center.y + extents[1], center.z + extents[2]);
    }

    public static Vec3 worldPointFromLocal(Vec3 origin, float yawDegrees, float pitchDegrees, Vec3 local) {
        float yaw = yawDegrees * Mth.DEG_TO_RAD;
        float pitch = pitchDegrees * Mth.DEG_TO_RAD;
        float cosPitch = Mth.cos(pitch);
        float sinPitch = Mth.sin(pitch);
        double x1 = local.x;
        double y1 = local.y * cosPitch - local.z * sinPitch;
        double z1 = local.y * sinPitch + local.z * cosPitch;
        float cosYaw = Mth.cos(yaw);
        float sinYaw = Mth.sin(yaw);
        double x2 = x1 * cosYaw - z1 * sinYaw;
        double z2 = x1 * sinYaw + z1 * cosYaw;
        return new Vec3(origin.x + x2, origin.y + y1, origin.z + z2);
    }

    public static AABB colliderBox(Vec3 origin, float yawDegrees, float pitchDegrees,
                                   Vec3 localCenter, double halfW, double halfH, double halfD) {
        Vec3 center = worldPointFromLocal(origin, yawDegrees, pitchDegrees, localCenter);
        double[] extents = yawPitchMorphedHalfExtents(halfW, halfH, halfD, yawDegrees, pitchDegrees);
        return new AABB(
                center.x - extents[0], center.y - extents[1], center.z - extents[2],
                center.x + extents[0], center.y + extents[1], center.z + extents[2]);
    }

    public static double[] yawPitchMorphedHalfExtents(double halfW, double halfH, double halfD,
                                                      float yawDegrees, float pitchDegrees) {
        Vec3 right = worldPointFromLocal(Vec3.ZERO, yawDegrees, pitchDegrees, new Vec3(1.0D, 0.0D, 0.0D));
        Vec3 up = worldPointFromLocal(Vec3.ZERO, yawDegrees, pitchDegrees, new Vec3(0.0D, 1.0D, 0.0D));
        Vec3 forward = worldPointFromLocal(Vec3.ZERO, yawDegrees, pitchDegrees, new Vec3(0.0D, 0.0D, 1.0D));

        double exactX = Math.abs(right.x) * halfW + Math.abs(up.x) * halfH + Math.abs(forward.x) * halfD;
        double exactY = Math.abs(right.y) * halfW + Math.abs(up.y) * halfH + Math.abs(forward.y) * halfD;
        double exactZ = Math.abs(right.z) * halfW + Math.abs(up.z) * halfH + Math.abs(forward.z) * halfD;

        double[] yawBlend = axisMorphedHalfExtents(halfW, halfD, yawDegrees);
        float localYaw = Mth.abs(Math.floorMod(Mth.floor(Mth.wrapDegrees(yawDegrees)), 90));
        float sin2 = Mth.sin(localYaw * Mth.DEG_TO_RAD * 2.0F);
        double squareBlend = sin2 * sin2 * 0.35D;

        double halfX = Mth.lerp(squareBlend, exactX, Math.max(exactX, yawBlend[0]));
        double halfZ = Mth.lerp(squareBlend, exactZ, Math.max(exactZ, yawBlend[1]));
        return new double[] {halfX, exactY, halfZ};
    }

    public static double[] axisMorphedHalfExtents(double halfA, double halfB, float degrees) {
        float rad = degrees * Mth.DEG_TO_RAD;
        float localDeg = Mth.abs(Math.floorMod(Mth.floor(Mth.wrapDegrees(degrees)), 90));
        float localRad = localDeg * Mth.DEG_TO_RAD;

        double cos = Mth.abs(Mth.cos(rad));
        double sin = Mth.abs(Mth.sin(rad));
        double standardA = cos * halfA + sin * halfB;
        double standardB = sin * halfA + cos * halfB;

        double maxHalf = Math.max(halfA, halfB);
        float sin2 = Mth.sin(localRad * 2.0F);
        double squareBlend = sin2 * sin2;

        double outA = Mth.lerp(squareBlend, standardA, maxHalf);
        double outB = Mth.lerp(squareBlend, standardB, maxHalf);
        return new double[] {outA, outB};
    }
}
