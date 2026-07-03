package destiny.null_ouroboros.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import destiny.null_ouroboros.client.render.DusterbikeHumanoidRenderScope;
import destiny.null_ouroboros.server.entity.DusterbikeEntity;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin<T extends LivingEntity> {
    @Inject(method = "render", at = @At("HEAD"))
    private void nullOuroboros$beginDusterbikeHumanoidRender(
            T entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer,
            int packedLight, CallbackInfo ci) {
        DusterbikeHumanoidRenderScope.beginEntityRenderSetup();
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void nullOuroboros$endDusterbikeHumanoidRender(
            T entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer,
            int packedLight, CallbackInfo ci) {
        DusterbikeHumanoidRenderScope.endEntityRenderSetup();
    }

    @Inject(method = "setupRotations", at = @At("HEAD"), cancellable = true)
    private void nullOuroboros$dusterbikeSetupRotations(
            T entity, PoseStack poseStack, float bob, float bodyYaw, float partialTick, CallbackInfo ci) {
        if (!(entity.getVehicle() instanceof DusterbikeEntity bike)) {
            return;
        }
        if (entity.getPose() == Pose.SLEEPING) {
            return;
        }

        ci.cancel();
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - bike.getRenderYaw(partialTick)));

        float roll = bike.getRenderRoll(partialTick);
        float pitch = bike.getRenderPitch(partialTick);
        poseStack.mulPose(Axis.ZP.rotationDegrees(roll));
        poseStack.mulPose(Axis.XP.rotationDegrees(-pitch));
    }
}
