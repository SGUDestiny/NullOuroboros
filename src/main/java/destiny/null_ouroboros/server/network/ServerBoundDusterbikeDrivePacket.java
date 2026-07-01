package destiny.null_ouroboros.server.network;

import destiny.null_ouroboros.common.DusterbikeGear;
import destiny.null_ouroboros.server.entity.DusterbikeEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ServerBoundDusterbikeDrivePacket {
    private final int entityId;
    private final byte gearOrdinal;
    private final float forwardSpeed;
    private final float steerAngle;

    public ServerBoundDusterbikeDrivePacket(int entityId, byte gearOrdinal, float forwardSpeed, float steerAngle) {
        this.entityId = entityId;
        this.gearOrdinal = gearOrdinal;
        this.forwardSpeed = forwardSpeed;
        this.steerAngle = steerAngle;
    }

    public static void encode(ServerBoundDusterbikeDrivePacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.entityId);
        buf.writeByte(msg.gearOrdinal);
        buf.writeFloat(msg.forwardSpeed);
        buf.writeFloat(msg.steerAngle);
    }

    public static ServerBoundDusterbikeDrivePacket decode(FriendlyByteBuf buf) {
        return new ServerBoundDusterbikeDrivePacket(buf.readVarInt(), buf.readByte(), buf.readFloat(), buf.readFloat());
    }

    public static boolean handle(ServerBoundDusterbikeDrivePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) {
                return;
            }

            Entity entity = player.level().getEntity(msg.entityId);
            if (!(entity instanceof DusterbikeEntity bike)) {
                return;
            }

            if (player.getVehicle() != bike || bike.getControllingPassenger() != player) {
                return;
            }

            DusterbikeGear[] gears = DusterbikeGear.values();
            if (msg.gearOrdinal < 0 || msg.gearOrdinal >= gears.length) {
                return;
            }

            bike.applyClientDriveState(gears[msg.gearOrdinal], msg.forwardSpeed, msg.steerAngle);
        });
        return true;
    }
}
