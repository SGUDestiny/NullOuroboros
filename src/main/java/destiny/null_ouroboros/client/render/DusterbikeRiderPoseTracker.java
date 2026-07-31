package destiny.null_ouroboros.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

public final class DusterbikeRiderPoseTracker {
    private static final Set<LivingEntity> APPLIED_RIDER_POSE_ENTITIES =
            Collections.newSetFromMap(new WeakHashMap<>());

    private DusterbikeRiderPoseTracker() {}

    public static void markApplied(LivingEntity entity) {
        APPLIED_RIDER_POSE_ENTITIES.add(entity);
    }

    public static boolean clearIfApplied(HumanoidModel<?> model, LivingEntity entity) {
        if (!APPLIED_RIDER_POSE_ENTITIES.remove(entity)) {
            return false;
        }
        DusterbikeRiderPoses.forceReset(model);
        return true;
    }

    public static void forceClearAll() {
        APPLIED_RIDER_POSE_ENTITIES.clear();
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        EntityRenderer<?> renderer = minecraft.getEntityRenderDispatcher().getRenderer(minecraft.player);
        if (renderer instanceof LivingEntityRenderer<?, ?> livingRenderer
                && livingRenderer.getModel() instanceof HumanoidModel<?> model) {
            DusterbikeRiderPoses.forceReset(model);
        }
    }
}
