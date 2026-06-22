package destiny.null_ouroboros.server.terminal.command;

import destiny.null_ouroboros.server.terminal.filesystem.*;
import destiny.null_ouroboros.server.terminal.TerminalCommand;
import net.minecraft.core.BlockPos;

public class CommandCt extends TerminalCommand {
    private final String args;

    public CommandCt(TerminusFileSystem fs, BlockPos pos, String args) {
        super(fs, pos);
        this.args = args.trim();
    }

    @Override
    public void execute() {
        if (args.isEmpty()) {
            println("Usage: ct <name>");
            setDone();
            return;
        }

        boolean isFile = args.contains(".");
        try {
            if (isFile) {
                fs.createTextFile(args, "");
                println("File created: " + args);
            } else {
                fs.createDirectory(args);

                String fullPath = fs.resolveToAbsolutePath(args);
                TerminusNode node = fs.resolvePath(fullPath);
                if (node instanceof TerminusDirectory dir) {
                    fs.setCurrentDirectory(dir);
                    println("Created and entered directory " + args);
                } else {
                    println("Directory created, but could not navigate to it.");
                }
            }
        } catch (FileSystemException e) {
            println("Error: " + e.getMessage());
        }
        setDone();
    }
}