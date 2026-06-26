package destiny.null_ouroboros.server.terminal.command;

import destiny.null_ouroboros.server.terminal.TerminalCommand;
import destiny.null_ouroboros.server.terminal.filesystem.TerminusFileSystem;
import destiny.null_ouroboros.server.terminal.p2p.P2pConnectionManager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

public class P2pTransferLoadingCommand extends TerminalCommand {
    private final String computer;

    public P2pTransferLoadingCommand(TerminusFileSystem fs, BlockPos pos, @Nullable Level level, String computer) {
        super(fs, pos, level);
        this.computer = computer;
        awaitInput();
    }

    @Override
    public void execute() {}

    @Override
    public boolean handleInput(String input) {
        return false;
    }

    @Override
    public void cancel() {
        P2pConnectionManager.cancelTransfer(computer);
        setDone();
    }
}
