package destiny.null_ouroboros.server.terminal.command;

import destiny.null_ouroboros.server.terminal.TerminalCommand;
import destiny.null_ouroboros.server.terminal.filesystem.TerminusFileSystem;
import destiny.null_ouroboros.server.terminal.p2p.P2pConnectionManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

public class P2pIncomingTransferCommand extends TerminalCommand {
    private final String sender;
    private final String receiver;
    private final String sourcePath;
    private final String mode;

    public P2pIncomingTransferCommand(TerminusFileSystem fs, BlockPos pos, @Nullable Level level,
                                      String sender, String receiver, String sourcePath, String mode) {
        super(fs, pos, level);
        this.sender = sender;
        this.receiver = receiver;
        this.sourcePath = sourcePath;
        this.mode = mode;
        awaitInput();
    }

    @Override
    public void execute() {}

    @Override
    public boolean echoesInput(String input) {
        return !input.trim().equalsIgnoreCase("y");
    }

    @Override
    public boolean handleInput(String input) {
        String value = input.trim().toLowerCase();
        if (!(level instanceof ServerLevel serverLevel)) {
            setDone();
            return true;
        }
        if (value.equals("y")) {
            P2pConnectionManager.acceptTransfer(serverLevel, sender, receiver, sourcePath, mode);
            setDone();
            return true;
        }
        if (value.equals("n")) {
            P2pConnectionManager.rejectTransfer(serverLevel, sender, receiver);
            setDone();
            return true;
        }
        return false;
    }

    @Override
    public void cancel() {
        setDone();
    }
}
