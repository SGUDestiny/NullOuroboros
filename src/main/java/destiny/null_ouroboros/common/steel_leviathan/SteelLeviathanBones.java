package destiny.null_ouroboros.common.steel_leviathan;

import java.util.List;

public final class SteelLeviathanBones {
    private SteelLeviathanBones() {}

    public static final String SINEW = "sinew";
    public static final String MAW_DRILLS = "maw_drills";
    public static final String MAW = "maw";

    public static final String CONNECTION_FRONT = "connection_front";

    public static final String CONNECTION_BACK = "connection_back";

    public static final String[] HEATSINKS = {"heatsink", "heatsink2", "heatsink3", "heatsink4"};

    public static final List<String> HEAD_THRUSTERS = List.of(
            "thruster", "thruster2", "thruster3", "thruster4",
            "thruster5", "thruster6", "thruster7", "thruster8"
    );

    public static final List<String> TAIL_THRUSTERS = List.of(
            "thruster", "thruster2", "thruster3", "thruster4", "thruster5"
    );

    public static final List<String> PLUME_PREFIXES = List.of("plume", "tail_plume");

    public static String heatsinkBone(int index) {
        if (index < 0 || index >= HEATSINKS.length) {
            return HEATSINKS[0];
        }
        return HEATSINKS[index];
    }

    public static boolean isHeatsinkBone(String name) {
        if (name == null) {
            return false;
        }
        for (String heatsink : HEATSINKS) {
            if (name.equals(heatsink) || name.startsWith(heatsink)) {
                return true;
            }
        }
        return name.startsWith("shutter");
    }

    public static int heatsinkIndexForBone(String name) {
        if (name == null) {
            return -1;
        }

        for (int i = HEATSINKS.length - 1; i >= 0; i--) {
            String bone = HEATSINKS[i];
            if (name.equals(bone) || name.startsWith(bone)) {
                return i;
            }
        }

        if (name.startsWith("shutter")) {
            String suffix = name.substring("shutter".length());
            if (suffix.isEmpty()) {
                return 0;
            }
            try {
                int n = Integer.parseInt(suffix);
                return Math.min(3, (n - 1) / 6);
            } catch (NumberFormatException ignored) {
                return -1;
            }
        }
        return -1;
    }

    public static boolean isThrusterOrPlumeBone(String name) {
        if (name == null) {
            return false;
        }
        String lower = name.toLowerCase();
        return lower.contains("thruster") || lower.contains("plume") || lower.contains("drill");
    }

    public static boolean isEngineBone(String name) {
        if (name == null) {
            return false;
        }
        String lower = name.toLowerCase();
        if (lower.equals("engine")) {
            return true;
        }
        if (!lower.startsWith("engine") || lower.length() <= "engine".length()) {
            return false;
        }
        for (int i = "engine".length(); i < lower.length(); i++) {
            if (!Character.isDigit(lower.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    public static boolean isPlumeBone(String name) {
        return name != null && name.toLowerCase().contains("plume");
    }

    public static boolean isBodyGearBone(String name) {
        int n = gearNumber(name);
        return n >= 1 && n <= 8;
    }

    public static boolean isMawInternalGearBone(String name) {
        int n = gearNumber(name);
        return n >= 9 && n <= 16;
    }

    public static boolean isMawExternalGearBone(String name) {
        int n = gearNumber(name);
        return n >= 17 && n <= 24;
    }

    public static boolean isDrillBone(String name) {
        if (name == null || !name.startsWith("drill")) {
            return false;
        }
        String suffix = name.substring("drill".length());
        if (suffix.isEmpty()) {
            return true;
        }
        for (int i = 0; i < suffix.length(); i++) {
            if (!Character.isDigit(suffix.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    public static float mawGearSpinSign(String name) {
        int n = gearNumber(name);
        if (n < 9 || n > 24) {
            return 1.0F;
        }
        boolean firstOfPair = (n % 2) != 0;
        if (n <= 16) {
            return firstOfPair ? 1.0F : -1.0F;
        }
        return firstOfPair ? -1.0F : 1.0F;
    }

    private static int gearNumber(String name) {
        if (name == null || !name.startsWith("gear")) {
            return -1;
        }
        String suffix = name.substring("gear".length());
        if (suffix.isEmpty()) {
            return 1;
        }
        for (int i = 0; i < suffix.length(); i++) {
            if (!Character.isDigit(suffix.charAt(i))) {
                return -1;
            }
        }
        try {
            return Integer.parseInt(suffix);
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }
}

