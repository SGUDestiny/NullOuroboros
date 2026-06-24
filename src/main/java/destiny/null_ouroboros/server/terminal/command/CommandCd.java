package destiny.null_ouroboros.server.terminal.command;

import destiny.null_ouroboros.server.terminal.filesystem.*;
import destiny.null_ouroboros.server.terminal.FileSessionMode;
import destiny.null_ouroboros.server.terminal.FileSessionRequest;
import destiny.null_ouroboros.server.terminal.TerminalCommand;
import net.minecraft.core.BlockPos;

import javax.annotation.Nullable;

public class CommandCd extends TerminalCommand {
    private final String args;
    @Nullable
    private FileSessionRequest sessionRequest = null;

    public CommandCd(TerminusFileSystem fs, BlockPos pos, net.minecraft.world.level.Level level, String args) {
        super(fs, pos, level);
        this.args = args.trim();
    }

    @Override
    public void execute() {
        if (fs.getCurrentDirectory() == null) {
            fs.setCurrentDirectory(fs.getRoot());
        }

        if (args.isEmpty()) {
            fs.setCurrentDirectory(fs.getRoot());
            setDone();
            return;
        }

        if (args.equals("..")) {
            TerminusDirectory current = fs.getCurrentDirectory();
            if (current != null && current.getParent() != null) {
                fs.setCurrentDirectory(current.getParent());
            } else {
                fs.setCurrentDirectory(fs.getRoot());
            }
            setDone();
            return;
        }

        TerminusNode target = fs.resolvePath(args);
        if (target == null) {
            printlnTranslatable("message.null_ouroboros.terminus.cd.path_not_found", args);
            setDone();
            return;
        }

        if (target.isDirectory()) {
            fs.setCurrentDirectory(target);
        } else if (target instanceof TerminusTextFile file) {
            sessionRequest = new FileSessionRequest(file, FileSessionMode.VIEW);
        } else {
            printlnTranslatable("message.null_ouroboros.terminus.cd.cannot_open_file_type");
        }
        setDone();
    }

    @Override
    @Nullable
    public FileSessionRequest getFileSessionRequest() {
        return sessionRequest;
    }
}
