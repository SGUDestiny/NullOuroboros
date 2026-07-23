package destiny.null_ouroboros.client.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class ClientBoundParticleBatchPacket {
    public record Entry(double x, double y, double z, double vx, double vy, double vz) {
    }

    private final ResourceLocation particleId;
    private final List<Entry> entries;

    public ClientBoundParticleBatchPacket(ResourceLocation particleId, List<Entry> entries) {
        this.particleId = particleId;
        this.entries = entries;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeResourceLocation(this.particleId);
        buf.writeVarInt(this.entries.size());
        for (Entry entry : this.entries) {
            buf.writeDouble(entry.x);
            buf.writeDouble(entry.y);
            buf.writeDouble(entry.z);
            buf.writeDouble(entry.vx);
            buf.writeDouble(entry.vy);
            buf.writeDouble(entry.vz);
        }
    }

    public static ClientBoundParticleBatchPacket decode(FriendlyByteBuf buf) {
        ResourceLocation id = buf.readResourceLocation();
        int count = buf.readVarInt();
        List<Entry> entries = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            entries.add(new Entry(
                    buf.readDouble(),
                    buf.readDouble(),
                    buf.readDouble(),
                    buf.readDouble(),
                    buf.readDouble(),
                    buf.readDouble()));
        }
        return new ClientBoundParticleBatchPacket(id, entries);
    }

    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            for (Entry entry : entries) {
                ClientBoundPacketHandler.sendParticle(
                        particleId, entry.x, entry.y, entry.z, entry.vx, entry.vy, entry.vz, 1);
            }
        });
        return true;
    }
}
