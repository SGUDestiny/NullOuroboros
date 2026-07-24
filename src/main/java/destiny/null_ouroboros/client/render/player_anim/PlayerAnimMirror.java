package destiny.null_ouroboros.client.render.player_anim;

import net.minecraft.client.animation.AnimationChannel;
import net.minecraft.client.model.geom.ModelPart;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class PlayerAnimMirror {
    private static final Map<String, String> PAIR_SWAP = new HashMap<>();

    static {
        putPair("left_arm", "right_arm");
        putPair("left_sleeve", "right_sleeve");
        putPair("left_leg", "right_leg");
        putPair("left_pants", "right_pants");
        putPair("left_item", "right_item");
    }

    private PlayerAnimMirror() {}

    private static void putPair(String left, String right) {
        PAIR_SWAP.put(left, right);
        PAIR_SWAP.put(right, left);
    }

    public static String swapName(String boneName) {
        return PAIR_SWAP.getOrDefault(boneName, boneName);
    }

    public static void mirrorVector(AnimationChannel.Target target, Vector3f vector) {
        if (target == AnimationChannel.Targets.POSITION) {
            vector.x = -vector.x;
        } else if (target == AnimationChannel.Targets.ROTATION) {
            vector.y = -vector.y;
            vector.z = -vector.z;
        }
    }

    public static Optional<ModelPart> resolvePart(PlayerModelAnimatable animatable, String boneName, boolean mirror) {
        String name = mirror ? swapName(boneName) : boneName;
        return animatable.getAnyDescendantWithName(name);
    }
}
