package destiny.null_ouroboros.client.render.player_anim;

import destiny.null_ouroboros.common.player_anim.PlayerAnimationMeta;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

public final class PlayerAnimationRegistry {
    private static final Map<ResourceLocation, AnimationDefinition> DEFINITIONS = new HashMap<>();

    private PlayerAnimationRegistry() {}

    public static void register(ResourceLocation id, AnimationDefinition definition) {
        DEFINITIONS.put(id, definition);
        PlayerAnimationMeta.register(id, definition.lengthInSeconds());
    }

    public static AnimationDefinition get(ResourceLocation id) {
        return DEFINITIONS.get(id);
    }
}
