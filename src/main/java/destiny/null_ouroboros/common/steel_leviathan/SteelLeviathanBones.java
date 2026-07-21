package destiny.null_ouroboros.common.steel_leviathan;

import java.util.List;

public final class SteelLeviathanBones {
    private SteelLeviathanBones() {}

    public static final String SINEW = "sinew";
    public static final String MAW_DRILLS = "maw_drills";
    public static final String MAW = "maw";
    public static final String SCAN_ORIGIN = "scan_origin";

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

    public static final List<String> TAIL_MISSILE_THRUSTERS = List.of(
            "thruster2", "thruster3", "thruster4", "thruster5"
    );

    public static final List<String> MAW_MISSILE_THRUSTERS = List.of(
            "thruster", "thruster2", "thruster3", "thruster4",
            "thruster5", "thruster6", "thruster7", "thruster8"
    );

    public static final List<String> PLUME_PREFIXES = List.of("plume", "tail_plume");

    public static String tailMissileThrusterBone(int index) {
        if (index < 0 || index >= TAIL_MISSILE_THRUSTERS.size()) {
            return TAIL_MISSILE_THRUSTERS.get(0);
        }
        return TAIL_MISSILE_THRUSTERS.get(index);
    }

    public static String mawMissileThrusterBone(int index) {
        if (index < 0 || index >= MAW_MISSILE_THRUSTERS.size()) {
            return MAW_MISSILE_THRUSTERS.get(0);
        }
        return MAW_MISSILE_THRUSTERS.get(index);
    }

    public static int tailMissileIndexForBone(String name) {
        if (name == null) {
            return -1;
        }
        for (int i = 0; i < TAIL_MISSILE_THRUSTERS.size(); i++) {
            if (name.equals(TAIL_MISSILE_THRUSTERS.get(i))) {
                return i;
            }
        }
        return -1;
    }

    public static int mawMissileIndexForBone(String name) {
        if (name == null) {
            return -1;
        }
        for (int i = 0; i < MAW_MISSILE_THRUSTERS.size(); i++) {
            if (name.equals(MAW_MISSILE_THRUSTERS.get(i))) {
                return i;
            }
        }
        return -1;
    }

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

    public static boolean isEmissiveBone(String name) {
        return matchesPrefixedName(name, "emissive");
    }

    public static boolean isBlinkerBone(String name) {
        return matchesPrefixedName(name, "blinker");
    }

    public static int blinkerIndex(String name) {
        if (!isBlinkerBone(name)) {
            return 0;
        }
        String suffix = name.substring("blinker".length());
        if (suffix.isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(suffix);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    public static int heatsinkIndexForEmissiveBone(String name) {
        if (!isEmissiveBone(name)) {
            return -1;
        }
        String suffix = name.substring("emissive".length());
        if (suffix.isEmpty()) {
            return -1;
        }
        try {
            int n = Integer.parseInt(suffix);
            if ((n >= 20 && n <= 24) || n == 28) {
                return 0;
            }
            if ((n >= 25 && n <= 27) || (n >= 29 && n <= 32)) {
                return 1;
            }
            if (n >= 33 && n <= 39) {
                return 2;
            }
            if (n >= 40 && n <= 46) {
                return 3;
            }
        } catch (NumberFormatException ignored) {
            return -1;
        }
        return -1;
    }

    private static boolean matchesPrefixedName(String name, String prefix) {
        if (name == null) {
            return false;
        }
        String lower = name.toLowerCase();
        if (lower.equals(prefix)) {
            return true;
        }
        if (!lower.startsWith(prefix) || lower.length() <= prefix.length()) {
            return false;
        }
        for (int i = prefix.length(); i < lower.length(); i++) {
            if (!Character.isDigit(lower.charAt(i))) {
                return false;
            }
        }
        return true;
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

