package destiny.null_ouroboros.client.render.player_anim;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import org.joml.Quaternionf;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayerAnimAimFollow {
    private static final Map<UUID, Set<ModelPart>> PLAYER_PARTS = new ConcurrentHashMap<>();
    private static final Map<ModelPart, Quaternionf> PART_YAWS =
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
            PART_YAWS.put(part, new Quaternionf(rotation));
        }
        PLAYER_PARTS.put(playerId, parts);
    }

    public static Quaternionf applyParentRotation(ModelPart part, Quaternionf localRotation) {
        Quaternionf parentRotation = PART_YAWS.get(part);
        if (parentRotation != null) {
            localRotation.premul(parentRotation);
        }
        return localRotation;
    }

    public static void clear(UUID playerId) {
        Set<ModelPart> parts = PLAYER_PARTS.remove(playerId);
        if (parts == null) {
            return;
        }
        for (ModelPart part : parts) {
            PART_YAWS.remove(part);
        }
    }
}
