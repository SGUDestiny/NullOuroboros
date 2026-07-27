package destiny.null_ouroboros.server.network;

import destiny.null_ouroboros.common.respirator.FilterAction;
import destiny.null_ouroboros.server.item.RespiratorFilterActions;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ServerboundFilterActionPacket {
    private final byte action;

    public ServerboundFilterActionPacket(FilterAction action) {
        this.action = (byte) action.ordinal();
    }

    private ServerboundFilterActionPacket(byte action) {
        this.action = action;
    }

    public static void encode(ServerboundFilterActionPacket message, FriendlyByteBuf buffer) {
        buffer.writeByte(message.action);
    }

    public static ServerboundFilterActionPacket decode(FriendlyByteBuf buffer) {
        return new ServerboundFilterActionPacket(buffer.readByte());
    }

    public static boolean handle(ServerboundFilterActionPacket message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }
            FilterAction action = FilterAction.byOrdinal(message.action);
            RespiratorFilterActions.handleAction(player, action);
        });
        context.setPacketHandled(true);
        return true;
    }
}
