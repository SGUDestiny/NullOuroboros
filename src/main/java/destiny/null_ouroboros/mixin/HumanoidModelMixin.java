package destiny.null_ouroboros.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import destiny.null_ouroboros.client.render.DusterbikeHumanoidRenderScope;
import destiny.null_ouroboros.client.render.DusterbikeRiderPoseTracker;
import destiny.null_ouroboros.client.render.DusterbikeRiderPoses;
import destiny.null_ouroboros.client.render.player_anim.PlayerAnimAimFollow;
import destiny.null_ouroboros.client.render.player_anim.PlayerAnimApplier;
import destiny.null_ouroboros.client.render.player_anim.PlayerAnimController;
import destiny.null_ouroboros.client.render.player_anim.PlayerAnimItemLocators;
import destiny.null_ouroboros.common.player_anim.PlayerAnimInstance;
import destiny.null_ouroboros.server.entity.DusterbikeEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HumanoidModel.class)
public abstract class HumanoidModelMixin<T extends LivingEntity> {
    @Inject(method = "setupAnim", at = @At("HEAD"), cancellable = true)
    private void nullOuroboros$replaceDusterbikeRiderAnim(
            T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch,
            CallbackInfo ci) {
        if (!DusterbikeHumanoidRenderScope.isEntityRenderSetupActive()
                || !(entity.getVehicle() instanceof DusterbikeEntity)) {
            DusterbikeRiderPoseTracker.clearIfApplied((HumanoidModel<?>) (Object) this, entity);
            return;
        }

        ci.cancel();
        HumanoidModel<?> model = (HumanoidModel<?>) (Object) this;
        DusterbikeRiderPoseTracker.markApplied(entity);
        if (hasOverridePlayerAnim(entity)) {
            DusterbikeRiderPoses.applySittingBaseForEntity(model, entity, headPitch);
        } else {
            DusterbikeRiderPoses.applyForEntity(model, entity, headPitch);
            PlayerAnimAimFollow.clear(entity.getUUID());
            PlayerAnimItemLocators.clear(entity.getUUID());
        }
    }

    @Inject(method = "setupAnim", at = @At("TAIL"))
    private void nullOuroboros$applyPlayerAnimation(
            T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch,
            CallbackInfo ci) {
        if (entity.getVehicle() instanceof DusterbikeEntity) {
            if (!DusterbikeHumanoidRenderScope.isEntityRenderSetupActive()) {
                return;
            }
            HumanoidModel<?> model = (HumanoidModel<?>) (Object) this;
            if (hasOverridePlayerAnim(entity)) {
                DusterbikeRiderPoses.applySittingBaseForEntity(model, entity, headPitch);
                PlayerAnimApplier.apply(model, entity, ageInTicks, netHeadYaw, headPitch);
            } else {
                DusterbikeRiderPoses.applyForEntity(model, entity, headPitch);
                PlayerAnimAimFollow.clear(entity.getUUID());
                PlayerAnimItemLocators.clear(entity.getUUID());
            }
            return;
        }
        PlayerAnimApplier.apply((HumanoidModel<?>) (Object) this, entity, ageInTicks, netHeadYaw, headPitch);
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

    private static boolean hasOverridePlayerAnim(LivingEntity entity) {
        PlayerAnimInstance instance = PlayerAnimController.get(entity);
        return instance != null && instance.options().override();
    }
}
