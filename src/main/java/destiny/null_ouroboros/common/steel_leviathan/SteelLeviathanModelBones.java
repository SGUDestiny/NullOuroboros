package destiny.null_ouroboros.common.steel_leviathan;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class SteelLeviathanModelBones {
    private SteelLeviathanModelBones() {}

    public static final double SEGMENT_BODY_CENTER_Z = 2.0D;

    public static final Vec3 SEGMENT_HEATSINK_BASE = new Vec3(0.0D, 2.5D, 2.0D);

    public static final Vec3 HEAD_HEATSINK_BASE = new Vec3(0.0D, 2.5D, -2.0D);

    private static final float[] HEATSINK_ROLL_DEG = {0.0F, 90.0F, -180.0F, -90.0F};

    public static Vec3 heatsinkLocal(boolean head, int index) {
        Vec3 base = head ? HEAD_HEATSINK_BASE : SEGMENT_HEATSINK_BASE;
        int slot = Mth.clamp(index, 0, HEATSINK_ROLL_DEG.length - 1);
        return rotateAroundZ(base, HEATSINK_ROLL_DEG[slot]);
    }

    public static Vec3 rotateAroundZ(Vec3 local, float degrees) {
        float rad = degrees * Mth.DEG_TO_RAD;
        float cos = Mth.cos(rad);
        float sin = Mth.sin(rad);
        return new Vec3(
                local.x * cos + local.y * sin,
                -local.x * sin + local.y * cos,
                local.z);
    }
}

