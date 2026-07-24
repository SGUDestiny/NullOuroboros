package destiny.null_ouroboros.client.render.player_anim;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.HumanoidArm;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayerAnimItemLocators {
    private static final Map<UUID, State> STATES = new ConcurrentHashMap<>();

    private PlayerAnimItemLocators() {}

    public static void update(UUID playerId, ModelPart leftItem, ModelPart rightItem, boolean leftActive, boolean rightActive) {
        if (!leftActive && !rightActive) {
            STATES.remove(playerId);
            return;
        }
        State state = STATES.computeIfAbsent(playerId, id -> new State());
        state.leftActive = leftActive;
        state.rightActive = rightActive;
        if (leftActive) {
            copyPose(leftItem, state.leftItem);
        }
        if (rightActive) {
            copyPose(rightItem, state.rightItem);
        }
    }

    public static void clear(UUID playerId) {
        STATES.remove(playerId);
    }

    public static State get(UUID playerId) {
        return STATES.get(playerId);
    }

    public static boolean hasCustom(UUID playerId, HumanoidArm arm) {
        State state = STATES.get(playerId);
        if (state == null) {
            return false;
        }
        return arm == HumanoidArm.LEFT ? state.leftActive : state.rightActive;
    }

    public static void applyLocator(PoseStack poseStack, UUID playerId, HumanoidArm arm) {
        State state = STATES.get(playerId);
        if (state == null) {
            return;
        }
        ModelPart locator = arm == HumanoidArm.LEFT ? state.leftItem : state.rightItem;
        boolean active = arm == HumanoidArm.LEFT ? state.leftActive : state.rightActive;
        if (active) {
            locator.translateAndRotate(poseStack);
        }
    }

    private static void copyPose(ModelPart from, ModelPart to) {
        to.x = from.x;
        to.y = from.y;
        to.z = from.z;
        to.xRot = from.xRot;
        to.yRot = from.yRot;
        to.zRot = from.zRot;
        to.xScale = from.xScale;
        to.yScale = from.yScale;
        to.zScale = from.zScale;
    }

    public static final class State {
        public final ModelPart leftItem = emptyPart();
        public final ModelPart rightItem = emptyPart();
        public boolean leftActive;
        public boolean rightActive;

        private static ModelPart emptyPart() {
            return new ModelPart(Collections.emptyList(), Map.of());
        }
    }
}
