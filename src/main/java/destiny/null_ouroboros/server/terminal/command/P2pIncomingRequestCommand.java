package destiny.null_ouroboros.server.terminal.command;

import destiny.null_ouroboros.server.terminal.TerminalCommand;
import destiny.null_ouroboros.server.terminal.filesystem.TerminusFileSystem;
import destiny.null_ouroboros.server.terminal.p2p.P2pConnectionManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

public class P2pIncomingRequestCommand extends TerminalCommand {
    private final String requester;
    private final String target;

    public P2pIncomingRequestCommand(TerminusFileSystem fs, BlockPos pos, @Nullable Level level, String requester, String target) {
        super(fs, pos, level);
        this.requester = requester;
        this.target = target;
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
            P2pConnectionManager.acceptRequest(serverLevel, target, requester);
            setDone();
            return true;
        }
        if (value.equals("n")) {
            P2pConnectionManager.rejectRequest(serverLevel, target, requester);
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
