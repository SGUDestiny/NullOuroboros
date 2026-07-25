package destiny.null_ouroboros.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import destiny.null_ouroboros.client.render.player_anim.PlayerAnimAimFollow;
import net.minecraft.client.model.AgeableListModel;
import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Consumer;

@Mixin(AgeableListModel.class)
public abstract class AgeableListModelMixin {
    @Unique
    private static final ThreadLocal<Deque<PoseStack>> NULL_OUROBOROS$POSE_STACKS =
            ThreadLocal.withInitial(ArrayDeque::new);

    @Inject(method = "renderToBuffer", at = @At("HEAD"))
    private void nullOuroboros$capturePoseStack(
            PoseStack poseStack,
            VertexConsumer buffer,
            int packedLight,
            int packedOverlay,
            float red,
            float green,
            float blue,
            float alpha,
            CallbackInfo ci) {
        NULL_OUROBOROS$POSE_STACKS.get().push(poseStack);
    }

    @Inject(method = "renderToBuffer", at = @At("RETURN"))
    private void nullOuroboros$releasePoseStack(
            PoseStack poseStack,
            VertexConsumer buffer,
            int packedLight,
            int packedOverlay,
            float red,
            float green,
            float blue,
            float alpha,
            CallbackInfo ci) {
        Deque<PoseStack> poseStacks = NULL_OUROBOROS$POSE_STACKS.get();
        poseStacks.pop();
        if (poseStacks.isEmpty()) {
            NULL_OUROBOROS$POSE_STACKS.remove();
        }
    }

    @ModifyArg(
            method = "renderToBuffer",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/lang/Iterable;forEach(Ljava/util/function/Consumer;)V",
                    remap = false
            ),
            index = 0
    )
    private Consumer<ModelPart> nullOuroboros$applyParentRotation(Consumer<ModelPart> renderer) {
        PoseStack poseStack = NULL_OUROBOROS$POSE_STACKS.get().peek();
        if (poseStack == null) {
            return renderer;
        }
        return part -> {
            if (!PlayerAnimAimFollow.hasParentRotation(part)) {
                renderer.accept(part);
                return;
            }
            poseStack.pushPose();
            PlayerAnimAimFollow.applyBeforePartTransform(part, poseStack);
            renderer.accept(part);
            poseStack.popPose();
        };
    }
}
