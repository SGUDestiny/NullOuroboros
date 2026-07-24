package destiny.null_ouroboros.client.render.player_anim;

import net.minecraft.client.animation.AnimationChannel;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.animation.Keyframe;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class PlayerAnimBaker {
    private PlayerAnimBaker() {}

    public static void bake(
            PlayerModelAnimatable animatable,
            AnimationDefinition definition,
            long animationMs,
            boolean mirror,
            Vector3f vecCache) {
        float elapsedSeconds = getElapsedSeconds(definition, animationMs);
        for (Map.Entry<String, List<AnimationChannel>> entry : definition.boneAnimations().entrySet()) {
            Optional<ModelPart> optional = PlayerAnimMirror.resolvePart(animatable, entry.getKey(), mirror);
            if (optional.isEmpty()) {
                continue;
            }
            ModelPart part = optional.get();
            for (AnimationChannel channel : entry.getValue()) {
                Keyframe[] keyframes = channel.keyframes();
                int i = Math.max(0, Mth.binarySearch(0, keyframes.length, index -> elapsedSeconds <= keyframes[index].timestamp()) - 1);
                int j = Math.min(keyframes.length - 1, i + 1);
                Keyframe keyframe = keyframes[i];
                Keyframe keyframe2 = keyframes[j];
                float delta = elapsedSeconds - keyframe.timestamp();
                float alpha = j != i
                        ? Mth.clamp(delta / (keyframe2.timestamp() - keyframe.timestamp()), 0.0F, 1.0F)
                        : 0.0F;
                keyframe2.interpolation().apply(vecCache, alpha, keyframes, i, j, 1.0F);
                if (mirror) {
                    PlayerAnimMirror.mirrorVector(channel.target(), vecCache);
                }
                channel.target().apply(part, vecCache);
            }
        }
    }

    private static float getElapsedSeconds(AnimationDefinition definition, long animationMs) {
        float seconds = (float) animationMs / 1000.0F;
        return definition.looping() ? seconds % definition.lengthInSeconds() : Math.min(seconds, definition.lengthInSeconds());
    }
}
