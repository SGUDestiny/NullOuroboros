package destiny.null_ouroboros.server.network;

import destiny.null_ouroboros.server.block.entity.DustyComputerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ServerBoundDustyComputerP2pToggleModePacket {
    private final BlockPos pos;

    public ServerBoundDustyComputerP2pToggleModePacket(BlockPos pos) {
        this.pos = pos;
    }

    public static void encode(ServerBoundDustyComputerP2pToggleModePacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
    }

    public static ServerBoundDustyComputerP2pToggleModePacket decode(FriendlyByteBuf buf) {
        return new ServerBoundDustyComputerP2pToggleModePacket(buf.readBlockPos());
    }

    public static boolean handle(ServerBoundDustyComputerP2pToggleModePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            if (player.level().getBlockEntity(msg.pos) instanceof DustyComputerBlockEntity computer) {
                computer.toggleP2pMode(player);
            }
        });
        return true;
    }
}
