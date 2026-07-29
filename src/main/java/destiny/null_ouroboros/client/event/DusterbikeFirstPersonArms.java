package destiny.null_ouroboros.client.event;

import destiny.null_ouroboros.client.render.player_anim.PlayerAnimController;
import destiny.null_ouroboros.client.render.player_anim.PlayerAnimFirstPersonRenderScope;
import destiny.null_ouroboros.common.player_anim.PlayerAnimInstance;
import destiny.null_ouroboros.common.player_anim.PlayOptions;
import destiny.null_ouroboros.mixin.EntityRenderDispatcherAccessor;
import destiny.null_ouroboros.server.entity.DusterbikeEntity;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public final class DusterbikeFirstPersonArms {
    private static final PlayOptions ARMS_OPTIONS = PlayOptions.builder()
            .renderFirstPerson(true)
            .renderFirstPersonBody(false)
            .renderFirstPersonHead(false)
            .build();

    private DusterbikeFirstPersonArms() {}

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onRenderHand(RenderHandEvent event) {
        if (!shouldRenderBikeArms()) {
            return;
        }
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            return;
        }
        if (!shouldRenderBikeArms()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || !minecraft.options.getCameraType().isFirstPerson()) {
            return;
        }

        Camera camera = event.getCamera();
        float partialTick = event.getPartialTick();
        double x = Mth.lerp(partialTick, player.xo, player.getX()) - camera.getPosition().x;
        double y = Mth.lerp(partialTick, player.yo, player.getY()) - camera.getPosition().y;
        double z = Mth.lerp(partialTick, player.zo, player.getZ()) - camera.getPosition().z;
        float bodyYaw = Mth.rotLerp(partialTick, player.yBodyRotO, player.yBodyRot);

        EntityRenderDispatcher dispatcher = minecraft.getEntityRenderDispatcher();
        boolean renderShadow = ((EntityRenderDispatcherAccessor) dispatcher).nullOuroboros$shouldRenderShadow();
        MultiBufferSource.BufferSource buffer = minecraft.renderBuffers().bufferSource();

        PlayerAnimFirstPersonRenderScope.begin(ARMS_OPTIONS);
        dispatcher.setRenderShadow(false);
        try {
            dispatcher.render(
                    player,
                    x,
                    y,
                    z,
                    bodyYaw,
                    partialTick,
                    event.getPoseStack(),
                    buffer,
                    dispatcher.getPackedLightCoords(player, partialTick)
            );
        } finally {
            dispatcher.setRenderShadow(renderShadow);
            PlayerAnimFirstPersonRenderScope.end();
        }
        buffer.endBatch();
    }

    private static boolean shouldRenderBikeArms() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || !(player.getVehicle() instanceof DusterbikeEntity)) {
            return false;
        }
        PlayerAnimInstance instance = PlayerAnimController.get(player);
        return instance == null || !instance.options().renderFirstPerson();
    }
}
