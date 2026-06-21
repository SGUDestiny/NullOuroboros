package destiny.null_ouroboros.client.network;

import destiny.null_ouroboros.server.block.entity.DustyComputerBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class ClientBoundDustyComputerSyncPacket {
    private final BlockPos pos;
    private final List<String> lines;

    public ClientBoundDustyComputerSyncPacket(BlockPos pos, List<String> lines) {
        this.pos = pos;
        this.lines = lines;
    }

    public static void encode(ClientBoundDustyComputerSyncPacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
        buf.writeCollection(msg.lines, FriendlyByteBuf::writeUtf);
    }

    public static ClientBoundDustyComputerSyncPacket decode(FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        List<String> lines = buf.readCollection(ArrayList::new, FriendlyByteBuf::readUtf);

        return new ClientBoundDustyComputerSyncPacket(pos, lines);
    }

    public static boolean handle(ClientBoundDustyComputerSyncPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ClientLevel level = Minecraft.getInstance().level;

            if (level == null) return;

            DustyComputerBlockEntity computer = (DustyComputerBlockEntity) level.getBlockEntity(msg.pos);

            if (computer != null) {
                computer.setLines(msg.lines);
            }
        });
        return true;
    }
}