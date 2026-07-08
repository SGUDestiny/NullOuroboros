package destiny.null_ouroboros.server.network;

import destiny.null_ouroboros.server.entity.DusterbikeEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ServerBoundDusterbikeHeadlightPacket {
    private final int bikeId;

    public ServerBoundDusterbikeHeadlightPacket(int bikeId) {
        this.bikeId = bikeId;
    }

    public static void encode(ServerBoundDusterbikeHeadlightPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.bikeId);
    }

    public static ServerBoundDusterbikeHeadlightPacket decode(FriendlyByteBuf buf) {
        return new ServerBoundDusterbikeHeadlightPacket(buf.readVarInt());
    }

    public static boolean handle(ServerBoundDusterbikeHeadlightPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) {
                return;
            }

            Entity entity = player.level().getEntity(msg.bikeId);
            if (entity instanceof DusterbikeEntity bike && bike.getControllingPassenger() == player) {
                bike.toggleHeadlights();
            }
        });
        return true;
    }
}
