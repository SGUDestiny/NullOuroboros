package destiny.null_ouroboros.server.network;

import destiny.null_ouroboros.common.dusterbike.DusterbikeInteractionConstants;
import destiny.null_ouroboros.common.dusterbike.HoistPartTargetType;
import destiny.null_ouroboros.server.entity.EngineHoistEntity;
import destiny.null_ouroboros.server.entity.HoistPartInteractionEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ServerBoundHoistPartInteractPacket {
    private final int hoistId;
    private final HoistPartTargetType targetType;
    private final InteractionHand hand;
    private final boolean secondaryUse;

    public ServerBoundHoistPartInteractPacket(int hoistId, HoistPartTargetType targetType, InteractionHand hand, boolean secondaryUse) {
        this.hoistId = hoistId;
        this.targetType = targetType;
        this.hand = hand;
        this.secondaryUse = secondaryUse;
    }

    public static void encode(ServerBoundHoistPartInteractPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.hoistId);
        buf.writeVarInt(msg.targetType.id());
        buf.writeVarInt(msg.hand.ordinal());
        buf.writeBoolean(msg.secondaryUse);
    }

    public static ServerBoundHoistPartInteractPacket decode(FriendlyByteBuf buf) {
        int hoistId = buf.readVarInt();
        HoistPartTargetType targetType = HoistPartTargetType.byId(buf.readVarInt());
        InteractionHand[] hands = InteractionHand.values();
        int handOrdinal = buf.readVarInt();
        return new ServerBoundHoistPartInteractPacket(
                hoistId,
                targetType,
                hands[Math.max(0, Math.min(hands.length - 1, handOrdinal))],
                buf.readBoolean());
    }

    public static boolean handle(ServerBoundHoistPartInteractPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            Entity entity = player.level().getEntity(msg.hoistId);
            if (!(entity instanceof EngineHoistEntity hoist)) return;

            if (player.distanceToSqr(hoist) > DusterbikeInteractionConstants.PART_INTERACTION_REACH * DusterbikeInteractionConstants.PART_INTERACTION_REACH)
                return;

            HoistPartInteractionEntity target = hoist.getPartTargetEntity(msg.targetType);
            if (target == null) return;

            Vec3 eyePos = player.getEyePosition(1.0F);
            Vec3 look = player.getViewVector(1.0F);
            Vec3 end = eyePos.add(look.scale(DusterbikeInteractionConstants.PART_INTERACTION_REACH));
            if (target.getBoundingBox().clip(eyePos, end).isEmpty()) return;

            hoist.handlePartInteraction(player, msg.hand, msg.targetType, msg.secondaryUse);
        });
        return true;
    }
}