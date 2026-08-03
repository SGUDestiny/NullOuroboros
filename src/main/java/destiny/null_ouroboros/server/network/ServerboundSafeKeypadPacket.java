package destiny.null_ouroboros.server.network;

import destiny.null_ouroboros.server.block.entity.CodelockSafeBlockEntity;
import destiny.null_ouroboros.server.menu.SafeWheelMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ServerboundSafeKeypadPacket {
    private final BlockPos pos;
    private final int keyId;

    public ServerboundSafeKeypadPacket(BlockPos pos, int keyId) {
        this.pos = pos;
        this.keyId = keyId;
    }

    public static void encode(ServerboundSafeKeypadPacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
        buf.writeVarInt(msg.keyId);
    }

    public static ServerboundSafeKeypadPacket decode(FriendlyByteBuf buf) {
        return new ServerboundSafeKeypadPacket(buf.readBlockPos(), buf.readVarInt());
    }

    public static boolean handle(ServerboundSafeKeypadPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) {
                return;
            }
            Level level = player.level();
            if (!(level.getBlockEntity(msg.pos) instanceof CodelockSafeBlockEntity safe)) {
                return;
            }
            if (!(player.containerMenu instanceof SafeWheelMenu)) {
                return;
            }
            if (player.distanceToSqr(msg.pos.getX() + 0.5, msg.pos.getY() + 0.5, msg.pos.getZ() + 0.5) > 64.0) {
                return;
            }
            safe.handleKey(msg.keyId);
        });
        return true;
    }
}
