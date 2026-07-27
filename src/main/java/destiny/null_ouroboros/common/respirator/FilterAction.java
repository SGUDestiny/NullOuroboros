package destiny.null_ouroboros.common.respirator;

public enum FilterAction {
    REMOVE_RIGHT,
    REMOVE_LEFT,
    PUT;

    public static FilterAction byOrdinal(int ordinal) {
        FilterAction[] values = values();
        if (ordinal < 0 || ordinal >= values.length) {
            return null;
        }
        return values[ordinal];
    }

    public boolean isRemove() {
        return this == REMOVE_RIGHT || this == REMOVE_LEFT;
    }

    public boolean isLeft() {
        return this == REMOVE_LEFT;
    }
}
