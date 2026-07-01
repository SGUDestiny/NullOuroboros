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
    private final float frontWheelRotation;
    private final float rearWheelRotation;

    public ServerBoundDusterbikeDrivePacket(
            int entityId,
            byte gearOrdinal,
            float forwardSpeed,
            float steerAngle,
            float frontWheelRotation,
            float rearWheelRotation) {
        this.entityId = entityId;
        this.gearOrdinal = gearOrdinal;
        this.forwardSpeed = forwardSpeed;
        this.steerAngle = steerAngle;
        this.frontWheelRotation = frontWheelRotation;
        this.rearWheelRotation = rearWheelRotation;
    }

    public static void encode(ServerBoundDusterbikeDrivePacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.entityId);
        buf.writeByte(msg.gearOrdinal);
        buf.writeFloat(msg.forwardSpeed);
        buf.writeFloat(msg.steerAngle);
        buf.writeFloat(msg.frontWheelRotation);
        buf.writeFloat(msg.rearWheelRotation);
    }

    public static ServerBoundDusterbikeDrivePacket decode(FriendlyByteBuf buf) {
        return new ServerBoundDusterbikeDrivePacket(
                buf.readVarInt(),
                buf.readByte(),
                buf.readFloat(),
                buf.readFloat(),
                buf.readFloat(),
                buf.readFloat());
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

            boolean isDriver = player.getVehicle() == bike && bike.getControllingPassenger() == player;
            if (!isDriver && player.distanceToSqr(bike) > 64.0D) {
                return;
            }

            DusterbikeGear[] gears = DusterbikeGear.values();
            if (msg.gearOrdinal < 0 || msg.gearOrdinal >= gears.length) {
                return;
            }

            bike.applyClientDriveState(
                    gears[msg.gearOrdinal],
                    msg.forwardSpeed,
                    msg.steerAngle,
                    msg.frontWheelRotation,
                    msg.rearWheelRotation);
        });
        return true;
    }
}
