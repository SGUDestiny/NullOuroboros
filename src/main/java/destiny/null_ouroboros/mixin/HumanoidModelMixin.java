package destiny.null_ouroboros.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import destiny.null_ouroboros.client.render.DusterbikeHumanoidRenderScope;
import destiny.null_ouroboros.client.render.DusterbikeRiderPoses;
import destiny.null_ouroboros.client.render.player_anim.PlayerAnimAimFollow;
import destiny.null_ouroboros.client.render.player_anim.PlayerAnimApplier;
import destiny.null_ouroboros.client.render.player_anim.PlayerAnimItemLocators;
import destiny.null_ouroboros.server.entity.DusterbikeEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

@Mixin(HumanoidModel.class)
public abstract class HumanoidModelMixin<T extends LivingEntity> {
    private static final Set<LivingEntity> APPLIED_RIDER_POSE_ENTITIES =
            Collections.newSetFromMap(new WeakHashMap<>());

    @Inject(method = "setupAnim", at = @At("HEAD"), cancellable = true)
    private void nullOuroboros$replaceDusterbikeRiderAnim(
            T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch,
            CallbackInfo ci) {
        if (!DusterbikeHumanoidRenderScope.isEntityRenderSetupActive()
                || !(entity.getVehicle() instanceof DusterbikeEntity)) {
            clearDusterbikeRiderPoseIfApplied((HumanoidModel<?>) (Object) this, entity);
            return;
        }

        ci.cancel();
        applyDusterbikeRiderPose((HumanoidModel<?>) (Object) this, entity, headPitch);
    }

    @Inject(method = "setupAnim", at = @At("TAIL"))
    private void nullOuroboros$applyPlayerAnimation(
            T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch,
            CallbackInfo ci) {
        if (entity.getVehicle() instanceof DusterbikeEntity) {
            if (DusterbikeHumanoidRenderScope.isEntityRenderSetupActive()) {
                applyDusterbikeRiderPose((HumanoidModel<?>) (Object) this, entity, headPitch);
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

    private static void applyDusterbikeRiderPose(HumanoidModel<?> model, LivingEntity entity, float headPitch) {
        APPLIED_RIDER_POSE_ENTITIES.add(entity);
        DusterbikeRiderPoses.applyForEntity(model, entity, headPitch);
        PlayerAnimAimFollow.clear(entity.getUUID());
        PlayerAnimItemLocators.clear(entity.getUUID());
    }

    private static void clearDusterbikeRiderPoseIfApplied(HumanoidModel<?> model, LivingEntity entity) {
        if (APPLIED_RIDER_POSE_ENTITIES.remove(entity)) {
            model.leftArm.resetPose();
            model.rightArm.resetPose();
            model.leftLeg.resetPose();
            model.rightLeg.resetPose();
        }
    }
}
