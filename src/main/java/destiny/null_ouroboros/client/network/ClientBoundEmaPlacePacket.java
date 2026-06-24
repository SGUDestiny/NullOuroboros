package destiny.null_ouroboros.client.network;

import destiny.null_ouroboros.server.block.entity.ElectromagneticAssemblyBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ClientBoundEmaPlacePacket {
    private final BlockPos pos;
    private final float spinnerAngle;
    private final float vaneBaseAngle;

    public ClientBoundEmaPlacePacket(BlockPos pos, float spinnerAngle, float vaneBaseAngle) {
        this.pos = pos;
        this.spinnerAngle = spinnerAngle;
        this.vaneBaseAngle = vaneBaseAngle;
    }

    public static void encode(ClientBoundEmaPlacePacket pkt, FriendlyByteBuf buf) {
        buf.writeBlockPos(pkt.pos);
        buf.writeFloat(pkt.spinnerAngle);
        buf.writeFloat(pkt.vaneBaseAngle);
    }

    public static ClientBoundEmaPlacePacket decode(FriendlyByteBuf buf) {
        return new ClientBoundEmaPlacePacket(buf.readBlockPos(), buf.readFloat(), buf.readFloat());
    }

    public static boolean handle(ClientBoundEmaPlacePacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            var level = Minecraft.getInstance().level;
            if (level != null && level.getBlockEntity(pkt.pos) instanceof ElectromagneticAssemblyBlockEntity be) {
                be.spinnerAngle = pkt.spinnerAngle;
                be.vaneBaseAngle = pkt.vaneBaseAngle;
                be.initialized = true;
            }
        });
        return true;
    }
}
