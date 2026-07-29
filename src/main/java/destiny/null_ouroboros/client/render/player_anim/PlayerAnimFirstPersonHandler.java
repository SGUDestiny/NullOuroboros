package destiny.null_ouroboros.client.render.player_anim;

import destiny.null_ouroboros.common.player_anim.PlayerAnimInstance;
import destiny.null_ouroboros.common.player_anim.HeavyRevolverPlayerAnims;
import destiny.null_ouroboros.mixin.EntityRenderDispatcherAccessor;
import destiny.null_ouroboros.server.item.HeavyRevolverItem;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public final class PlayerAnimFirstPersonHandler {
    private PlayerAnimFirstPersonHandler() {}

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) {
            return;
        }

        PlayerAnimInstance instance = syncLocalRevolverSelection(player);
        if (instance == null || !instance.options().renderFirstPerson()) {
            return;
        }

        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || !minecraft.options.getCameraType().isFirstPerson()) {
            return;
        }

        PlayerAnimInstance instance = syncLocalRevolverSelection(player);
        if (instance == null || !instance.options().renderFirstPerson()) {
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

        PlayerAnimFirstPersonRenderScope.begin(instance.options());
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

    private static PlayerAnimInstance syncLocalRevolverSelection(LocalPlayer player) {
        PlayerAnimInstance current = PlayerAnimController.get(player);
        boolean revolverSelected = player.getMainHandItem().getItem() instanceof HeavyRevolverItem;
        boolean revolverAnimation = current != null && HeavyRevolverPlayerAnims.isRevolverAnim(current.animationId());

        if (!revolverSelected) {
            if (revolverAnimation) {
                PlayerAnimController.cancel(player.getUUID(), current.animationId());
                return null;
            }
            return current;
        }

        if (current == null) {
            PlayerAnimController.play(
                    player.getUUID(),
                    HeavyRevolverPlayerAnims.HOLD_ID,
                    player.level().getGameTime(),
                    HeavyRevolverPlayerAnims.holdOptions()
            );
            return PlayerAnimController.get(player);
        }

        return current;
    }
}
