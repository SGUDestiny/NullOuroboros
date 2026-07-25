package destiny.null_ouroboros.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import destiny.null_ouroboros.client.render.player_anim.PlayerAnimAimFollow;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(PlayerModel.class)
public abstract class PlayerModelMixin {
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
