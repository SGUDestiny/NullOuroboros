package destiny.null_ouroboros.server.terminal.command;

import destiny.null_ouroboros.server.terminal.filesystem.*;
import destiny.null_ouroboros.server.terminal.TerminalCommand;
import net.minecraft.core.BlockPos;

public class CommandMv extends TerminalCommand {
    private final String args;

    public CommandMv(TerminusFileSystem fs, BlockPos pos, net.minecraft.world.level.Level level, String args) {
        super(fs, pos, level);
        this.args = args.trim();
    }

    @Override
    public void execute() {
        if (args.isEmpty()) {
            printlnTranslatable("message.null_ouroboros.terminus.mv.usage");
            return;
        }
        String[] parts = args.split("\\s+", 2);
        if (parts.length < 2) {
            printlnTranslatable("message.null_ouroboros.terminus.mv.missing_destination");
            return;
        }
        String sourcePath = parts[0];
        String destDirPath = parts[1];
        try {
            fs.move(sourcePath, destDirPath);
        } catch (FileSystemException e) {
            printlnError(e);
        }
    }
}
