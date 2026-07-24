package destiny.null_ouroboros.common.player_anim;

import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayerAnimTracker {
    private static final Map<UUID, PlayerAnimInstance> ACTIVE = new ConcurrentHashMap<>();

    private PlayerAnimTracker() {}

    public static void set(UUID playerId, PlayerAnimInstance instance) {
        ACTIVE.put(playerId, instance);
    }

    public static PlayerAnimInstance get(UUID playerId) {
        return ACTIVE.get(playerId);
    }

    public static PlayerAnimInstance clear(UUID playerId) {
        return ACTIVE.remove(playerId);
    }

    public static PlayerAnimInstance clearMatching(UUID playerId, ResourceLocation animationId) {
        PlayerAnimInstance current = ACTIVE.get(playerId);
        if (current != null && current.animationId().equals(animationId)) {
            return ACTIVE.remove(playerId);
        }
        return null;
    }
}
