package destiny.null_ouroboros.server.terminal.command;

import destiny.null_ouroboros.server.terminal.TerminalArgumentParser;
import destiny.null_ouroboros.server.terminal.TerminalCommand;
import destiny.null_ouroboros.server.terminal.filesystem.FileSystemException;
import destiny.null_ouroboros.server.terminal.filesystem.TerminusFileSystem;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

public class CommandDp extends TerminalCommand {
    private final String args;

    public CommandDp(TerminusFileSystem fs, BlockPos pos, @Nullable Level level, String args) {
        super(fs, pos, level);
        this.args = args.trim();
    }

    @Override
    public void execute() {
        List<String> parsed = TerminalArgumentParser.parse(args);
        if (parsed.isEmpty() || parsed.size() > 2) {
            printlnTranslatable("message.null_ouroboros.terminus.dp.usage");
            setDone();
            return;
        }
        try {
            fs.duplicate(parsed.get(0), parsed.size() == 2 ? parsed.get(1) : null);
        } catch (FileSystemException e) {
            printlnError(e);
        }
        setDone();
    }
}
