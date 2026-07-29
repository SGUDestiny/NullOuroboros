package destiny.null_ouroboros.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import destiny.null_ouroboros.client.render.DusterbikeHumanoidRenderScope;
import destiny.null_ouroboros.client.render.DusterbikeRiderPoses;
import destiny.null_ouroboros.client.render.player_anim.PlayerAnimAimFollow;
import destiny.null_ouroboros.client.render.player_anim.PlayerAnimItemLocators;
import destiny.null_ouroboros.server.entity.DusterbikeEntity;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerModel.class)
public abstract class PlayerModelMixin {
    @Inject(method = "setupAnim", at = @At("RETURN"))
    private void nullOuroboros$forceDusterbikeRiderPose(
            LivingEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch,
            CallbackInfo ci) {
        if (!DusterbikeHumanoidRenderScope.isEntityRenderSetupActive()
                || !(entity.getVehicle() instanceof DusterbikeEntity)) {
            return;
        }

        PlayerModel<?> model = (PlayerModel<?>) (Object) this;
        DusterbikeRiderPoses.applyForEntity(model, entity, headPitch);
        PlayerAnimAimFollow.clear(entity.getUUID());
        PlayerAnimItemLocators.clear(entity.getUUID());
    }

    @WrapOperation(
            method = "translateToHand",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/model/geom/ModelPart;translateAndRotate(Lcom/mojang/blaze3d/vertex/PoseStack;)V"
            )
    )
    private void nullOuroboros$applyHeldItemParentRotation(
            ModelPart part, PoseStack poseStack, Operation<Void> original) {
        PlayerAnimAimFollow.applyBeforePartTransform(part, poseStack);
        original.call(part, poseStack);
    }
}
