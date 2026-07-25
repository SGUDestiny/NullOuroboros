package destiny.null_ouroboros.client.render.player_anim;

import destiny.null_ouroboros.common.player_anim.AimFollowMode;
import destiny.null_ouroboros.common.player_anim.PlayerAnimInstance;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.joml.Vector3f;

import java.util.HashSet;
import java.util.Set;

public final class PlayerAnimApplier {
    private static final Vector3f ANIMATION_VEC_CACHE = new Vector3f();

    private PlayerAnimApplier() {}

    public static void apply(
            HumanoidModel<?> model, LivingEntity entity, float ageInTicks, float netHeadYaw, float headPitch) {
        PlayerAnimInstance instance = PlayerAnimController.get(entity);
        if (instance == null) {
            PlayerAnimItemLocators.clear(entity.getUUID());
            PlayerAnimAimFollow.clear(entity.getUUID());
            return;
        }

        AnimationDefinition definition = PlayerAnimationRegistry.get(instance.animationId());
        if (definition == null) {
            PlayerAnimItemLocators.clear(entity.getUUID());
            PlayerAnimAimFollow.clear(entity.getUUID());
            return;
        }

        boolean mirror = instance.options().mirrorForLeftHanded()
                && entity instanceof Player player
                && player.getMainArm() == HumanoidArm.LEFT;

        Set<String> keyed = definition.boneAnimations().keySet();
        Set<String> resetTargets = mirrorTargets(keyed, mirror);
        PlayerModelAnimatable animatable = new PlayerModelAnimatable(model);

        if (instance.options().override()) {
            resetKeyedLimbs(model, resetTargets);
        }

        boolean leftItemKeyed = resetTargets.contains("left_item");
        boolean rightItemKeyed = resetTargets.contains("right_item");
        if (leftItemKeyed) {
            PlayerModelAnimatable.resetItemRest(animatable.leftItem(), true);
        }
        if (rightItemKeyed) {
            PlayerModelAnimatable.resetItemRest(animatable.rightItem(), false);
        }

        float partialTick = ageInTicks - entity.tickCount;
        long animationMs = PlayerAnimController.animationTimeMs(entity, instance, definition, partialTick);
        PlayerAnimBaker.bake(animatable, definition, animationMs, mirror, ANIMATION_VEC_CACHE);

        if (entity.isCrouching()) {
            if (resetTargets.contains("left_arm")) {
                model.leftArm.y += 4.0F;
            }
            if (resetTargets.contains("right_arm")) {
                model.rightArm.y += 4.0F;
            }
        }

        float pitch = headPitch * Mth.DEG_TO_RAD;
        float yaw = netHeadYaw * Mth.DEG_TO_RAD;
        if (instance.options().aimFollowArms()
                && instance.options().aimFollowMode() != AimFollowMode.PARENT_ROTATION) {
            if (instance.options().aimFollowArmsX()) {
                model.leftArm.xRot += pitch;
                model.rightArm.xRot += pitch;
            }
            model.leftArm.yRot += yaw;
            model.rightArm.yRot += yaw;
        }

        syncOverlays(model, resetTargets);
        if (instance.options().aimFollowArms()
                && instance.options().aimFollowMode() == AimFollowMode.PARENT_ROTATION) {
            float parentPitch = instance.options().aimFollowArmsX() ? pitch * 0.7F : 0.0F;
            PlayerAnimAimFollow.registerParentRotation(entity.getUUID(), model, parentPitch, yaw);
        } else {
            PlayerAnimAimFollow.clear(entity.getUUID());
        }
        PlayerAnimItemLocators.update(
                entity.getUUID(),
                animatable.leftItem(),
                animatable.rightItem(),
                leftItemKeyed,
                rightItemKeyed
        );
    }

    private static Set<String> mirrorTargets(Set<String> keyed, boolean mirror) {
        if (!mirror) {
            return keyed;
        }
        Set<String> targets = new HashSet<>();
        for (String name : keyed) {
            targets.add(PlayerAnimMirror.swapName(name));
        }
        return targets;
    }

    private static void resetKeyedLimbs(HumanoidModel<?> model, Set<String> keyed) {
        resetIfKeyed(keyed, "head", model.head);
        resetIfKeyed(keyed, "hat", model.hat);
        resetIfKeyed(keyed, "body", model.body);
        resetIfKeyed(keyed, "left_arm", model.leftArm);
        resetIfKeyed(keyed, "right_arm", model.rightArm);
        resetIfKeyed(keyed, "left_leg", model.leftLeg);
        resetIfKeyed(keyed, "right_leg", model.rightLeg);
        if (model instanceof PlayerModel<?> playerModel) {
            resetIfKeyed(keyed, "jacket", playerModel.jacket);
            resetIfKeyed(keyed, "left_sleeve", playerModel.leftSleeve);
            resetIfKeyed(keyed, "right_sleeve", playerModel.rightSleeve);
            resetIfKeyed(keyed, "left_pants", playerModel.leftPants);
            resetIfKeyed(keyed, "right_pants", playerModel.rightPants);
        }
    }

    private static void resetIfKeyed(Set<String> keyed, String name, ModelPart part) {
        if (keyed.contains(name)) {
            part.resetPose();
        }
    }

    private static void syncOverlays(HumanoidModel<?> model, Set<String> keyed) {
        if (keyed.contains("head") || keyed.contains("hat")) {
            model.hat.copyFrom(model.head);
        }
        if (!(model instanceof PlayerModel<?> playerModel)) {
            return;
        }
        if (keyed.contains("body") || keyed.contains("jacket")) {
            playerModel.jacket.copyFrom(playerModel.body);
        }
        if (keyed.contains("left_arm") || keyed.contains("left_sleeve")) {
            playerModel.leftSleeve.copyFrom(playerModel.leftArm);
        }
        if (keyed.contains("right_arm") || keyed.contains("right_sleeve")) {
            playerModel.rightSleeve.copyFrom(playerModel.rightArm);
        }
        if (keyed.contains("left_leg") || keyed.contains("left_pants")) {
            playerModel.leftPants.copyFrom(playerModel.leftLeg);
        }
        if (keyed.contains("right_leg") || keyed.contains("right_pants")) {
            playerModel.rightPants.copyFrom(playerModel.rightLeg);
        }
    }
}
