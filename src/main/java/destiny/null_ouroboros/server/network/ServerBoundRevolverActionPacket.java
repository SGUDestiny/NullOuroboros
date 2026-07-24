package destiny.null_ouroboros.server.network;

import destiny.null_ouroboros.common.revolver.RevolverAction;
import destiny.null_ouroboros.server.item.HeavyRevolverItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ServerBoundRevolverActionPacket {
    private final byte action;

    public ServerBoundRevolverActionPacket(RevolverAction action) {
        this.action = (byte) action.ordinal();
    }

    private ServerBoundRevolverActionPacket(byte action) {
        this.action = action;
    }

    public static void encode(ServerBoundRevolverActionPacket message, FriendlyByteBuf buffer) {
        buffer.writeByte(message.action);
    }

    public static ServerBoundRevolverActionPacket decode(FriendlyByteBuf buffer) {
        return new ServerBoundRevolverActionPacket(buffer.readByte());
    }

    public static boolean handle(ServerBoundRevolverActionPacket message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            RevolverAction action = RevolverAction.byOrdinal(message.action);
            if (player != null && action != null) {
                HeavyRevolverItem.handleAction(player, action);
            }
        });
        return true;
    }
}
