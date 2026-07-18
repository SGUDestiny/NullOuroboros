package destiny.null_ouroboros.server.network;

import destiny.null_ouroboros.common.dusterbike.DusterbikeInteractionConstants;
import destiny.null_ouroboros.server.entity.DusterbikeEntity;
import destiny.null_ouroboros.server.entity.DusterbikeKeyEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ServerBoundDusterbikeKeyPacket {
    private final int entityId;
    private final boolean holding;
    private final boolean pressed;

    public ServerBoundDusterbikeKeyPacket(int entityId, boolean holding, boolean pressed) {
        this.entityId = entityId;
        this.holding = holding;
        this.pressed = pressed;
    }

    public static void encode(ServerBoundDusterbikeKeyPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.entityId);
        buf.writeBoolean(msg.holding);
        buf.writeBoolean(msg.pressed);
    }

    public static ServerBoundDusterbikeKeyPacket decode(FriendlyByteBuf buf) {
        return new ServerBoundDusterbikeKeyPacket(buf.readVarInt(), buf.readBoolean(), buf.readBoolean());
    }

    public static boolean handle(ServerBoundDusterbikeKeyPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) {
                return;
            }

            Entity entity = player.level().getEntity(msg.entityId);
            if (!(entity instanceof DusterbikeEntity bike)) {
                return;
            }

            if (player.distanceToSqr(bike) > DusterbikeInteractionConstants.PART_INTERACTION_REACH
                    * DusterbikeInteractionConstants.PART_INTERACTION_REACH) {
                return;
            }

            if (!msg.holding && !msg.pressed) {
                bike.handleKeyInteraction(player, false, false);
                return;
            }

            Vec3 eyePos = player.getEyePosition(1.0F);
            Vec3 look = player.getViewVector(1.0F);
            if (!rayHitsKey(bike, eyePos, look)) {
                return;
            }

            if (msg.pressed && !msg.holding) {
                bike.handleKeyPortInteraction(player, InteractionHand.MAIN_HAND);
                return;
            }

            bike.handleKeyInteraction(player, msg.holding, msg.pressed);
        });
        return true;
    }

    private static boolean rayHitsKey(DusterbikeEntity bike, Vec3 eyePos, Vec3 look) {
        DusterbikeKeyEntity key = bike.getKeyEntity();
        if (key == null) {
            return false;
        }

        Vec3 end = eyePos.add(look.scale(DusterbikeInteractionConstants.PART_INTERACTION_REACH));
        return key.getBoundingBox().clip(eyePos, end).isPresent();
    }
}
