package destiny.null_ouroboros.client.render;

import destiny.null_ouroboros.common.dusterbike.DusterbikeRiderAnimation;
import destiny.null_ouroboros.server.entity.DusterbikeEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

public final class DusterbikeRiderPoses {
    public static final float DRIVER_BODY_LEAN = (float) Math.toRadians(30.0);
    public static final float DRIVER_ARM_FORWARD = (float) Math.toRadians(-75.0);
    public static final float DRIVER_ARM_OUTWARD = (float) Math.toRadians(5.0);
    public static final float DRIVER_ARM_KNUCKLES_UP = (float) Math.toRadians(90.0);
    public static final float DRIVER_ARM_HAND_SPREAD = (float) Math.toRadians(10.0);
    public static final float DRIVER_ARM_FORWARD_OFFSET_PIXELS = -2.0F;
    public static final float DRIVER_LEG_BEND = (float) Math.toRadians(-20.0);
    public static final float DRIVER_LEG_SPREAD = (float) Math.toRadians(-5.0);
    public static final float DRIVER_LEG_X_OFFSET_PIXELS = 3.0F;
    public static final float DRIVER_LEG_Y_OFFSET_PIXELS = 11.0F;
    public static final float DRIVER_LEG_Z_OFFSET_PIXELS = -1.0F;
    public static final float DRIVER_HEAD_FORWARD_TILT = (float) Math.toRadians(8.0);

    public static final float PASSENGER_BODY_LEAN = (float) Math.toRadians(18.0);
    public static final float PASSENGER_ARM_FORWARD = (float) Math.toRadians(-60.0);
    public static final float PASSENGER_ARM_WRAP = (float) Math.toRadians(0.0);
    public static final float PASSENGER_ARM_PALMS_IN = (float) Math.toRadians(10.0);
    public static final float PASSENGER_ARM_FORWARD_OFFSET_PIXELS = 1.0F;
    public static final float PASSENGER_LEG_BEND = (float) Math.toRadians(25.0);
    public static final float PASSENGER_LEG_SPREAD = (float) Math.toRadians(-8.0);
    public static final float PASSENGER_LEG_X_OFFSET_PIXELS = 3.0F;
    public static final float PASSENGER_LEG_Y_OFFSET_PIXELS = 11.0F;
    public static final float PASSENGER_LEG_Z_OFFSET_PIXELS = -1.0F;
    public static final float PASSENGER_HEAD_FORWARD_TILT = (float) Math.toRadians(6.0);

    private DusterbikeRiderPoses() {}

    public static boolean isDriverSeat(LivingEntity entity) {
        if (!(entity.getVehicle() instanceof DusterbikeEntity bike)) {
            return true;
        }
        return bike.getPassengers().indexOf(entity) <= 0;
    }

    public static void apply(HumanoidModel<?> model, boolean driver, float headPitchDegrees, float headYawOffsetDegrees) {
        model.riding = true;

        if (driver) {
            applyDriver(model, headPitchDegrees, headYawOffsetDegrees);
        } else {
            applyPassenger(model, headPitchDegrees, headYawOffsetDegrees);
        }
    }

    public static void applyForEntity(HumanoidModel<?> model, LivingEntity entity, float headPitchDegrees) {
        apply(model, isDriverSeat(entity), headPitchDegrees, DusterbikeRiderAnimation.getHeadOffset(entity));
    }

