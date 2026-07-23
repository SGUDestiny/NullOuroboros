package destiny.null_ouroboros.server.network;

import destiny.null_ouroboros.server.block.entity.DustyComputerBlockEntity;
import destiny.null_ouroboros.server.registry.SoundRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ServerBoundDustyComputerCommandPacket {
    private final BlockPos pos;
    private final Action action;
    private final String command;

    public enum Action {
        TYPE,
        ERASE,
        ENTER
    }

    public ServerBoundDustyComputerCommandPacket(BlockPos pos, Action action) {
        this.pos = pos;
        this.action = action;
        this.command = "";
    }

    public ServerBoundDustyComputerCommandPacket(BlockPos pos, String command) {
        this.pos = pos;
        this.action = Action.ENTER;
        this.command = command;
    }

    public static void encode(ServerBoundDustyComputerCommandPacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
        buf.writeEnum(msg.action);
        if (msg.action == Action.ENTER) {
            buf.writeUtf(msg.command);
        }
    }

    public static ServerBoundDustyComputerCommandPacket decode(FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        Action action = buf.readEnum(Action.class);
        if (action == Action.ENTER) {
            return new ServerBoundDustyComputerCommandPacket(pos, buf.readUtf());
        }
        return new ServerBoundDustyComputerCommandPacket(pos, action);
    }

    public static boolean handle(ServerBoundDustyComputerCommandPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();

            if (player == null) return;

            Level level = player.level();
            if (!(level.getBlockEntity(msg.pos) instanceof DustyComputerBlockEntity computer)) {
                return;
            }
            if (!computer.canPlayerInteract(player)) {
                return;
            }

            switch (msg.action) {
                case TYPE:
                    level.playSound(null, msg.pos, SoundRegistry.DUSTY_COMPUTER_TYPE.get(), SoundSource.BLOCKS, 0.4f, 1f);
                    break;
                case ERASE:
                    level.playSound(null, msg.pos, SoundRegistry.DUSTY_COMPUTER_ERASE.get(), SoundSource.BLOCKS, 0.6f, 1f);
                    break;
                case ENTER:
                    computer.processCommand(msg.command, player);
                    level.playSound(null, msg.pos, SoundRegistry.DUSTY_COMPUTER_ENTER.get(), SoundSource.BLOCKS, 0.6f, 1f);
                    break;
            }
        });
        return true;
    }
}
