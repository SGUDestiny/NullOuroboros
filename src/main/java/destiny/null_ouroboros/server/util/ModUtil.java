package destiny.null_ouroboros.server.util;

import net.minecraft.world.level.Level;

public class ModUtil {
    public float getBoundRandomFloat(Level level, float origin, float limit) {
        return origin + (limit - origin) * level.getRandom().nextFloat();
    }

    public static float getBoundRandomFloatStatic(Level level, float origin, float limit) {
        return origin + (limit - origin) * level.getRandom().nextFloat();
    }
}
