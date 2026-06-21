package destiny.null_ouroboros.client.network;

import destiny.null_ouroboros.server.block.entity.TemporalSurgeDetectorBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ClientBoundDetectorPlacePacket {
    private final BlockPos pos;
    private final float angle1;
    private final float angle2;

    public ClientBoundDetectorPlacePacket(BlockPos pos, float angle1, float angle2) {
        this.pos = pos;
        this.angle1 = angle1;
        this.angle2 = angle2;
    }

    public static void encode(ClientBoundDetectorPlacePacket pkt, FriendlyByteBuf buf) {
        buf.writeBlockPos(pkt.pos);
        buf.writeFloat(pkt.angle1);
        buf.writeFloat(pkt.angle2);
    }

    public static ClientBoundDetectorPlacePacket decode(FriendlyByteBuf buf) {
        return new ClientBoundDetectorPlacePacket(buf.readBlockPos(), buf.readFloat(), buf.readFloat());
    }

    public static boolean handle(ClientBoundDetectorPlacePacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            var level = Minecraft.getInstance().level;
            if (level != null && level.getBlockEntity(pkt.pos) instanceof TemporalSurgeDetectorBlockEntity be) {
                be.ring1Angle = pkt.angle1;
                be.ring2Angle = pkt.angle2;
                be.initialized = true;
            }
        });
        return true;
    }
}