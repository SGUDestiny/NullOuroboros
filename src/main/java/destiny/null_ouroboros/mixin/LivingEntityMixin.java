package destiny.null_ouroboros.mixin;

import destiny.null_ouroboros.common.dusterbike.DusterbikeRiderAnimation;
import destiny.null_ouroboros.server.entity.DusterbikeEntity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {
    @Inject(method = "tickHeadTurn", at = @At("HEAD"), cancellable = true)
    private void nullOuroboros$skipDusterbikeHeadTurn(
            float bodyYaw, float headYaw, CallbackInfoReturnable<Float> ci) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (entity.getVehicle() instanceof DusterbikeEntity) {
            ci.setReturnValue(headYaw);
        }
    }

    @Inject(method = "getViewYRot", at = @At("HEAD"), cancellable = true)
    private void nullOuroboros$dusterbikeViewYaw(float partialTick, CallbackInfoReturnable<Float> ci) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (entity.getVehicle() instanceof DusterbikeEntity bike) {
            ci.setReturnValue(DusterbikeRiderAnimation.viewYaw(entity, bike, partialTick));
        }
    }
}
