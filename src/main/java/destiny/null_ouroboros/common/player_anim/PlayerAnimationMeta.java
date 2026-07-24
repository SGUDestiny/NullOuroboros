package destiny.null_ouroboros.common.player_anim;

import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayerAnimationMeta {
    private static final Map<ResourceLocation, Float> LENGTH_SECONDS = new ConcurrentHashMap<>();

    private PlayerAnimationMeta() {}

    public static void register(ResourceLocation id, float lengthInSeconds) {
        LENGTH_SECONDS.put(id, lengthInSeconds);
    }

    public static float lengthInSeconds(ResourceLocation id) {
        return LENGTH_SECONDS.getOrDefault(id, -1.0F);
    }
}
