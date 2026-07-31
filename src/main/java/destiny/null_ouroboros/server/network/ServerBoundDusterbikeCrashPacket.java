package destiny.null_ouroboros.server.network;

import destiny.null_ouroboros.server.entity.DusterbikeEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ServerBoundDusterbikeCrashPacket {
    private final int entityId;
    private final float verticalSpeed;

    public ServerBoundDusterbikeCrashPacket(int entityId, float verticalSpeed) {
        this.entityId = entityId;
        this.verticalSpeed = verticalSpeed;
    }

    public static ServerBoundDusterbikeCrashPacket fall(int entityId, float verticalSpeed) {
        return new ServerBoundDusterbikeCrashPacket(entityId, Math.abs(verticalSpeed));
    }

    public static void encode(ServerBoundDusterbikeCrashPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.entityId);
        buf.writeFloat(msg.verticalSpeed);
    }

    public static ServerBoundDusterbikeCrashPacket decode(FriendlyByteBuf buf) {
        return new ServerBoundDusterbikeCrashPacket(buf.readVarInt(), buf.readFloat());
    }

    public static boolean handle(ServerBoundDusterbikeCrashPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }

            DusterbikeEntity bike = null;
            if (player.getVehicle() instanceof DusterbikeEntity riding) {
                bike = riding;
            } else if (player.level().getEntity(msg.entityId) instanceof DusterbikeEntity found) {
                bike = found;
            }
            if (bike == null) {
                return;
            }

            bike.handleServerFallImpactReport(player, msg.verticalSpeed);
        });
        context.setPacketHandled(true);
        return true;
    }
}
