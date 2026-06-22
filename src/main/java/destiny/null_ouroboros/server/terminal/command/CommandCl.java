package destiny.null_ouroboros.server.terminal.command;

import destiny.null_ouroboros.server.terminal.TerminalCommand;
import destiny.null_ouroboros.server.terminal.filesystem.TerminusFileSystem;
import net.minecraft.core.BlockPos;

public class CommandCl extends TerminalCommand {
    private final String args;

    public CommandCl(TerminusFileSystem fs, BlockPos pos, String args) {
        super(fs, pos);
        this.args = args.trim();
    }

    @Override
    public void execute() {
        if (!args.isEmpty()) {
            printlnTranslatable("message.null_ouroboros.terminus.cl.usage");
            setDone();
            return;
        }

        setDone();
    }

    @Override
    public boolean requestsClear() {
        return args.isEmpty();
    }
}
