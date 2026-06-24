package destiny.null_ouroboros.server.terminal.command;

import destiny.null_ouroboros.server.terminal.FileSessionMode;
import destiny.null_ouroboros.server.terminal.FileSessionRequest;
import destiny.null_ouroboros.server.terminal.filesystem.*;
import destiny.null_ouroboros.server.terminal.TerminalCommand;
import net.minecraft.core.BlockPos;

import javax.annotation.Nullable;

public class CommandEd extends TerminalCommand {
    private final String args;
    @Nullable
    private FileSessionRequest sessionRequest = null;

    public CommandEd(TerminusFileSystem fs, BlockPos pos, net.minecraft.world.level.Level level, String args) {
        super(fs, pos, level);
        this.args = args.trim();
    }

    @Override
    public void execute() {
        if (args.isEmpty()) {
            printlnTranslatable("message.null_ouroboros.terminus.ed.usage");
            setDone();
            return;
        }

        if (!TerminusFileSystem.hasExtension(args)) {
            printlnTranslatable("message.null_ouroboros.terminus.ed.not_a_file");
            setDone();
            return;
        }

        if (!TerminusFileSystem.isTextFileName(args)) {
            printlnTranslatable("message.null_ouroboros.terminus.ed.invalid_format");
            setDone();
            return;
        }

        TerminusNode target = fs.resolvePath(args);
        if (target == null) {
            printlnTranslatable("message.null_ouroboros.terminus.ed.path_not_found", args);
            setDone();
            return;
        }

        if (target instanceof TerminusTextFile file) {
            sessionRequest = new FileSessionRequest(file, FileSessionMode.EDIT);
        } else {
            printlnTranslatable("message.null_ouroboros.terminus.ed.not_a_text_file");
        }
        setDone();
    }

    @Override
    @Nullable
    public FileSessionRequest getFileSessionRequest() {
        return sessionRequest;
    }
}
