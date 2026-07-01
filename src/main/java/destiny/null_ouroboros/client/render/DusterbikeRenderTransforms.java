package destiny.null_ouroboros.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import destiny.null_ouroboros.common.DusterbikeTransforms;
import destiny.null_ouroboros.server.entity.DusterbikeEntity;
import net.minecraft.world.phys.Vec3;

public final class DusterbikeRenderTransforms {
    private DusterbikeRenderTransforms() {}

    public static void applyEntityRenderPose(PoseStack poseStack, DusterbikeEntity entity, float partialTicks) {
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - entity.getRenderYaw(partialTicks)));

        float roll = entity.getRenderRoll(partialTicks);
        Vec3 pivot = DusterbikeTransforms.PITCH_PIVOT_LOCAL;
        poseStack.translate(pivot.x, pivot.y, pivot.z);
        poseStack.mulPose(Axis.ZP.rotationDegrees(roll));
        poseStack.translate(-pivot.x, -pivot.y, -pivot.z);

        float pitch = entity.getRenderPitch(partialTicks);
        poseStack.translate(pivot.x, pivot.y, pivot.z);
        poseStack.mulPose(Axis.XP.rotationDegrees(-pitch));
        poseStack.translate(-pivot.x, -pivot.y, -pivot.z);

        poseStack.scale(-1.0F, -1.0F, 1.0F);
        poseStack.translate(DusterbikeTransforms.MODEL_X_OFFSET, DusterbikeTransforms.MODEL_Y_OFFSET, 0.0F);
    }

    public static void applyModelRootPose(PoseStack poseStack) {
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        poseStack.translate(DusterbikeTransforms.MODEL_X_OFFSET, DusterbikeTransforms.MODEL_Y_OFFSET, 0.0F);
    }
}
