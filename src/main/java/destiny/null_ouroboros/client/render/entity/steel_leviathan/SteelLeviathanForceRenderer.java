package destiny.null_ouroboros.client.render.entity.steel_leviathan;

import com.mojang.blaze3d.vertex.PoseStack;
import destiny.null_ouroboros.server.entity.steel_leviathan.SteelLeviathanPartEntity;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.client.event.RenderLevelStageEvent;

import java.util.HashSet;
import java.util.Set;

public final class SteelLeviathanForceRenderer {
    private static final Set<Integer> RENDERED_THIS_FRAME = new HashSet<>();

    private SteelLeviathanForceRenderer() {
    }

    public static void markRendered(SteelLeviathanPartEntity entity) {
        RENDERED_THIS_FRAME.add(entity.getId());
    }

    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) {
            return;
        }

        Camera camera = event.getCamera();
        double camX = camera.getPosition().x;
        double camY = camera.getPosition().y;
        double camZ = camera.getPosition().z;
        float partialTick = event.getPartialTick();
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource buffer = mc.renderBuffers().bufferSource();
        EntityRenderDispatcher dispatcher = mc.getEntityRenderDispatcher();

        for (Entity entity : level.entitiesForRendering()) {
            if (!(entity instanceof SteelLeviathanPartEntity part)) {
                continue;
            }
            if (RENDERED_THIS_FRAME.contains(part.getId())) {
                continue;
            }
            double x = Mth.lerp(partialTick, part.xo, part.getX()) - camX;
            double y = Mth.lerp(partialTick, part.yo, part.getY()) - camY;
            double z = Mth.lerp(partialTick, part.zo, part.getZ()) - camZ;
            float yaw = Mth.rotLerp(partialTick, part.getBodyYawO(), part.getBodyYaw());
            dispatcher.render(part, x, y, z, yaw, partialTick, poseStack, buffer,
                    dispatcher.getPackedLightCoords(part, partialTick));
        }

        buffer.endBatch();
        RENDERED_THIS_FRAME.clear();
    }
}

