package destiny.null_ouroboros.common.dusterbike;

import net.minecraft.world.phys.Vec3;

public enum DusterbikePartTargetType {
    FRONT_LIGHT(1, DusterbikePartType.FRONT_LIGHT, new Vec3(0.0D, 0.48D, 0.60D), 0.32D, 0.22D, 0.28D),
    REAR_LIGHT(2, DusterbikePartType.REAR_LIGHT, new Vec3(0.0D, 0.33D, -1.10D), 0.32D, 0.22D, 0.24D),
    BATTERY(3, DusterbikePartType.BATTERY, new Vec3(0.0D, 0.47D, -0.55D), 0.28D, 0.2D, 0.25D),
    FUEL_INTAKE(4, null, new Vec3(0.0D, 1.03D, 0.35D), 0.22D, 0.12D, 0.22D),
    ENGINE(5, DusterbikePartType.ENGINE, new Vec3(0.0D, 0.45D, 0.1D), 0.42D, 0.32D, 0.45D);

    private final int id;
    private final DusterbikePartType partType;
    private final Vec3 localCenter;
    private final double halfWidth;
    private final double halfHeight;
    private final double halfDepth;

    DusterbikePartTargetType(
            int id,
            DusterbikePartType partType,
            Vec3 localCenter,
            double halfWidth,
            double halfHeight,
            double halfDepth) {
        this.id = id;
        this.partType = partType;
        this.localCenter = localCenter;
        this.halfWidth = halfWidth;
        this.halfHeight = halfHeight;
        this.halfDepth = halfDepth;
    }

    public int id() {
        return id;
    }

    public DusterbikePartType partType() {
        return partType;
    }

    public Vec3 localCenter() {
        return localCenter;
    }

    public double halfWidth() {
        return halfWidth;
    }

    public double halfHeight() {
        return halfHeight;
    }

    public double halfDepth() {
        return halfDepth;
    }

    @org.jetbrains.annotations.Nullable
    public static DusterbikePartTargetType byId(int id) {
        for (DusterbikePartTargetType type : values()) {
            if (type.id == id) {
                return type;
            }
        }
        return null;
    }
}
