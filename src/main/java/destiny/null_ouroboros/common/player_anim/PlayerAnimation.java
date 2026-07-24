package destiny.null_ouroboros.common.player_anim;

import destiny.null_ouroboros.client.network.ClientboundPlayerAnimCancelPacket;
import destiny.null_ouroboros.client.network.ClientboundPlayerAnimPlayPacket;
import destiny.null_ouroboros.server.registry.PacketHandlerRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.PacketDistributor;

public final class PlayerAnimation {
    private PlayerAnimation() {}

    public static void play(LivingEntity entity, ResourceLocation animationId, PlayOptions options) {
        playInternal(entity, animationId, options, false);
    }

    public static void playAtEnd(LivingEntity entity, ResourceLocation animationId, PlayOptions options) {
        PlayOptions resolved = options == null ? PlayOptions.defaults() : options;
        playInternal(entity, animationId, PlayOptions.builder()
                .loopMode(resolved.loopMode())
                .override(resolved.override())
                .renderFirstPerson(resolved.renderFirstPerson())
                .renderFirstPersonHead(resolved.renderFirstPersonHead())
                .renderFirstPersonBody(resolved.renderFirstPersonBody())
                .aimFollowArms(resolved.aimFollowArms())
                .aimFollowArmsX(resolved.aimFollowArmsX())
                .aimFollowMode(resolved.aimFollowMode())
                .mirrorForLeftHanded(resolved.mirrorForLeftHanded())
                .startAtEnd(true)
                .build(), true);
    }

    private static void playInternal(LivingEntity entity, ResourceLocation animationId, PlayOptions options, boolean forceAtEnd) {
        if (!(entity instanceof Player player)) {
            return;
        }
        if (player.level().isClientSide) {
            return;
        }

        PlayOptions resolved = options == null ? PlayOptions.defaults() : options;
        boolean atEnd = forceAtEnd || resolved.startAtEnd();
        long startGameTime = player.level().getGameTime();
        if (atEnd) {
            float lengthSeconds = PlayerAnimationMeta.lengthInSeconds(animationId);
            if (lengthSeconds >= 0.0F) {
                long lengthTicks = Math.max(1L, Math.round(lengthSeconds * 20.0F));
                startGameTime -= lengthTicks;
            }
        }

        PlayerAnimInstance instance = new PlayerAnimInstance(animationId, resolved, startGameTime);
        PlayerAnimTracker.set(player.getUUID(), instance);

        ClientboundPlayerAnimPlayPacket packet = new ClientboundPlayerAnimPlayPacket(
                player.getId(), animationId, startGameTime, resolved);
        PacketHandlerRegistry.INSTANCE.send(
                PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                packet
        );
    }

    public static void cancel(LivingEntity entity, ResourceLocation animationId) {
        if (!(entity instanceof Player player)) {
            return;
        }
        if (player.level().isClientSide) {
            return;
        }

        PlayerAnimInstance cleared = animationId == null
                ? PlayerAnimTracker.clear(player.getUUID())
                : PlayerAnimTracker.clearMatching(player.getUUID(), animationId);
        if (cleared == null) {
            return;
        }

        ClientboundPlayerAnimCancelPacket packet = new ClientboundPlayerAnimCancelPacket(
                player.getId(), animationId);
        PacketHandlerRegistry.INSTANCE.send(
                PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                packet
        );
    }

    public static void cancelAll(LivingEntity entity) {
        cancel(entity, null);
    }

    public static void syncTo(ServerPlayer tracker, Player target) {
        PlayerAnimInstance instance = PlayerAnimTracker.get(target.getUUID());
        if (instance == null) {
            return;
        }
        PacketHandlerRegistry.INSTANCE.send(
                PacketDistributor.PLAYER.with(() -> tracker),
                new ClientboundPlayerAnimPlayPacket(
                        target.getId(),
                        instance.animationId(),
                        instance.startGameTime(),
                        instance.options()
                )
        );
    }
}
