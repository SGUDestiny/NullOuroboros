package destiny.null_ouroboros.server.network;

import destiny.null_ouroboros.common.DusterbikeGear;
import destiny.null_ouroboros.server.entity.DusterbikeEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ServerBoundDusterbikeShiftPacket {
    private final int entityId;
    private final byte direction;

    public ServerBoundDusterbikeShiftPacket(int entityId, int direction) {
        this.entityId = entityId;
        this.direction = (byte) direction;
    }

    public static void encode(ServerBoundDusterbikeShiftPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.entityId);
        buf.writeByte(msg.direction);
    }

    public static ServerBoundDusterbikeShiftPacket decode(FriendlyByteBuf buf) {
        return new ServerBoundDusterbikeShiftPacket(buf.readVarInt(), buf.readByte());
    }

    public static boolean handle(ServerBoundDusterbikeShiftPacket msg, Supplier<NetworkEvent.Context> ctx) {
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

            if (msg.direction != 1 && msg.direction != -1) {
                return;
            }

            if (bike.shiftGear(msg.direction)) {
                DusterbikeGear gear = bike.getGear();
                player.displayClientMessage(
                        Component.translatable(
                                "message.null_ouroboros.dusterbike.gear",
                                Component.translatable(gear.translationKey())),
                        true);
            }
        });
        return true;
    }
}
