package destiny.null_ouroboros.client.network;

import destiny.null_ouroboros.server.entity.BurrowBeaconEntity;
import destiny.null_ouroboros.server.registry.PacketHandlerRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

import static destiny.null_ouroboros.server.entity.BurrowBeaconEntity.CONNECTIONS;

public class ClientBoundBurrowBeaconSyncPacket {
    private final int entityId;
    private final Set<BlockPos> connections;

    public ClientBoundBurrowBeaconSyncPacket(int entityId, Set<BlockPos> connections) {
        this.entityId = entityId;
        this.connections = connections;
    }

    public static void send(BurrowBeaconEntity beacon) {
        PacketHandlerRegistry.INSTANCE.send(
                PacketDistributor.TRACKING_ENTITY.with(() -> beacon),
                new ClientBoundBurrowBeaconSyncPacket(beacon.getId(), beacon.getConnectedPositions())
        );
    }

    public static void encode(ClientBoundBurrowBeaconSyncPacket pkt, FriendlyByteBuf buf) {
        buf.writeVarInt(pkt.entityId);
        buf.writeVarInt(pkt.connections.size());
        for (BlockPos pos : pkt.connections) buf.writeBlockPos(pos);
    }

    public static ClientBoundBurrowBeaconSyncPacket decode(FriendlyByteBuf buf) {
        int id = buf.readVarInt();
        int count = buf.readVarInt();
        Set<BlockPos> set = new HashSet<>();
        for (int i = 0; i < count; i++) set.add(buf.readBlockPos());
        return new ClientBoundBurrowBeaconSyncPacket(id, set);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            var level = Minecraft.getInstance().level;
            if (level == null) {
                return;
            }
            Entity entity = level.getEntity(this.entityId);
            if (entity instanceof BurrowBeaconEntity beacon) {
                CompoundTag tag = new CompoundTag();
                long[] arr = new long[this.connections.size()];
                int i = 0;
                for (BlockPos p : this.connections) arr[i++] = p.asLong();
                tag.putLongArray("list", arr);
                beacon.getEntityData().set(CONNECTIONS, tag);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}