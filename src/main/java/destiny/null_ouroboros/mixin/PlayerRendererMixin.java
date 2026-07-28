package destiny.null_ouroboros.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import destiny.null_ouroboros.client.render.player_anim.PlayerAnimFirstPersonRenderScope;
import destiny.null_ouroboros.common.dusterbike.DusterbikeRiderAnimation;
import destiny.null_ouroboros.server.entity.DusterbikeEntity;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerRenderer.class)
public class PlayerRendererMixin {
    @ModifyVariable(method = "render", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private float nullOuroboros$dusterbikeEntityYaw(float entityYaw, AbstractClientPlayer entity, float partialTicks) {
        if (entity.getVehicle() instanceof DusterbikeEntity bike) {
            return bike.getRenderYaw(partialTicks) + DusterbikeRiderAnimation.getHeadOffset(entity);
        }
        return entityYaw;
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void nullOuroboros$captureFirstPersonVisibility(
            AbstractClientPlayer player,
            float entityYaw,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            CallbackInfo ci) {
        if (PlayerAnimFirstPersonRenderScope.isActive()) {
            PlayerAnimFirstPersonRenderScope.capture(((PlayerRenderer) (Object) this).getModel());
        }
    }

    @Inject(method = "setModelProperties", at = @At("TAIL"))
    private void nullOuroboros$applyFirstPersonVisibility(AbstractClientPlayer player, CallbackInfo ci) {
        if (PlayerAnimFirstPersonRenderScope.isActive()) {
            PlayerAnimFirstPersonRenderScope.apply(((PlayerRenderer) (Object) this).getModel());
        }
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void nullOuroboros$restoreFirstPersonVisibility(
            AbstractClientPlayer player,
            float entityYaw,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            CallbackInfo ci) {
        if (PlayerAnimFirstPersonRenderScope.isActive()) {
            PlayerAnimFirstPersonRenderScope.restore(((PlayerRenderer) (Object) this).getModel());
        }
    }

    @Inject(
            method = "renderNameTag(Lnet/minecraft/client/player/AbstractClientPlayer;Lnet/minecraft/network/chat/Component;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void nullOuroboros$hideFirstPersonNameTag(
            AbstractClientPlayer player,
            Component displayName,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            CallbackInfo ci) {
        if (PlayerAnimFirstPersonRenderScope.isActive()) {
            ci.cancel();
        }
    }
}
