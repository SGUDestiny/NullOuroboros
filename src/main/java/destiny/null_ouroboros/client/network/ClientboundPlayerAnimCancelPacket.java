package destiny.null_ouroboros.client.network;

import destiny.null_ouroboros.client.render.player_anim.PlayerAnimController;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;

import javax.annotation.Nullable;
import java.util.function.Supplier;

public class ClientboundPlayerAnimCancelPacket {
    private final int entityId;
    @Nullable
    private final ResourceLocation animationId;

    public ClientboundPlayerAnimCancelPacket(int entityId, @Nullable ResourceLocation animationId) {
        this.entityId = entityId;
        this.animationId = animationId;
    }

    public static void encode(ClientboundPlayerAnimCancelPacket pkt, FriendlyByteBuf buf) {
        buf.writeVarInt(pkt.entityId);
        buf.writeBoolean(pkt.animationId != null);
        if (pkt.animationId != null) {
            buf.writeResourceLocation(pkt.animationId);
        }
    }

    public static ClientboundPlayerAnimCancelPacket decode(FriendlyByteBuf buf) {
        int entityId = buf.readVarInt();
        ResourceLocation animationId = buf.readBoolean() ? buf.readResourceLocation() : null;
        return new ClientboundPlayerAnimCancelPacket(entityId, animationId);
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
            PlayerAnimController.cancel(player.getUUID(), this.animationId);
        });
        ctx.get().setPacketHandled(true);
    }
}
