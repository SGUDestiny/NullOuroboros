package destiny.null_ouroboros.client.render.player_anim;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ArmedModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public final class PlayerAnimItemRenderHelper {
    private PlayerAnimItemRenderHelper() {}

    public static void renderArmItem(
            AbstractClientPlayer player,
            ArmedModel model,
            HumanoidArm arm,
            ItemStack stack,
            ItemDisplayContext displayContext,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight) {
        if (stack.isEmpty()) {
            return;
        }

        poseStack.pushPose();
        model.translateToHand(arm, poseStack);

        boolean custom = PlayerAnimItemLocators.hasCustom(player.getUUID(), arm);
        if (custom) {
            PlayerAnimItemLocators.applyLocator(poseStack, player.getUUID(), arm);
            poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        } else {
            poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            boolean left = arm == HumanoidArm.LEFT;
            poseStack.translate((left ? -1.0F : 1.0F) / 16.0F, 0.125F, -0.625F);
        }

        ItemDisplayContext resolvedContext = displayContext;
        if (custom) {
            resolvedContext = arm == HumanoidArm.LEFT
                    ? ItemDisplayContext.THIRD_PERSON_LEFT_HAND
                    : ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
        }

        ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
        itemRenderer.renderStatic(
                player,
                stack,
                resolvedContext,
                arm == HumanoidArm.LEFT,
                poseStack,
                buffer,
                player.level(),
                packedLight,
                OverlayTexture.NO_OVERLAY,
                player.getId() + resolvedContext.ordinal()
        );
        poseStack.popPose();
    }
}
