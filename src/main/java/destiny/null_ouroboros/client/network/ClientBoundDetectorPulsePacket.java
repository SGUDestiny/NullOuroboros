package destiny.null_ouroboros.client.network;

import destiny.null_ouroboros.server.block.entity.TemporalSurgeDetectorBlockEntity;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ClientBoundDetectorPulsePacket {
    private final BlockPos pos;

    public ClientBoundDetectorPulsePacket(BlockPos pos) { this.pos = pos; }

    public static void encode(ClientBoundDetectorPulsePacket pkt, FriendlyByteBuf buf) {
        buf.writeBlockPos(pkt.pos);
    }

    public static ClientBoundDetectorPulsePacket decode(FriendlyByteBuf buf) {
        return new ClientBoundDetectorPulsePacket(buf.readBlockPos());
    }

    public static boolean handle(ClientBoundDetectorPulsePacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ClientLevel level = net.minecraft.client.Minecraft.getInstance().level;

            if (level != null && level.getBlockEntity(pkt.pos) instanceof TemporalSurgeDetectorBlockEntity temporalEntity) {
                temporalEntity.burstBoost = 1f;
            }
        });
        return true;
    }
}
