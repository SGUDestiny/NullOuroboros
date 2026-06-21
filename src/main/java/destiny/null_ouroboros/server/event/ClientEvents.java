package destiny.null_ouroboros.server.event;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import destiny.null_ouroboros.client.render.dimension.VergeOfRealityDimensionEffects;
import destiny.null_ouroboros.client.sound.ManifoldingSoundManager;
import destiny.null_ouroboros.client.sound.SirenSoundManager;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.opengl.GL11;

import static org.lwjgl.opengl.GL32C.GL_DEPTH_CLAMP;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ClientEvents {
    private static final BufferBuilder BUFFER = new BufferBuilder(65536);

    @SubscribeEvent
    public static void levelRender(RenderLevelStageEvent event) {
        boolean renderSkyPass = event.getStage().equals(RenderLevelStageEvent.Stage.AFTER_SKY);
        if (renderSkyPass) {
            ClientLevel level = Minecraft.getInstance().level;
            if (level == null)
                return;

            float partialTick = event.getPartialTick();

            Camera camera = event.getCamera();

            PoseStack pose = event.getPoseStack();
            MultiBufferSource.BufferSource buffer = MultiBufferSource.immediate(BUFFER);

            GL11.glEnable(GL_DEPTH_CLAMP);

            if (VergeOfRealityDimensionEffects.isVergeOfReality(level)) {
                VergeOfRealityDimensionEffects vergeOfRealityDimensionEffects = VergeOfRealityDimensionEffects.getInstance();

                if (vergeOfRealityDimensionEffects != null) {
                    pose.pushPose();
                    vergeOfRealityDimensionEffects.renderOverlay(level, partialTick, pose, camera, event.getProjectionMatrix());
                    pose.popPose();
                }
            }

            buffer.endBatch();

            GL11.glDisable(GL_DEPTH_CLAMP);
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        ManifoldingSoundManager.tick(event);
    }

    @SubscribeEvent
    public static void onClientDisconnect(ClientPlayerNetworkEvent.LoggingOut event) {
        SirenSoundManager.stopAll();
    }
}
