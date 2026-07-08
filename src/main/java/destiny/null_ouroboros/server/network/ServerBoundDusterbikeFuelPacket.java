package destiny.null_ouroboros.server.network;

import destiny.null_ouroboros.server.entity.DusterbikeEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ServerBoundDusterbikeFuelPacket {
    private final int bikeId;
    private final boolean holding;
    private final boolean drain;

    public ServerBoundDusterbikeFuelPacket(int bikeId, boolean holding, boolean drain) {
        this.bikeId = bikeId;
        this.holding = holding;
        this.drain = drain;
    }

    public static void encode(ServerBoundDusterbikeFuelPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.bikeId);
        buf.writeBoolean(msg.holding);
        buf.writeBoolean(msg.drain);
    }

    public static ServerBoundDusterbikeFuelPacket decode(FriendlyByteBuf buf) {
        return new ServerBoundDusterbikeFuelPacket(buf.readVarInt(), buf.readBoolean(), buf.readBoolean());
    }

    public static boolean handle(ServerBoundDusterbikeFuelPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            Entity entity = player.level().getEntity(msg.bikeId);
            if (entity instanceof DusterbikeEntity bike) {
                bike.handleFuelHold(player, msg.holding, msg.drain);
            }
        });
        return true;
    }
}