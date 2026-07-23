package destiny.null_ouroboros.server.network;

import destiny.null_ouroboros.server.block.entity.DustyComputerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public class ServerBoundDustyComputerShutdownPacket {
    private final BlockPos pos;

    public ServerBoundDustyComputerShutdownPacket(BlockPos pos) {
        this.pos = pos;
    }

    public static void encode(ServerBoundDustyComputerShutdownPacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
    }

    public static ServerBoundDustyComputerShutdownPacket decode(FriendlyByteBuf buf) {
        return new ServerBoundDustyComputerShutdownPacket(buf.readBlockPos());
    }

    public static boolean handle(ServerBoundDustyComputerShutdownPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();

            if (player == null) return;

            if (player.level().getBlockEntity(msg.pos) instanceof DustyComputerBlockEntity computer) {
                if (!computer.canPlayerInteract(player)) return;
                computer.shutdown();
            }
        });
        return true;
    }
}
