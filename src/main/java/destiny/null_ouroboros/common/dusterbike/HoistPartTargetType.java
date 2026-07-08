package destiny.null_ouroboros.common.dusterbike;

import net.minecraft.world.phys.Vec3;

public enum HoistPartTargetType {
    ENGINE(0, DusterbikePartType.ENGINE, new Vec3(
            0.0, 1.0, -0.55),
            0.22, 0.1, 0.3),
    PISTON_FRONT(1, DusterbikePartType.PISTON_FRONT, new Vec3(
            0.0, 1.3, -0.7),
            0.15, 0.3, 0.15),
    PISTON_REAR(2, DusterbikePartType.PISTON_REAR, new Vec3(
            0.0, 1.3, -0.4),
            0.15, 0.3, 0.15),
    SPARK_PLUG_FRONT(3, DusterbikePartType.SPARK_PLUG_FRONT, new Vec3(
            0.0, 1.3, -1.05),
            0.1, 0.1, 0.2),
    SPARK_PLUG_REAR(4, DusterbikePartType.SPARK_PLUG_REAR, new Vec3(
            0.0, 1.3, -0.05),
            0.1, 0.1, 0.2),
    KEY(5, DusterbikePartType.KEY, new Vec3(
            -0.24, 1.5, -0.55),
            0.05, 0.1, 0.05);

    private final int id;
    private final DusterbikePartType partType;
    private final Vec3 localCenter;
    private final double halfWidth, halfHeight, halfDepth;

    HoistPartTargetType(int id, DusterbikePartType partType, Vec3 localCenter,
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

    @javax.annotation.Nullable
    public static HoistPartTargetType byId(int id) {
        for (HoistPartTargetType type : values()) {
            if (type.id == id) return type;
        }
        return null;
    }
}