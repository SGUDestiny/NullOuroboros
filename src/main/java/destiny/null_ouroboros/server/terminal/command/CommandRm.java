package destiny.null_ouroboros.server.terminal.command;

import destiny.null_ouroboros.server.terminal.filesystem.*;
import destiny.null_ouroboros.server.terminal.TerminalCommand;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.List;

public class CommandRm extends TerminalCommand {
    private String targetPath;
    private State state = State.INIT;

    private enum State { INIT, WAITING_FOR_CONFIRM }

    public CommandRm(TerminusFileSystem fs, BlockPos pos, net.minecraft.world.level.Level level, String args) {
        super(fs, pos, level);
        this.targetPath = args.trim();
    }

    @Override
    public void execute() {
        if (targetPath.isEmpty()) {
            printlnTranslatable("message.null_ouroboros.terminus.rm.usage");
            setDone();
            return;
        }

        ParsedArgs parsed = parseArgs(targetPath);
        if (parsed.path.isEmpty()) {
            printlnTranslatable("message.null_ouroboros.terminus.rm.no_target");
            setDone();
            return;
        }

        if (parsed.force) {
            performDeletion(parsed.path);
            setDone();
        } else {
            this.targetPath = parsed.path;
            state = State.WAITING_FOR_CONFIRM;
            awaitInput();
            printlnTranslatable("message.null_ouroboros.terminus.rm.confirm", parsed.path);
            printlnTranslatable("message.null_ouroboros.terminus.rm.confirm_hint");
        }
    }

    private static ParsedArgs parseArgs(String rawArgs) {
        boolean force = false;
        List<String> parts = new ArrayList<>();
        for (String token : rawArgs.split("\\s+")) {
            if (token.equals("-c") || token.equals("--confirm")) {
                force = true;
            } else if (!token.isEmpty()) {
                parts.add(token);
            }
        }
        String path = String.join(" ", parts).trim();
        return new ParsedArgs(path, force);
    }

    private record ParsedArgs(String path, boolean force) {}

    private void performDeletion(String path) {
        TerminusNode target = fs.resolvePath(path);
        if (target == null) {
            printlnTranslatable("message.null_ouroboros.terminus.rm.path_not_found", path);
            return;
        }
        if (target == fs.getRoot()) {
            printlnTranslatable("message.null_ouroboros.terminus.rm.cannot_delete_root");
            return;
        }

        TerminusDirectory currentDir = fs.getCurrentDirectory();
        boolean isCurrent = (target == currentDir);

        if (target.isDirectory() && !isCurrent && currentDir != null) {
            if (isAncestorOf(target, currentDir)) {
                printlnTranslatable("message.null_ouroboros.terminus.rm.cannot_delete_ancestor");
                return;
            }
        }

        TerminusDirectory parent = target.getParent();
        try {
            fs.delete(path, true);

            if (isCurrent) {
                if (parent != null) {
                    fs.setCurrentDirectory(parent);
                } else {
                    fs.setCurrentDirectory(fs.getRoot());
                }
            }
        } catch (FileSystemException e) {
            printlnError(e);
        }
    }

    private boolean isAncestorOf(TerminusNode ancestor, TerminusNode descendant) {
        TerminusNode current = descendant;
        while (current != null) {
            if (current == ancestor) return true;
            current = current.getParent();
        }
        return false;
    }

    @Override
    public boolean handleInput(String input) {
        if (state != State.WAITING_FOR_CONFIRM) {
            setDone();
            return true;
        }

        input = input.trim().toLowerCase();
        if (input.equals("y")) {
            performDeletion(targetPath);
            setDone();
            return true;
        }
        if (input.equals("n")) {
            setDone();
            return true;
        }
        return false;
    }

    @Override
    public void cancel() {
        setDone();
    }
}
