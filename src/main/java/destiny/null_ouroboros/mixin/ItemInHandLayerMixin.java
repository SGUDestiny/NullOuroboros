package destiny.null_ouroboros.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import destiny.null_ouroboros.client.render.player_anim.PlayerAnimItemLocators;
import net.minecraft.client.model.ArmedModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandLayer.class)
public abstract class ItemInHandLayerMixin<T extends LivingEntity, M extends EntityModel<T> & ArmedModel> {
    @Shadow
    @Final
    private ItemInHandRenderer itemInHandRenderer;

    @Inject(method = "renderArmWithItem", at = @At("HEAD"), cancellable = true)
    private void nullOuroboros$applyItemLocator(
            T livingEntity,
            ItemStack itemStack,
            ItemDisplayContext displayContext,
            HumanoidArm arm,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            CallbackInfo ci) {
        if (itemStack.isEmpty() || !PlayerAnimItemLocators.hasCustom(livingEntity.getUUID(), arm)) {
            return;
        }

        @SuppressWarnings("unchecked")
        ItemInHandLayer<T, M> layer = (ItemInHandLayer<T, M>) (Object) this;
        ArmedModel model = layer.getParentModel();

        poseStack.pushPose();
        model.translateToHand(arm, poseStack);
        PlayerAnimItemLocators.applyLocator(poseStack, livingEntity.getUUID(), arm);
        poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        boolean leftHand = arm == HumanoidArm.LEFT;
        this.itemInHandRenderer.renderItem(
                livingEntity,
                itemStack,
                displayContext,
                leftHand,
                poseStack,
                buffer,
                packedLight
        );
        poseStack.popPose();
        ci.cancel();
    }
}
