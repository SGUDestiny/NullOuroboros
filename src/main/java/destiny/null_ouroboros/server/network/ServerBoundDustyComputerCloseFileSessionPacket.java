package destiny.null_ouroboros.server.network;

import destiny.null_ouroboros.server.block.entity.DustyComputerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ServerBoundDustyComputerCloseFileSessionPacket {
    private final BlockPos pos;
    private final boolean saveChanges;
    private final String content;

    public ServerBoundDustyComputerCloseFileSessionPacket(BlockPos pos, boolean saveChanges, String content) {
        this.pos = pos;
        this.saveChanges = saveChanges;
        this.content = content;
    }

    public static void encode(ServerBoundDustyComputerCloseFileSessionPacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
        buf.writeBoolean(msg.saveChanges);
        buf.writeUtf(msg.content);
    }

    public static ServerBoundDustyComputerCloseFileSessionPacket decode(FriendlyByteBuf buf) {
        return new ServerBoundDustyComputerCloseFileSessionPacket(buf.readBlockPos(), buf.readBoolean(), buf.readUtf());
    }

    public static boolean handle(ServerBoundDustyComputerCloseFileSessionPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            Level level = player.level();
            if (level.getBlockEntity(msg.pos) instanceof DustyComputerBlockEntity computer) {
                computer.closeFileSession(msg.saveChanges ? msg.content : null, player);
            }
        });
        return true;
    }
}
