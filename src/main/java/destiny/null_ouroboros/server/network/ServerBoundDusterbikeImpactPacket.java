package destiny.null_ouroboros.server.network;

import destiny.null_ouroboros.server.entity.DusterbikeEntity;
import destiny.null_ouroboros.server.entity.DusterbikePhysics;
import destiny.null_ouroboros.server.registry.DamageTypeRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ServerBoundDusterbikeImpactPacket {
    private final int entityId;
    private final float impactSpeed;

    public ServerBoundDusterbikeImpactPacket(int entityId, float impactSpeed) {
        this.entityId = entityId;
        this.impactSpeed = impactSpeed;
    }

    public static void encode(ServerBoundDusterbikeImpactPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.entityId);
        buf.writeFloat(msg.impactSpeed);
    }

    public static ServerBoundDusterbikeImpactPacket decode(FriendlyByteBuf buf) {
        return new ServerBoundDusterbikeImpactPacket(buf.readVarInt(), buf.readFloat());
    }

    public static boolean handle(ServerBoundDusterbikeImpactPacket msg, Supplier<NetworkEvent.Context> ctx) {
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

            float damage = DusterbikePhysics.computeWallImpactDamage(msg.impactSpeed);
            if (damage <= 0.0F) {
                return;
            }

            player.hurt(DamageTypeRegistry.getSimpleDamageSource(player.level(), DamageTypeRegistry.DUSTERBIKE_IMPACT), damage);
        });
        return true;
    }
}
