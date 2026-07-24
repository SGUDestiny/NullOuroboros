package destiny.null_ouroboros.common.player_anim;

import destiny.null_ouroboros.client.network.ClientboundPlayerAnimCancelPacket;
import destiny.null_ouroboros.server.item.HeavyRevolverItem;
import destiny.null_ouroboros.server.registry.PacketHandlerRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

@Mod.EventBusSubscriber
public final class PlayerAnimServerEvents {
    private PlayerAnimServerEvents() {}

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (!(event.getEntity() instanceof ServerPlayer tracker)) {
            return;
        }
        if (!(event.getTarget() instanceof Player target)) {
            return;
        }
        PlayerAnimation.syncTo(tracker, target);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        PlayerAnimTracker.clear(event.getEntity().getUUID());
        HeavyRevolverItem.clearDrawState(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (player.level().isClientSide) {
            return;
        }
        HeavyRevolverItem.clearDrawState(player.getUUID());
        PlayerAnimInstance cleared = PlayerAnimTracker.clear(player.getUUID());
        if (cleared == null) {
            return;
        }
        PacketHandlerRegistry.INSTANCE.send(
                PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                new ClientboundPlayerAnimCancelPacket(player.getId(), null)
        );
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) {
            return;
        }
        Player player = event.player;
        PlayerAnimInstance instance = PlayerAnimTracker.get(player.getUUID());
        if (instance == null || instance.options().loopMode() != LoopMode.PLAY_ONCE) {
            return;
        }
        float lengthSeconds = PlayerAnimationMeta.lengthInSeconds(instance.animationId());
        if (lengthSeconds < 0.0F) {
            return;
        }
        long lengthTicks = Math.max(1L, Math.round(lengthSeconds * 20.0F));
        if (player.level().getGameTime() - instance.startGameTime() < lengthTicks) {
            return;
        }
        if (HeavyRevolverPlayerAnims.isRevolverAnim(instance.animationId())
                && player.getMainHandItem().getItem() instanceof HeavyRevolverItem) {
            PlayerAnimation.playAtEnd(player, HeavyRevolverPlayerAnims.HOLD_ID, HeavyRevolverPlayerAnims.holdOptions());
            return;
        }
        PlayerAnimTracker.clear(player.getUUID());
        PacketHandlerRegistry.INSTANCE.send(
                PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                new ClientboundPlayerAnimCancelPacket(player.getId(), instance.animationId())
        );
    }
}
