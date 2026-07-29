package destiny.null_ouroboros.client.render.player_anim;

import destiny.null_ouroboros.common.player_anim.AimFollowMode;
import destiny.null_ouroboros.common.player_anim.PlayerAnimInstance;
import destiny.null_ouroboros.server.entity.DusterbikeEntity;
import net.minecraft.client.animation.AnimationChannel;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.joml.Vector3f;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

        if (entity.getVehicle() instanceof DusterbikeEntity) {
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
        Set<String> positionTargets = Set.of();

        if (instance.options().override()) {
            positionTargets = resetKeyedChannels(animatable, definition, mirror);
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
            if (positionTargets.contains("left_arm")) {
                model.leftArm.y += 4.0F;
            }
            if (positionTargets.contains("right_arm")) {
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

    private static Set<String> resetKeyedChannels(
            PlayerModelAnimatable animatable, AnimationDefinition definition, boolean mirror) {
        Set<String> positionTargets = new HashSet<>();
        for (Map.Entry<String, List<AnimationChannel>> entry : definition.boneAnimations().entrySet()) {
            Optional<ModelPart> optional = PlayerAnimMirror.resolvePart(animatable, entry.getKey(), mirror);
            if (optional.isEmpty()) {
                continue;
            }
            ModelPart part = optional.get();
            String resolvedName = mirror ? PlayerAnimMirror.swapName(entry.getKey()) : entry.getKey();
            PartPose initial = part.getInitialPose();
            for (AnimationChannel channel : entry.getValue()) {
                AnimationChannel.Target target = channel.target();
                if (target == AnimationChannel.Targets.POSITION) {
                    part.x = initial.x;
                    part.y = initial.y;
                    part.z = initial.z;
                    positionTargets.add(resolvedName);
                } else if (target == AnimationChannel.Targets.ROTATION) {
                    part.xRot = initial.xRot;
                    part.yRot = initial.yRot;
                    part.zRot = initial.zRot;
                } else if (target == AnimationChannel.Targets.SCALE) {
                    part.xScale = 1.0F;
                    part.yScale = 1.0F;
                    part.zScale = 1.0F;
                }
            }
        }
        return positionTargets;
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
