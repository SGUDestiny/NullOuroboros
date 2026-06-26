package destiny.null_ouroboros.server.terminal.command;

import destiny.null_ouroboros.server.block.entity.DustyComputerBlockEntity;
import destiny.null_ouroboros.server.terminal.TerminalCommand;
import destiny.null_ouroboros.server.terminal.filesystem.TerminusFileSystem;
import destiny.null_ouroboros.server.terminal.filesystem.TerminusSavedData;
import destiny.null_ouroboros.server.terminal.p2p.P2pConnectionManager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import javax.annotation.Nullable;

public class CommandIpvinf extends TerminalCommand {
    private final String args;

    public CommandIpvinf(TerminusFileSystem fs, BlockPos pos, @Nullable Level level, String args) {
        super(fs, pos, level);
        this.args = args.trim();
    }

    @Override
    public void execute() {
        if (level == null) {
            printlnTranslatable("message.null_ouroboros.terminus.internal_error");
            setDone();
            return;
        }
        BlockEntity blockEntity = level.getBlockEntity(computerPos);
        if (!(blockEntity instanceof DustyComputerBlockEntity computer) || computer.getIpvInf() == null) {
            printlnTranslatable("message.null_ouroboros.terminus.internal_error");
            setDone();
            return;
        }
        TerminusSavedData data = TerminusSavedData.get(level);
        if (data == null) {
            printlnTranslatable("message.null_ouroboros.terminus.internal_error");
            setDone();
            return;
        }

        String ipvInf = computer.getIpvInf();
        if (args.equalsIgnoreCase("regenerate")) {
            P2pConnectionManager.disconnect(ipvInf, P2pConnectionManager.DisconnectCause.LOST);
            String newIpvInf = data.reassignIpvInf(ipvInf);
            computer.setIpvInf(newIpvInf);
            printlnTranslatable("message.null_ouroboros.terminus.ipvinf_regenerate", newIpvInf);
            setDone();
            return;
        }
        if (args.equalsIgnoreCase("-c")) {
            printIpvInf(ipvInf);
            copyToClipboard(ipvInf);
            printlnTranslatable("message.null_ouroboros.terminus.ipvinf.copied");
            setDone();
            return;
        }
        if (!args.isEmpty()) {
            printlnTranslatable("message.null_ouroboros.terminus.ipvinf.usage");
            setDone();
            return;
        }
        printIpvInf(ipvInf);
        setDone();
    }

    private void printIpvInf(String ipvInf) {
        println(ipvInf);
    }
}
