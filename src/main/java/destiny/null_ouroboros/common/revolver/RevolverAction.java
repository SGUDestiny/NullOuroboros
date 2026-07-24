package destiny.null_ouroboros.common.revolver;

public enum RevolverAction {
    TOGGLE_CYLINDER,
    EJECT_SELECTED,
    EJECT_ALL,
    INSERT_SELECTED,
    ROTATE_FORWARD,
    ROTATE_BACKWARD,
    FIRE,
    TOGGLE_COCK,
    SPEEDLOAD;

    public static RevolverAction byOrdinal(int ordinal) {
        RevolverAction[] values = values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : null;
    }
}
