package destiny.null_ouroboros.server.util;

public final class WindCompass {
    private static final String KEY_PREFIX = "message.null_ouroboros.wind.";

    private WindCompass() {}

    public static String directionKey(float yaw) {
        int normalized = Math.floorMod(Math.round(yaw), 360);
        int index = (int) Math.floor((normalized + 22.5) / 45.0) % 8;
        return KEY_PREFIX + switch (index) {
            case 0 -> "south";
            case 1 -> "south_west";
            case 2 -> "west";
            case 3 -> "north_west";
            case 4 -> "north";
            case 5 -> "north_east";
            case 6 -> "east";
            default -> "south_east";
        };
    }

    public static int normalizedDegrees(float yaw) {
        return Math.floorMod(Math.round(yaw), 360);
    }
}
