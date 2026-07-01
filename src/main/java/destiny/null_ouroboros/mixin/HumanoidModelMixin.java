package destiny.null_ouroboros.mixin;

import destiny.null_ouroboros.common.DusterbikeRiderAnimation;
import destiny.null_ouroboros.server.entity.DusterbikeEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.util.Mth;
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
    private static final float BODY_LEAN = (float) Math.toRadians(30.0);
    private static final float ARM_FORWARD = (float) Math.toRadians(-75.0);
    private static final float ARM_OUTWARD = (float) Math.toRadians(5.0);
    private static final float ARM_KNUCKLES_UP = (float) Math.toRadians(90.0);
    private static final float ARM_HAND_SPREAD = (float) Math.toRadians(10.0);
    private static final float ARM_FORWARD_OFFSET_PIXELS = -2.0F;
    private static final float LEG_BEND = (float) Math.toRadians(-20.0);
    private static final float LEG_SPREAD = (float) Math.toRadians(-5.0);
    private static final float LEG_X_OFFSET_PIXELS = 3.0F;
    private static final float LEG_Y_OFFSET_PIXELS = 11.0F;
    private static final float LEG_Z_OFFSET_PIXELS = -1.0F;
    private static final float HEAD_FORWARD_TILT = (float) Math.toRadians(8.0);

    private static final Set<LivingEntity> APPLIED_RIDER_POSE_ENTITIES =
            Collections.newSetFromMap(new WeakHashMap<>());

    @Inject(method = "setupAnim", at = @At("HEAD"), cancellable = true)
    private void nullOuroboros$replaceDusterbikeRiderAnim(
            T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch,
            CallbackInfo ci) {
        if (!(entity.getVehicle() instanceof DusterbikeEntity)) {
            clearDusterbikeRiderPoseIfApplied((HumanoidModel<?>) (Object) this, entity);
            return;
        }

        ci.cancel();
        applyDusterbikeRiderPose((HumanoidModel<?>) (Object) this, entity, headPitch);
    }

    private static void applyDusterbikeRiderPose(HumanoidModel<?> model, LivingEntity entity, float headPitch) {
        APPLIED_RIDER_POSE_ENTITIES.add(entity);

        model.riding = true;

        model.body.xRot = BODY_LEAN;
        model.body.yRot = 0.0F;
        model.body.zRot = 0.0F;

        model.leftArm.xRot = ARM_FORWARD;
        model.leftArm.yRot = ARM_OUTWARD - ARM_HAND_SPREAD;
        model.leftArm.zRot = -ARM_KNUCKLES_UP;
        model.leftArm.z = ARM_FORWARD_OFFSET_PIXELS;

        model.rightArm.xRot = ARM_FORWARD;
        model.rightArm.yRot = -ARM_OUTWARD + ARM_HAND_SPREAD;
        model.rightArm.zRot = ARM_KNUCKLES_UP;
        model.rightArm.z = ARM_FORWARD_OFFSET_PIXELS;

        model.leftLeg.xRot = LEG_BEND;
        model.leftLeg.yRot = 0.0F;
        model.leftLeg.zRot = LEG_SPREAD;
        model.leftLeg.x = LEG_X_OFFSET_PIXELS;
        model.leftLeg.y = LEG_Y_OFFSET_PIXELS;
        model.leftLeg.z = LEG_Z_OFFSET_PIXELS;

        model.rightLeg.xRot = LEG_BEND;
        model.rightLeg.yRot = 0.0F;
        model.rightLeg.zRot = -LEG_SPREAD;
        model.rightLeg.x = -LEG_X_OFFSET_PIXELS;
        model.rightLeg.y = LEG_Y_OFFSET_PIXELS;
        model.rightLeg.z = LEG_Z_OFFSET_PIXELS;

        model.head.xRot = headPitch * Mth.DEG_TO_RAD + HEAD_FORWARD_TILT;
        model.head.yRot = DusterbikeRiderAnimation.getHeadOffset(entity) * Mth.DEG_TO_RAD;

        model.hat.copyFrom(model.head);
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