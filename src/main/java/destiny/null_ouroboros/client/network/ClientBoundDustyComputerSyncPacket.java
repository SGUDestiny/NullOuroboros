package destiny.null_ouroboros.client.network;

import destiny.null_ouroboros.server.block.entity.DustyComputerBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class ClientBoundDustyComputerSyncPacket {
    private final BlockPos pos;
    private final List<String> lines;
    private final String currentPath;

    public ClientBoundDustyComputerSyncPacket(BlockPos pos, List<String> lines, String currentPath) {
        this.pos = pos;
        this.lines = lines;
        this.currentPath = currentPath;
    }

    public static void encode(ClientBoundDustyComputerSyncPacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
        buf.writeCollection(msg.lines, FriendlyByteBuf::writeUtf);
        buf.writeUtf(msg.currentPath);
    }

    public static ClientBoundDustyComputerSyncPacket decode(FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        List<String> lines = buf.readCollection(ArrayList::new, FriendlyByteBuf::readUtf);
        String currentPath = buf.readUtf();
        return new ClientBoundDustyComputerSyncPacket(pos, lines, currentPath);
    }

    public static boolean handle(ClientBoundDustyComputerSyncPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            var level = Minecraft.getInstance().level;
            if (level == null) return;
            var be = level.getBlockEntity(msg.pos);
            if (be instanceof DustyComputerBlockEntity computer) {
                computer.setLines(msg.lines);
                computer.setCurrentPath(msg.currentPath);
            }
        });
        return true;
    }
}