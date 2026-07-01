package destiny.null_ouroboros.common;

import net.minecraft.world.phys.Vec3;

public final class DusterbikeModelBones {
    public static final double BIKE_X = 1.25D;
    public static final double BIKE_Y = 16.0D;
    public static final double BIKE_Z = 0.0D;

    public static final double BODY_X = 1.25D;
    public static final double BODY_Y = 2.0D;
    public static final double BODY_Z = 1.5252D;

    public static final double ENGINE_X = -3.5D;
    public static final double ENGINE_Y = -8.5D;
    public static final double ENGINE_Z = 0.0D;

    public static final double DRIVER_X = 0.0D;
    public static final double DRIVER_Y = -10.8301D;
    public static final double DRIVER_Z = -9.011D;

    public static final double KEY_X = 0.0D;
    public static final double KEY_Y = 0.0D;
    public static final double KEY_Z = 0.0D;

    public static final double KEY_INTERACTION_COLLIDER_X = 0.0D;
    public static final double KEY_INTERACTION_COLLIDER_Y = 0.0D;
    public static final double KEY_INTERACTION_COLLIDER_Z = 0.0D;

    public static final double KEY_INTERACTION_COLLIDER_HALF_X = 1.0D;
    public static final double KEY_INTERACTION_COLLIDER_HALF_Y = 2.0D;
    public static final double KEY_INTERACTION_COLLIDER_HALF_Z = 1.0D;

    public static final double EXHAUST_X = 5.25D;
    public static final double EXHAUST_Y = 1.5D;
    public static final double EXHAUST_Z = -7.4748D;

    public static final double EXHAUST_UPPER_SMOKE_X = -1.0D;
    public static final double EXHAUST_UPPER_SMOKE_Y = -1.0D;
    public static final double EXHAUST_UPPER_SMOKE_Z = -21.0D;

    public static final double EXHAUST_LOWER_X = -1.0D;
    public static final double EXHAUST_LOWER_Y = 3.5D;
    public static final double EXHAUST_LOWER_Z = -20.0D;

    public static final double EXHAUST_LOWER_SMOKE_X = 0.0D;
    public static final double EXHAUST_LOWER_SMOKE_Y = -1.5D;
    public static final double EXHAUST_LOWER_SMOKE_Z = -1.0D;

    private DusterbikeModelBones() {}

    public static Vec3 deriveExhaustUpperSmokeEntityLocal() {
        return mirrorEntityLocalX(bikeLocalOffsetToEntityLocal(
                EXHAUST_X + EXHAUST_UPPER_SMOKE_X,
                EXHAUST_Y + EXHAUST_UPPER_SMOKE_Y,
                EXHAUST_Z + EXHAUST_UPPER_SMOKE_Z));
    }

    public static Vec3 deriveExhaustLowerSmokeEntityLocal() {
        return mirrorEntityLocalX(bikeLocalOffsetToEntityLocal(
                EXHAUST_X + EXHAUST_LOWER_X + EXHAUST_LOWER_SMOKE_X,
                EXHAUST_Y + EXHAUST_LOWER_Y + EXHAUST_LOWER_SMOKE_Y,
                EXHAUST_Z + EXHAUST_LOWER_Z + EXHAUST_LOWER_SMOKE_Z));
    }

    private static Vec3 mirrorEntityLocalX(Vec3 entityLocal) {
        return new Vec3(-entityLocal.x, entityLocal.y, entityLocal.z);
    }

    private static Vec3 bikeLocalOffsetToEntityLocal(double bikeLocalX, double bikeLocalY, double bikeLocalZ) {
        double worldX = BIKE_X - bikeLocalX;
        double worldY = BIKE_Y + bikeLocalY;
        double worldZ = BIKE_Z - bikeLocalZ;
        return DusterbikeTransforms.modelPixelPointToEntityLocal(worldX, worldY, worldZ);
    }

    public static Vec3 deriveKeyInteractionColliderCenterEntityLocal() {
        double localX = BODY_X + ENGINE_X + KEY_X + KEY_INTERACTION_COLLIDER_X;
        double localY = BODY_Y + ENGINE_Y + KEY_Y + KEY_INTERACTION_COLLIDER_Y;
        double localZ = BODY_Z + ENGINE_Z + KEY_Z + KEY_INTERACTION_COLLIDER_Z;

        double worldX = BIKE_X - localX;
        double worldY = BIKE_Y + localY;
        double worldZ = BIKE_Z - localZ;

        Vec3 entityLocal = DusterbikeTransforms.modelPixelPointToEntityLocal(worldX, worldY, worldZ);
        return new Vec3(-entityLocal.x, entityLocal.y, entityLocal.z);
    }
}
