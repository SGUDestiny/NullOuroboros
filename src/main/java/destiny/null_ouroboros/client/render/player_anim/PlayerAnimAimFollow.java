package destiny.null_ouroboros.client.render.player_anim;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import org.joml.Quaternionf;

import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayerAnimAimFollow {
    private static final Map<UUID, Set<ModelPart>> PLAYER_PARTS = new ConcurrentHashMap<>();
    private static final Map<ModelPart, Quaternionf> PART_ROTATIONS =
            Collections.synchronizedMap(new IdentityHashMap<>());

    private PlayerAnimAimFollow() {}

    public static void registerParentRotation(UUID playerId, HumanoidModel<?> model, float pitch, float yaw) {
        clear(playerId);
        Quaternionf rotation = new Quaternionf().rotationY(yaw).rotateX(pitch);
        Set<ModelPart> parts = new HashSet<>();
        parts.add(model.leftArm);
        parts.add(model.rightArm);
        if (model instanceof PlayerModel<?> playerModel) {
            parts.add(playerModel.leftSleeve);
            parts.add(playerModel.rightSleeve);
        }
        for (ModelPart part : parts) {
            PART_ROTATIONS.put(part, rotation);
        }
        PLAYER_PARTS.put(playerId, parts);
    }

    public static boolean applyBeforePartTransform(ModelPart part, PoseStack poseStack) {
        Quaternionf rotation = PART_ROTATIONS.get(part);
        if (rotation == null) {
            return false;
        }
        float x = part.x / 16.0F;
        float y = part.y / 16.0F;
        float z = part.z / 16.0F;
        poseStack.translate(x, y, z);
        poseStack.mulPose(rotation);
        poseStack.translate(-x, -y, -z);
        return true;
    }

    public static boolean hasParentRotation(ModelPart part) {
        return PART_ROTATIONS.containsKey(part);
    }

    public static void clear(UUID playerId) {
        Set<ModelPart> parts = PLAYER_PARTS.remove(playerId);
        if (parts == null) {
            return;
        }
        for (ModelPart part : parts) {
            PART_ROTATIONS.remove(part);
        }
    }
}
