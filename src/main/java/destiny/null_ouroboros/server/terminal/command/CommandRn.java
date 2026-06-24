package destiny.null_ouroboros.server.terminal.command;

import destiny.null_ouroboros.server.terminal.filesystem.*;
import destiny.null_ouroboros.server.terminal.TerminalCommand;
import net.minecraft.core.BlockPos;

public class CommandRn extends TerminalCommand {
    private final String args;

    public CommandRn(TerminusFileSystem fs, BlockPos pos, net.minecraft.world.level.Level level, String args) {
        super(fs, pos, level);
        this.args = args.trim();
    }

    @Override
    public void execute() {
        if (args.isEmpty()) {
            printlnTranslatable("message.null_ouroboros.terminus.rn.usage");
            return;
        }
        String[] parts = args.split("\\s+", 2);
        if (parts.length < 2) {
            printlnTranslatable("message.null_ouroboros.terminus.rn.missing_name");
            return;
        }
        String targetPath = parts[0];
        String newName = parts[1];
        try {
            fs.rename(targetPath, newName);
        } catch (FileSystemException e) {
            printlnError(e);
        }
    }
}
