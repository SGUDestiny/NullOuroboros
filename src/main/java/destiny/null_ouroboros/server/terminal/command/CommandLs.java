package destiny.null_ouroboros.server.terminal.command;

import destiny.null_ouroboros.server.terminal.TerminalCommand;
import destiny.null_ouroboros.server.terminal.filesystem.TerminusDirectory;
import destiny.null_ouroboros.server.terminal.filesystem.TerminusFileSystem;
import destiny.null_ouroboros.server.terminal.filesystem.TerminusNode;
import net.minecraft.core.BlockPos;

public class CommandLs extends TerminalCommand {
    private final String args;

    public CommandLs(TerminusFileSystem fs, BlockPos pos, String args) {
        super(fs, pos);
        this.args = args.trim();
    }

    @Override
    public void execute() {
        String targetPath = args.isEmpty() ? "." : args;
        TerminusNode target = fs.resolvePath(targetPath);
        if (target == null) {
            println("Path not found.");
            return;
        }
        if (!(target instanceof TerminusDirectory dir)) {
            println("Not a directory.");
            return;
        }
        println("Listing of " + fs.getAbsolutePath(dir) + ":");
        for (TerminusNode child : dir.getChildren().values()) {
            String type = child.isDirectory() ? "<DIR>" : "<FILE>";
            println(type + "  " + child.getName());
        }
    }
}