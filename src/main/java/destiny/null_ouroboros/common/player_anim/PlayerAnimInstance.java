package destiny.null_ouroboros.common.player_anim;

import net.minecraft.resources.ResourceLocation;

public record PlayerAnimInstance(ResourceLocation animationId, PlayOptions options, long startGameTime) {
}
