package destiny.null_ouroboros.server.terminal.command;

import destiny.null_ouroboros.server.terminal.filesystem.*;
import destiny.null_ouroboros.server.terminal.TerminalCommand;
import net.minecraft.core.BlockPos;

public class CommandRn extends TerminalCommand {
    private final String args;

    public CommandRn(TerminusFileSystem fs, BlockPos pos, String args) {
        super(fs, pos);
        this.args = args.trim();
    }

    @Override
    public void execute() {
        if (args.isEmpty()) {
            println("Usage: rn <target> <newName>");
            return;
        }
        String[] parts = args.split("\\s+", 2);
        if (parts.length < 2) {
            println("Missing new name.");
            return;
        }
        String targetPath = parts[0];
        String newName = parts[1];
        try {
            fs.rename(targetPath, newName);
            println("Renamed to " + newName);
        } catch (FileSystemException e) {
            println("Error: " + e.getMessage());
        }
    }
}