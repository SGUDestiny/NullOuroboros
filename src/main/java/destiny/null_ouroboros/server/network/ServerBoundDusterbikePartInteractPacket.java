package destiny.null_ouroboros.server.network;

import destiny.null_ouroboros.common.dusterbike.DusterbikeInteractionConstants;
import destiny.null_ouroboros.common.dusterbike.DusterbikePartTargetType;
import destiny.null_ouroboros.server.entity.DusterbikeEntity;
import destiny.null_ouroboros.server.entity.DusterbikePartInteractionEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ServerBoundDusterbikePartInteractPacket {
    private final int bikeId;
    private final DusterbikePartTargetType targetType;
    private final InteractionHand hand;
    private final boolean secondaryUse;

    public ServerBoundDusterbikePartInteractPacket(int bikeId, DusterbikePartTargetType targetType, InteractionHand hand, boolean secondaryUse) {
        this.bikeId = bikeId;
        this.targetType = targetType;
        this.hand = hand;
        this.secondaryUse = secondaryUse;
    }

    public static void encode(ServerBoundDusterbikePartInteractPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.bikeId);
        buf.writeVarInt(msg.targetType.id());
        buf.writeVarInt(msg.hand.ordinal());
        buf.writeBoolean(msg.secondaryUse);
    }

    public static ServerBoundDusterbikePartInteractPacket decode(FriendlyByteBuf buf) {
        int bikeId = buf.readVarInt();
        DusterbikePartTargetType targetType = DusterbikePartTargetType.byId(buf.readVarInt());
        InteractionHand[] hands = InteractionHand.values();
        int handOrdinal = buf.readVarInt();
        return new ServerBoundDusterbikePartInteractPacket(
                bikeId,
                targetType,
                hands[Math.max(0, Math.min(hands.length - 1, handOrdinal))],
                buf.readBoolean());
    }

    public static boolean handle(ServerBoundDusterbikePartInteractPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) {
                return;
            }

            Entity entity = player.level().getEntity(msg.bikeId);
            if (!(entity instanceof DusterbikeEntity bike)) {
                return;
            }

            if (player.distanceToSqr(bike) > DusterbikeInteractionConstants.PART_INTERACTION_REACH * DusterbikeInteractionConstants.PART_INTERACTION_REACH) {
                return;
            }

            if (msg.targetType == null) {
                return;
            }

            DusterbikePartInteractionEntity target = bike.getPartTargetEntity(msg.targetType);
            if (target == null) {
                return;
            }

            Vec3 eyePos = player.getEyePosition(1.0F);
            Vec3 look = player.getViewVector(1.0F);
            Vec3 end = eyePos.add(look.scale(DusterbikeInteractionConstants.PART_INTERACTION_REACH));
            if (target.getBoundingBox().clip(eyePos, end).isEmpty()) {
                return;
            }

            bike.handlePartInteraction(player, msg.hand, msg.targetType, msg.secondaryUse);
        });
        return true;
    }
}
