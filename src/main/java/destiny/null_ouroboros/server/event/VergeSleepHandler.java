package destiny.null_ouroboros.server.event;

import destiny.null_ouroboros.server.capability.ManifoldingCapability;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerWakeUpEvent;
import net.minecraftforge.event.entity.player.SleepingTimeCheckEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
public class VergeSleepHandler {
    private static final int SLEEP_FADE_COMPLETE = 100;

    private static final Set<UUID> SHOWN_REST_MESSAGE = new HashSet<>();

    private static boolean isVerge(Player player) {
        return player.level().dimension().location().equals(ManifoldingCapability.DIMENSION_ID);
    }

    @SubscribeEvent
    public static void onSleepingTimeCheck(SleepingTimeCheckEvent event) {
        if (isVerge(event.getEntity())) {
            event.setResult(Event.Result.ALLOW);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer player)) return;
        if (!isVerge(player) || !player.isSleeping()) return;

        if (player.getSleepTimer() == SLEEP_FADE_COMPLETE && SHOWN_REST_MESSAGE.add(player.getUUID())) {
            player.displayClientMessage(
                    Component.translatable("message.null_ouroboros.cannot_rest_on_verge"), true);
        }
    }

    @SubscribeEvent
    public static void onWakeUp(PlayerWakeUpEvent event) {
        if (isVerge(event.getEntity())) {
            SHOWN_REST_MESSAGE.remove(event.getEntity().getUUID());
        }
    }
}
