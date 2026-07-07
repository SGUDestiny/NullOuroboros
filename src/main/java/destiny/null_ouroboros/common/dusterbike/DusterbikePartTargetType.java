package destiny.null_ouroboros.common.dusterbike;

import net.minecraft.world.phys.Vec3;

public enum DusterbikePartTargetType {
    FRONT_LIGHT(1, DusterbikePartType.FRONT_LIGHT,
            new Vec3(0, 1.25, 0.8),
            0.35, 0.19, 0.2),

    REAR_LIGHT(2, DusterbikePartType.REAR_LIGHT,
            new Vec3(0, 0.95, -1.35),
            0.22, 0.14, 0.17),

    BATTERY(3, DusterbikePartType.BATTERY,
            new Vec3(0, 0.7, -0.45),
            0.2, 0.12, 0.19),

    FUEL_INTAKE(4, null,
            new Vec3(0, 1.35, 0.32),
            0.12, 0.06, 0.12),

    ENGINE(5, DusterbikePartType.ENGINE,
            new Vec3(0, 0.65, 0.1),
            0.22, 0.3, 0.30);

    private final int id;
    private final DusterbikePartType partType;
    private final Vec3 localCenter;
    private final double halfWidth;
    private final double halfHeight;
    private final double halfDepth;

    DusterbikePartTargetType(int id, DusterbikePartType partType,
                             Vec3 localCenter,
                             double halfWidth, double halfHeight, double halfDepth) {
        this.id = id;
        this.partType = partType;
        this.localCenter = localCenter;
        this.halfWidth = halfWidth;
        this.halfHeight = halfHeight;
        this.halfDepth = halfDepth;
    }

    public int id() { return id; }
    public DusterbikePartType partType() { return partType; }
    public Vec3 localCenter() { return localCenter; }
    public double halfWidth() { return halfWidth; }
    public double halfHeight() { return halfHeight; }
    public double halfDepth() { return halfDepth; }

    @org.jetbrains.annotations.Nullable
    public static DusterbikePartTargetType byId(int id) {
        for (DusterbikePartTargetType type : values()) {
            if (type.id == id) return type;
        }
        return null;
    }
}