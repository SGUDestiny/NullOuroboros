package destiny.null_ouroboros.common.dimension;

import net.minecraft.world.level.Level;

public final class VergeOfRealityDimension {
    private VergeOfRealityDimension() {}

    public static boolean isVergeOfReality(Level level) {
        return level.dimension().location().getPath().contains("verge_of_reality");
    }
}