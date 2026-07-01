package destiny.null_ouroboros.server.network;

import destiny.null_ouroboros.server.entity.DusterbikeEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ServerBoundDusterbikeImpactPacket {
    private final int entityId;

    public ServerBoundDusterbikeImpactPacket(int entityId) {
        this.entityId = entityId;
    }

    public static void encode(ServerBoundDusterbikeImpactPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.entityId);
    }

    public static ServerBoundDusterbikeImpactPacket decode(FriendlyByteBuf buf) {
        return new ServerBoundDusterbikeImpactPacket(buf.readVarInt());
    }

    public static boolean handle(ServerBoundDusterbikeImpactPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) {
                return;
            }

            Entity entity = player.level().getEntity(msg.entityId);
            if (!(entity instanceof DusterbikeEntity bike)) {
                return;
            }

            bike.handleServerWallImpactReport(player);
        });
        return true;
    }
}
