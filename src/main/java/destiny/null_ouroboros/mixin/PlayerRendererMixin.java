package destiny.null_ouroboros.mixin;

import destiny.null_ouroboros.server.entity.DusterbikeEntity;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(PlayerRenderer.class)
public class PlayerRendererMixin {
    @ModifyVariable(method = "render", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private float nullOuroboros$dusterbikeEntityYaw(float entityYaw, AbstractClientPlayer entity, float partialTicks) {
        if (entity.getVehicle() instanceof DusterbikeEntity bike) {
            return bike.getRenderYaw(partialTicks) + destiny.null_ouroboros.common.DusterbikeRiderAnimation.getHeadOffset(entity);
        }
        return entityYaw;
    }
}
