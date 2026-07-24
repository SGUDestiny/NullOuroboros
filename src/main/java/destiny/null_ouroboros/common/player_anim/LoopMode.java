package destiny.null_ouroboros.common.player_anim;

public enum LoopMode {
    LOOP,
    PLAY_ONCE,
    HOLD_LAST;

    public static LoopMode byOrdinal(int ordinal) {
        LoopMode[] values = values();
        if (ordinal < 0 || ordinal >= values.length) {
            return PLAY_ONCE;
        }
        return values[ordinal];
    }
}
