package destiny.null_ouroboros.client.network;

import destiny.null_ouroboros.client.render.player_anim.PlayerAnimController;
import destiny.null_ouroboros.common.player_anim.PlayOptions;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ClientboundPlayerAnimPlayPacket {
    private final int entityId;
    private final ResourceLocation animationId;
    private final long startGameTime;
    private final PlayOptions options;

    public ClientboundPlayerAnimPlayPacket(
            int entityId, ResourceLocation animationId, long startGameTime, PlayOptions options) {
        this.entityId = entityId;
        this.animationId = animationId;
        this.startGameTime = startGameTime;
        this.options = options;
    }

    public static void encode(ClientboundPlayerAnimPlayPacket pkt, FriendlyByteBuf buf) {
        buf.writeVarInt(pkt.entityId);
        buf.writeResourceLocation(pkt.animationId);
        buf.writeVarLong(pkt.startGameTime);
        pkt.options.encode(buf);
    }

    public static ClientboundPlayerAnimPlayPacket decode(FriendlyByteBuf buf) {
        return new ClientboundPlayerAnimPlayPacket(
                buf.readVarInt(),
                buf.readResourceLocation(),
                buf.readVarLong(),
                PlayOptions.decode(buf)
        );
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            var level = Minecraft.getInstance().level;
            if (level == null) {
                return;
            }
            Entity entity = level.getEntity(this.entityId);
            if (!(entity instanceof Player player)) {
                return;
            }
            PlayerAnimController.play(player.getUUID(), this.animationId, this.startGameTime, this.options);
        });
        ctx.get().setPacketHandled(true);
    }
}
