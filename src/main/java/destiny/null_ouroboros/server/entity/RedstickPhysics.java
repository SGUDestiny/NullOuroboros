package destiny.null_ouroboros.server.entity;

import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;

public final class RedstickPhysics {
    public static final float STICK_LENGTH = 0.5F;
    public static final double GRAVITY = -0.04D;
    public static final int CONSTRAINT_ITERATIONS = 3;

    private RedstickPhysics() {}

    public static void snapToStickLength(RedstickEndEntity topEnd, RedstickEndEntity bottomEnd) {
        Vec3 topPos = topEnd.position();
        Vec3 bottomPos = bottomEnd.position();
        Vec3 axis = topPos.subtract(bottomPos);
        Vec3 midpoint = topPos.add(bottomPos).scale(0.5D);
        Vec3 halfAxis;

        if (axis.lengthSqr() < 1.0E-6D) {
            halfAxis = new Vec3(0.0D, STICK_LENGTH * 0.5D, 0.0D);
        } else {
            halfAxis = axis.normalize().scale(STICK_LENGTH * 0.5D);
        }

        topEnd.setPos(midpoint.add(halfAxis));
        bottomEnd.setPos(midpoint.subtract(halfAxis));
    }

    public static void enforceStickLength(RedstickEndEntity topEnd, RedstickEndEntity bottomEnd) {
        for (int i = 0; i < CONSTRAINT_ITERATIONS; i++) {
            Vec3 topPos = topEnd.position();
            Vec3 bottomPos = bottomEnd.position();
            Vec3 axis = topPos.subtract(bottomPos);
            double length = axis.length();
            if (length < 1.0E-6D) {
                moveEndpoint(topEnd, new Vec3(0.0D, STICK_LENGTH, 0.0D));
                continue;
            }

            double error = length - STICK_LENGTH;
            if (Math.abs(error) < 1.0E-4D) {
                continue;
            }

            Vec3 correction = axis.scale(error / length);
            boolean topBlocked = isBlocked(topEnd);
            boolean bottomBlocked = isBlocked(bottomEnd);

            if (topBlocked && !bottomBlocked) {
                moveEndpoint(bottomEnd, correction);
            } else if (bottomBlocked && !topBlocked) {
                moveEndpoint(topEnd, correction.scale(-1.0D));
            } else {
                moveEndpoint(topEnd, correction.scale(-0.5D));
                moveEndpoint(bottomEnd, correction.scale(0.5D));
            }
        }
    }

    private static void moveEndpoint(RedstickEndEntity end, Vec3 correction) {
        if (correction.lengthSqr() < 1.0E-8D) return;
        end.move(MoverType.SELF, correction);
    }

    private static boolean isBlocked(RedstickEndEntity end) {
        return end.onGround() || end.horizontalCollision || end.verticalCollision;
    }
}
