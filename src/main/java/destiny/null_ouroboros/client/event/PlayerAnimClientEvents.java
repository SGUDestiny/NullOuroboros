package destiny.null_ouroboros.client.event;

import destiny.null_ouroboros.client.render.player_anim.PlayerAnimController;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public final class PlayerAnimClientEvents {
    private PlayerAnimClientEvents() {}

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        var player = Minecraft.getInstance().player;
        if (player != null) {
            PlayerAnimController.clear(player.getUUID());
        }
    }
}
