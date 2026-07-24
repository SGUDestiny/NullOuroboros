package destiny.null_ouroboros.common.player_anim;

public enum AimFollowMode {
    ARMS_LOCAL,
    PARENT_ROTATION;

    public static AimFollowMode byOrdinal(int ordinal) {
        AimFollowMode[] values = values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : ARMS_LOCAL;
    }
}
