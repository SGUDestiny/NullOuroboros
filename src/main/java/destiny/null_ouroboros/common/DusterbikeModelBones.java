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

    private DusterbikeModelBones() {}

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
