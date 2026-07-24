package destiny.null_ouroboros.client.render.player_anim;

import destiny.null_ouroboros.common.player_anim.LoopMode;
import destiny.null_ouroboros.common.player_anim.PlayerAnimInstance;
import destiny.null_ouroboros.common.player_anim.PlayOptions;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayerAnimController {
    private static final Map<UUID, PlayerAnimInstance> ACTIVE = new ConcurrentHashMap<>();

    private PlayerAnimController() {}

    public static void play(UUID playerId, ResourceLocation animationId, long startGameTime, PlayOptions options) {
        PlayerAnimAimFollow.clear(playerId);
        ACTIVE.put(playerId, new PlayerAnimInstance(animationId, options, startGameTime));
    }

    public static void cancel(UUID playerId, ResourceLocation animationId) {
        if (animationId == null) {
            ACTIVE.remove(playerId);
            PlayerAnimItemLocators.clear(playerId);
            PlayerAnimAimFollow.clear(playerId);
            return;
        }
        PlayerAnimInstance current = ACTIVE.get(playerId);
        if (current != null && current.animationId().equals(animationId)) {
            ACTIVE.remove(playerId);
            PlayerAnimItemLocators.clear(playerId);
            PlayerAnimAimFollow.clear(playerId);
        }
    }

    public static void clear(UUID playerId) {
        ACTIVE.remove(playerId);
        PlayerAnimItemLocators.clear(playerId);
        PlayerAnimAimFollow.clear(playerId);
    }

    public static PlayerAnimInstance get(LivingEntity entity) {
        PlayerAnimInstance instance = ACTIVE.get(entity.getUUID());
        if (instance == null) {
            return null;
        }
        if (shouldExpire(entity, instance)) {
            ACTIVE.remove(entity.getUUID());
            PlayerAnimItemLocators.clear(entity.getUUID());
            PlayerAnimAimFollow.clear(entity.getUUID());
            return null;
        }
        return instance;
    }

    public static boolean shouldExpire(LivingEntity entity, PlayerAnimInstance instance) {
        if (instance.options().loopMode() != LoopMode.PLAY_ONCE) {
            return false;
        }
        AnimationDefinition definition = PlayerAnimationRegistry.get(instance.animationId());
        if (definition == null) {
            return false;
        }
        long elapsedTicks = entity.level().getGameTime() - instance.startGameTime();
        long lengthTicks = Math.max(1L, Math.round(definition.lengthInSeconds() * 20.0F));
        return elapsedTicks > lengthTicks;
    }

    public static long animationTimeMs(
            LivingEntity entity, PlayerAnimInstance instance, AnimationDefinition definition, float partialTick) {
        long elapsedMs = Math.max(
                0L,
                (long) ((entity.level().getGameTime() - instance.startGameTime()) * 50L + partialTick * 50.0F));
        long lengthMs = Math.max(1L, Math.round(definition.lengthInSeconds() * 1000.0F));
        return switch (instance.options().loopMode()) {
            case LOOP -> elapsedMs % lengthMs;
            case PLAY_ONCE -> Math.min(elapsedMs, lengthMs);
            case HOLD_LAST -> Math.min(elapsedMs, lengthMs);
        };
    }
}
