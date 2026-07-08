package destiny.null_ouroboros.common;

import destiny.null_ouroboros.server.entity.DusterbikePhysics;
import net.minecraft.util.Mth;

public final class DusterbikeEngineSoundConstants {
    public static final int IGNITION_START_TICKS = 9;
    public static final int IGNITION_ATTEMPT_TICKS = 4;
    public static final int CROSSFADE_TICKS = 10;
    public static final float CROSSFADE_SPEED = 1.0F / CROSSFADE_TICKS;

    public static final float IGNITION_SUCCESS_CHANCE = 0.33F;
    public static final int MAX_IGNITION_ATTEMPTS = 5;
    public static final float SPEED_EPSILON = DusterbikePhysics.SPEED_EPSILON;

    public static final double KEY_INTERACTION_REACH = 5.0D;

    public static final float IDLE_PITCH = 1.0F;

    public static final float GEAR_1_START_PITCH = 0.5F;
    public static final float GEAR_1_TARGET_PITCH = 1.5F;

    public static final float GEAR_2_START_PITCH = 0F;
    public static final float GEAR_2_TARGET_PITCH = 1F;

    public static final float GEAR_3_START_PITCH = 0F;
    public static final float GEAR_3_TARGET_PITCH = 1F;

    public static final float REVERSE_START_PITCH = 0.5F;
    public static final float REVERSE_TARGET_PITCH = 1.5F;

    public enum EngineLoopProfile {
        IDLE,
        GEAR_1,
        GEAR_2,
        GEAR_3,
        REVERSE
    }

    private DusterbikeEngineSoundConstants() {}

    public static float computePitch(EngineLoopProfile profile, float speedRatio) {
        return switch (profile) {
            case IDLE -> IDLE_PITCH;
            case GEAR_1 -> Mth.lerp(speedRatio, GEAR_1_START_PITCH, GEAR_1_TARGET_PITCH);
            case GEAR_2 -> Mth.lerp(speedRatio, GEAR_2_START_PITCH, GEAR_2_TARGET_PITCH);
            case GEAR_3 -> Mth.lerp(speedRatio, GEAR_3_START_PITCH, GEAR_3_TARGET_PITCH);
            case REVERSE -> Mth.lerp(speedRatio, REVERSE_START_PITCH, REVERSE_TARGET_PITCH);
        };
    }
}
