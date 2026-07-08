package destiny.null_ouroboros.client.network;

import destiny.null_ouroboros.common.DusterbikeGear;
import destiny.null_ouroboros.server.entity.DusterbikeEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ClientBoundDusterbikeGearSyncPacket {
    private final int entityId;
    private final byte gearOrdinal;

    public ClientBoundDusterbikeGearSyncPacket(int entityId, byte gearOrdinal) {
        this.entityId = entityId;
        this.gearOrdinal = gearOrdinal;
    }

    public static void encode(ClientBoundDusterbikeGearSyncPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.entityId);
        buf.writeByte(msg.gearOrdinal);
    }

    public static ClientBoundDusterbikeGearSyncPacket decode(FriendlyByteBuf buf) {
        return new ClientBoundDusterbikeGearSyncPacket(buf.readVarInt(), buf.readByte());
    }

    public static boolean handle(ClientBoundDusterbikeGearSyncPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.level == null) {
                return;
            }

            Entity entity = minecraft.level.getEntity(msg.entityId);
            if (!(entity instanceof DusterbikeEntity bike)) {
                return;
            }

            DusterbikeGear[] gears = DusterbikeGear.values();
            if (msg.gearOrdinal < 0 || msg.gearOrdinal >= gears.length) {
                return;
            }

            bike.applyGearFromServer(gears[msg.gearOrdinal]);
        });
        return true;
    }
}
