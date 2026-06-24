package destiny.null_ouroboros.server.terminal.command;

import destiny.null_ouroboros.server.terminal.TerminalCommand;
import destiny.null_ouroboros.server.terminal.filesystem.TerminusFileSystem;
import destiny.null_ouroboros.server.terminal.filesystem.TerminusNode;
import net.minecraft.core.BlockPos;

import java.util.List;

public class CommandSr extends TerminalCommand {
    private final String args;

    public CommandSr(TerminusFileSystem fs, BlockPos pos, net.minecraft.world.level.Level level, String args) {
        super(fs, pos, level);
        this.args = args.trim();
    }

    @Override
    public void execute() {
        if (args.isEmpty()) {
            printlnTranslatable("message.null_ouroboros.terminus.sr.usage");
            setDone();
            return;
        }

        TerminusNode resolved = fs.resolvePath(args);
        if (resolved != null) {
            println(fs.getAbsolutePath(resolved));
            setDone();
            return;
        }

        String name = args;
        if (name.contains("\\")) {
            name = name.substring(name.lastIndexOf('\\') + 1);
        }
        if (name.isEmpty()) {
            printlnTranslatable("message.null_ouroboros.terminus.sr.usage");
            setDone();
            return;
        }

        List<String> matches = fs.searchByName(name);
        if (matches.isEmpty()) {
            printlnTranslatable("message.null_ouroboros.terminus.sr.not_found", args);
        } else {
            for (String path : matches) {
                println(path);
            }
        }
        setDone();
    }
}
