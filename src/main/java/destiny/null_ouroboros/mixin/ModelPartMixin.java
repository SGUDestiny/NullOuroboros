package destiny.null_ouroboros.mixin;

import destiny.null_ouroboros.client.render.player_anim.PlayerAnimAimFollow;
import net.minecraft.client.model.geom.ModelPart;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(ModelPart.class)
public class ModelPartMixin {
    @ModifyArg(
            method = "translateAndRotate",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/vertex/PoseStack;mulPose(Lorg/joml/Quaternionf;)V"
            ),
            index = 0
    )
    private Quaternionf nullOuroboros$applySharedArmYaw(Quaternionf localRotation) {
        return PlayerAnimAimFollow.applyParentRotation((ModelPart) (Object) this, localRotation);
    }
}
