package destiny.null_ouroboros.mixin;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.EquipmentSlot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HumanoidArmorLayer.class)
public abstract class HumanoidArmorLayerMixin {
    @Inject(method = "setPartVisibility", at = @At("RETURN"))
    private void nullOuroboros$respectParentVisibility(HumanoidModel<?> armorModel, EquipmentSlot slot, CallbackInfo ci) {
        Object parentModel = ((RenderLayer<?, ?>) (Object) this).getParentModel();
        if (!(parentModel instanceof HumanoidModel<?> parent)) {
            return;
        }

        armorModel.head.visible &= parent.head.visible;
        armorModel.hat.visible &= parent.hat.visible;
        armorModel.body.visible &= parent.body.visible;
        armorModel.leftArm.visible &= parent.leftArm.visible;
        armorModel.rightArm.visible &= parent.rightArm.visible;
        armorModel.leftLeg.visible &= parent.leftLeg.visible;
        armorModel.rightLeg.visible &= parent.rightLeg.visible;
    }
}
