package destiny.null_ouroboros.common.steel_leviathan;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class SteelLeviathanModelBones {
    private SteelLeviathanModelBones() {}

    public static final double SEGMENT_BODY_CENTER_Z = 2.0D;

    public static final Vec3 SEGMENT_HEATSINK_BASE = new Vec3(0.0D, 2.5D, 2.0D);

    public static final Vec3 HEAD_HEATSINK_BASE = new Vec3(0.0D, 2.5D, 2.0D);

    private static final float[] HEATSINK_ROLL_DEG = {0.0F, 90.0F, -180.0F, -90.0F};

    private static final float[] TAIL_MISSILE_ROLL_DEG = {-135.0F, -45.0F, 45.0F, 135.0F};

    private static final float[] MAW_MISSILE_ROLL_DEG = {
            0.0F, -45.0F, -90.0F, -135.0F, -180.0F, 135.0F, 90.0F, 45.0F
    };

    private static final float MAW_THRUSTER_YAW_DEG = 22.5F;

    private static final Vec3 TAIL_THRUSTER_PARENT = new Vec3(-1.0D / 16.0D, 0.0D, 45.0D / 16.0D);

    private static final Vec3 TAIL_THRUSTER_OFFSET = new Vec3(63.0D / 16.0D, 0.0D, 31.0D / 16.0D);

    private static final Vec3 MAW_THRUSTER_PARENT = new Vec3(0.0D, 0.0D, -85.0D / 16.0D);

    private static final Vec3 MAW_THRUSTER_OFFSET = new Vec3(63.0D / 16.0D, 0.0D, 24.0D / 16.0D);

    public static final int TAIL_MISSILE_COUNT = 4;

    public static final int MAW_MISSILE_COUNT = 8;

    public static Vec3 heatsinkLocal(boolean head, int index) {
        Vec3 base = head ? HEAD_HEATSINK_BASE : SEGMENT_HEATSINK_BASE;
        int slot = Mth.clamp(index, 0, HEATSINK_ROLL_DEG.length - 1);
        return rotateAroundZ(base, HEATSINK_ROLL_DEG[slot]);
    }

    public static Vec3 tailMissileLocal(int index) {
        int slot = Mth.clamp(index, 0, TAIL_MISSILE_ROLL_DEG.length - 1);
        Vec3 geo = TAIL_THRUSTER_PARENT.add(rotateAroundZ(TAIL_THRUSTER_OFFSET, TAIL_MISSILE_ROLL_DEG[slot]));
        return geoToGameplay(geo);
    }

    public static Vec3 mawMissileLocal(int index) {
        int slot = Mth.clamp(index, 0, MAW_MISSILE_ROLL_DEG.length - 1);
        Vec3 yawed = rotateAroundY(MAW_THRUSTER_OFFSET, MAW_THRUSTER_YAW_DEG);
        Vec3 geo = MAW_THRUSTER_PARENT.add(rotateAroundZ(yawed, MAW_MISSILE_ROLL_DEG[slot]));
        return geoToGameplay(geo);
    }

    public static Vec3 mawMissileLaunchLocal(int index) {
        int slot = Mth.clamp(index, 0, MAW_MISSILE_ROLL_DEG.length - 1);
        Vec3 drill = rotateAroundY(new Vec3(0.0D, 0.0D, -1.0D), MAW_THRUSTER_YAW_DEG);
        return geoToGameplay(rotateAroundZ(drill, MAW_MISSILE_ROLL_DEG[slot]));
    }

    private static Vec3 geoToGameplay(Vec3 geo) {
        return new Vec3(-geo.x, geo.y, -geo.z);
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

    public static Vec3 rotateAroundY(Vec3 local, float degrees) {
        float rad = degrees * Mth.DEG_TO_RAD;
        float cos = Mth.cos(rad);
        float sin = Mth.sin(rad);
        return new Vec3(
                local.x * cos + local.z * sin,
                local.y,
                -local.x * sin + local.z * cos);
    }
}
