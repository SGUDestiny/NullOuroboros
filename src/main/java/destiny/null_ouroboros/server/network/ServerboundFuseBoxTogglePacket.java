package destiny.null_ouroboros.server.network;

import destiny.null_ouroboros.server.block.entity.FuseBoxBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ServerboundFuseBoxTogglePacket {
    private final BlockPos pos;
    private final int slot;

    public ServerboundFuseBoxTogglePacket(BlockPos pos, int slot) {
        this.pos = pos;
        this.slot = slot;
    }

    public static void encode(ServerboundFuseBoxTogglePacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
        buf.writeVarInt(msg.slot);
    }

    public static ServerboundFuseBoxTogglePacket decode(FriendlyByteBuf buf) {
        return new ServerboundFuseBoxTogglePacket(buf.readBlockPos(), buf.readVarInt());
    }

    public static boolean handle(ServerboundFuseBoxTogglePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) {
                return;
            }
            Level level = player.level();
            if (!(level.getBlockEntity(msg.pos) instanceof FuseBoxBlockEntity box)) {
                return;
            }
            if (player.containerMenu == null) {
                return;
            }
            if (player.distanceToSqr(msg.pos.getX() + 0.5, msg.pos.getY() + 0.5, msg.pos.getZ() + 0.5) > 64.0) {
                return;
            }
            box.toggleSwitch(msg.slot);
        });
        return true;
    }
}
