package destiny.null_ouroboros.server.terminal.command;

import destiny.null_ouroboros.server.terminal.TerminalCommand;
import destiny.null_ouroboros.server.terminal.filesystem.TerminusDirectory;
import destiny.null_ouroboros.server.terminal.filesystem.TerminusFileSystem;
import destiny.null_ouroboros.server.terminal.filesystem.TerminusNode;
import net.minecraft.core.BlockPos;

public class CommandLs extends TerminalCommand {
    private final String args;

    public CommandLs(TerminusFileSystem fs, BlockPos pos, net.minecraft.world.level.Level level, String args) {
        super(fs, pos, level);
        this.args = args.trim();
    }

    @Override
    public void execute() {
        String targetPath = args.isEmpty() ? "." : args;
        TerminusNode target = fs.resolvePath(targetPath);
        if (target == null) {
            printlnTranslatable("message.null_ouroboros.terminus.ls.path_not_found");
            return;
        }
        if (!(target instanceof TerminusDirectory dir)) {
            printlnTranslatable("message.null_ouroboros.terminus.ls.not_directory");
            return;
        }
        if (dir.getChildren().isEmpty()) {
            printlnTranslatable("message.null_ouroboros.terminus.ls.directory_empty");
            return;
        }
        for (TerminusNode child : dir.getChildren().values()) {
            String type = child.isDirectory() ? "<DIR>" : "<FILE>";
            println(String.format("%-6s  %s", type, child.getName()));
        }
    }
}
