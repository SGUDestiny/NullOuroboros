package destiny.null_ouroboros.server.terminal.command;

import destiny.null_ouroboros.server.terminal.filesystem.*;
import destiny.null_ouroboros.server.terminal.TerminalCommand;
import net.minecraft.core.BlockPos;

public class CommandMv extends TerminalCommand {
    private final String args;

    public CommandMv(TerminusFileSystem fs, BlockPos pos, String args) {
        super(fs, pos);
        this.args = args.trim();
    }

    @Override
    public void execute() {
        if (args.isEmpty()) {
            println("Usage: mv <source> <destination directory>");
            return;
        }
        String[] parts = args.split("\\s+", 2);
        if (parts.length < 2) {
            println("Missing destination.");
            return;
        }
        String sourcePath = parts[0];
        String destDirPath = parts[1];
        try {
            fs.move(sourcePath, destDirPath);
            println("Moved " + sourcePath + " to " + destDirPath);
        } catch (FileSystemException e) {
            println("Error: " + e.getMessage());
        }
    }
}