package destiny.null_ouroboros.server.event;

import destiny.null_ouroboros.server.entity.DusterbikeEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
public class DusterbikeServerEvents {
    private static final double KEY_HOLD_SEARCH_RADIUS = 64.0D;

    private DusterbikeServerEvents() {}

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) {
            return;
        }

        double radius = KEY_HOLD_SEARCH_RADIUS;
        for (DusterbikeEntity bike : player.level().getEntitiesOfClass(
                DusterbikeEntity.class, player.getBoundingBox().inflate(radius, radius, radius))) {
            bike.releaseKeyHoldForPlayer(player);
        }
    }
}
