package destiny.null_ouroboros.common.steel_leviathan;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class SteelLeviathanSinew {
    private SteelLeviathanSinew() {}

    public static float[] averageBridgeLocalDegrees(float selfYaw, float selfPitch,
                                                    float otherYaw, float otherPitch) {
        float midYaw = selfYaw + Mth.wrapDegrees(otherYaw - selfYaw) * 0.5F;
        float midPitch = selfPitch + Mth.wrapDegrees(otherPitch - selfPitch) * 0.5F;
        float dYaw = Mth.wrapDegrees(midYaw - selfYaw);
        float dPitch = Mth.wrapDegrees(midPitch - selfPitch);
        return new float[]{dPitch, dYaw, 0.0F};
    }

    public static Vec3 facingFromYawPitch(float yawDeg, float pitchDeg) {
        float yaw = yawDeg * Mth.DEG_TO_RAD;
        float pitch = pitchDeg * Mth.DEG_TO_RAD;
        float cosPitch = Mth.cos(pitch);
        return new Vec3(-Mth.sin(yaw) * cosPitch, -Mth.sin(pitch), Mth.cos(yaw) * cosPitch);
    }
}

