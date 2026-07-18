package destiny.null_ouroboros.common.dusterbike;

public enum DusterbikeGear {
    R(DusterbikeGearConstants.MAX_REVERSE_SPEED),
    N(0.0F),
    GEAR_1(DusterbikeGearConstants.MAX_GEAR_1_SPEED),
    GEAR_2(DusterbikeGearConstants.MAX_GEAR_2_SPEED),
    GEAR_3(DusterbikeGearConstants.MAX_GEAR_3_SPEED);

    private final float maxSpeed;

    DusterbikeGear(float maxSpeed) {
        this.maxSpeed = maxSpeed;
    }

    public float maxSpeed() {
        return maxSpeed;
    }

    public boolean allowsForwardThrottle() {
        return this == GEAR_1 || this == GEAR_2 || this == GEAR_3;
    }

    public boolean allowsReverseThrottle() {
        return this == R;
    }

    public String translationKey() {
        return "message.null_ouroboros.dusterbike.gear." + name().toLowerCase().replace("gear_", "");
    }

    public DusterbikeGear shift(int direction) {
        int next = ordinal() + direction;
        if (next < 0 || next >= values().length) {
            return this;
        }
        return values()[next];
    }
}
