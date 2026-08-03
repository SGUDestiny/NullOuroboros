package destiny.null_ouroboros.server.network;

import destiny.null_ouroboros.server.block.entity.SafeBlockEntity;
import destiny.null_ouroboros.server.menu.SafeWheelMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ServerboundSafeWheelRotatePacket {
    private final BlockPos pos;
    private final float deltaDegrees;

    public ServerboundSafeWheelRotatePacket(BlockPos pos, float deltaDegrees) {
        this.pos = pos;
        this.deltaDegrees = deltaDegrees;
    }

    public static void encode(ServerboundSafeWheelRotatePacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
        buf.writeFloat(msg.deltaDegrees);
    }

    public static ServerboundSafeWheelRotatePacket decode(FriendlyByteBuf buf) {
        return new ServerboundSafeWheelRotatePacket(buf.readBlockPos(), buf.readFloat());
    }

    public static boolean handle(ServerboundSafeWheelRotatePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) {
                return;
            }
            Level level = player.level();
            if (!(level.getBlockEntity(msg.pos) instanceof SafeBlockEntity safe)) {
                return;
            }
            if (!(player.containerMenu instanceof SafeWheelMenu)) {
                return;
            }
            if (player.distanceToSqr(msg.pos.getX() + 0.5, msg.pos.getY() + 0.5, msg.pos.getZ() + 0.5) > 64.0) {
                return;
            }
            float clamped = Math.max(-45f, Math.min(45f, msg.deltaDegrees));
            safe.applyWheelDelta(clamped, player);
        });
        return true;
    }
}
