package destiny.null_ouroboros.server.terminal.command;

import destiny.null_ouroboros.server.terminal.FileSessionMode;
import destiny.null_ouroboros.server.terminal.FileSessionRequest;
import destiny.null_ouroboros.server.terminal.filesystem.*;
import destiny.null_ouroboros.server.terminal.TerminalCommand;
import net.minecraft.core.BlockPos;

import javax.annotation.Nullable;

public class CommandCr extends TerminalCommand {
    private final String args;
    @Nullable
    private FileSessionRequest sessionRequest = null;

    public CommandCr(TerminusFileSystem fs, BlockPos pos, net.minecraft.world.level.Level level, String args) {
        super(fs, pos, level);
        this.args = args.trim();
    }

    @Override
    public void execute() {
        if (args.isEmpty()) {
            printlnTranslatable("message.null_ouroboros.terminus.cr.usage");
            setDone();
            return;
        }

        boolean isFile = TerminusFileSystem.hasExtension(args);
        try {
            if (isFile) {
                if (!TerminusFileSystem.isTextFileName(args)) {
                    printlnTranslatable("message.null_ouroboros.terminus.cr.invalid_format");
                    setDone();
                    return;
                }
                fs.createTextFile(args, "");
                TerminusNode node = fs.resolvePath(args);
                if (node instanceof TerminusTextFile file) {
                    sessionRequest = new FileSessionRequest(file, FileSessionMode.EDIT);
                }
            } else {
                fs.createDirectory(args);

                String fullPath = fs.resolveToAbsolutePath(args);
                TerminusNode node = fs.resolvePath(fullPath);
                if (node instanceof TerminusDirectory dir) {
                    fs.setCurrentDirectory(dir);
                } else {
                    printlnTranslatable("message.null_ouroboros.terminus.cr.navigate_failed");
                }
            }
        } catch (FileSystemException e) {
            printlnError(e);
        }
        setDone();
    }

    @Override
    @Nullable
    public FileSessionRequest getFileSessionRequest() {
        return sessionRequest;
    }
}