    private static void applyDriver(HumanoidModel<?> model, float headPitchDegrees, float headYawOffsetDegrees) {
        model.body.xRot = DRIVER_BODY_LEAN;
        model.body.yRot = 0.0F;
        model.body.zRot = 0.0F;

        model.leftArm.xRot = DRIVER_ARM_FORWARD;
        model.leftArm.yRot = DRIVER_ARM_OUTWARD - DRIVER_ARM_HAND_SPREAD;
        model.leftArm.zRot = -DRIVER_ARM_KNUCKLES_UP;
        model.leftArm.z = DRIVER_ARM_FORWARD_OFFSET_PIXELS;

        model.rightArm.xRot = DRIVER_ARM_FORWARD;
        model.rightArm.yRot = -DRIVER_ARM_OUTWARD + DRIVER_ARM_HAND_SPREAD;
        model.rightArm.zRot = DRIVER_ARM_KNUCKLES_UP;
        model.rightArm.z = DRIVER_ARM_FORWARD_OFFSET_PIXELS;

        model.leftLeg.xRot = DRIVER_LEG_BEND;
        model.leftLeg.yRot = 0.0F;
        model.leftLeg.zRot = DRIVER_LEG_SPREAD;
        model.leftLeg.x = DRIVER_LEG_X_OFFSET_PIXELS;
        model.leftLeg.y = DRIVER_LEG_Y_OFFSET_PIXELS;
        model.leftLeg.z = DRIVER_LEG_Z_OFFSET_PIXELS;

        model.rightLeg.xRot = DRIVER_LEG_BEND;
        model.rightLeg.yRot = 0.0F;
        model.rightLeg.zRot = -DRIVER_LEG_SPREAD;
        model.rightLeg.x = -DRIVER_LEG_X_OFFSET_PIXELS;
        model.rightLeg.y = DRIVER_LEG_Y_OFFSET_PIXELS;
        model.rightLeg.z = DRIVER_LEG_Z_OFFSET_PIXELS;

        model.head.xRot = headPitchDegrees * Mth.DEG_TO_RAD + DRIVER_HEAD_FORWARD_TILT;
        model.head.yRot = headYawOffsetDegrees * Mth.DEG_TO_RAD;
        copyOuterLayers(model);
    }

    private static void applyPassenger(HumanoidModel<?> model, float headPitchDegrees, float headYawOffsetDegrees) {
        model.body.xRot = PASSENGER_BODY_LEAN;
        model.body.yRot = 0.0F;
        model.body.zRot = 0.0F;

        model.leftArm.xRot = PASSENGER_ARM_FORWARD;
        model.leftArm.yRot = PASSENGER_ARM_WRAP;
        model.leftArm.zRot = PASSENGER_ARM_PALMS_IN;
        model.leftArm.z = PASSENGER_ARM_FORWARD_OFFSET_PIXELS;

        model.rightArm.xRot = PASSENGER_ARM_FORWARD;
        model.rightArm.yRot = -PASSENGER_ARM_WRAP;
        model.rightArm.zRot = -PASSENGER_ARM_PALMS_IN;
        model.rightArm.z = PASSENGER_ARM_FORWARD_OFFSET_PIXELS;

        model.leftLeg.xRot = PASSENGER_LEG_BEND;
        model.leftLeg.yRot = 0.0F;
        model.leftLeg.zRot = PASSENGER_LEG_SPREAD;
        model.leftLeg.x = PASSENGER_LEG_X_OFFSET_PIXELS;
        model.leftLeg.y = PASSENGER_LEG_Y_OFFSET_PIXELS;
        model.leftLeg.z = PASSENGER_LEG_Z_OFFSET_PIXELS;

        model.rightLeg.xRot = PASSENGER_LEG_BEND;
        model.rightLeg.yRot = 0.0F;
        model.rightLeg.zRot = -PASSENGER_LEG_SPREAD;
        model.rightLeg.x = -PASSENGER_LEG_X_OFFSET_PIXELS;
        model.rightLeg.y = PASSENGER_LEG_Y_OFFSET_PIXELS;
        model.rightLeg.z = PASSENGER_LEG_Z_OFFSET_PIXELS;

        model.head.xRot = headPitchDegrees * Mth.DEG_TO_RAD + PASSENGER_HEAD_FORWARD_TILT;
        model.head.yRot = headYawOffsetDegrees * Mth.DEG_TO_RAD;
        copyOuterLayers(model);
    }

    private static void copyOuterLayers(HumanoidModel<?> model) {
        model.hat.copyFrom(model.head);
        if (model instanceof PlayerModel<?> playerModel) {
            playerModel.jacket.copyFrom(playerModel.body);
            playerModel.leftSleeve.copyFrom(playerModel.leftArm);
            playerModel.rightSleeve.copyFrom(playerModel.rightArm);
            playerModel.leftPants.copyFrom(playerModel.leftLeg);
            playerModel.rightPants.copyFrom(playerModel.rightLeg);
        }
    }
}
