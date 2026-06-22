package destiny.null_ouroboros.server.terminal.command;

import destiny.null_ouroboros.server.terminal.filesystem.*;
import destiny.null_ouroboros.server.terminal.TerminalCommand;
import net.minecraft.core.BlockPos;

public class CommandRm extends TerminalCommand {
    private String targetPath;
    private State state = State.INIT;

    private enum State { INIT, WAITING_FOR_CONFIRM }

    public CommandRm(TerminusFileSystem fs, BlockPos pos, String args) {
        super(fs, pos);
        this.targetPath = args.trim();
    }

    @Override
    public void execute() {
        if (targetPath.isEmpty()) {
            println("Usage: rm <target> [-c]");
            setDone();
            return;
        }

        boolean force = false;
        String path = targetPath;
        if (targetPath.endsWith(" -c")) {
            force = true;
            path = targetPath.substring(0, targetPath.length() - 3).trim();
        }

        if (path.isEmpty()) {
            println("No target specified.");
            setDone();
            return;
        }

        if (force) {
            performDeletion(path);
            setDone();
        } else {
            this.targetPath = path;
            state = State.WAITING_FOR_CONFIRM;
            println("Are you sure you want to delete '" + path + "'?");
            println("Type 'yes' to confirm, 'no' to cancel.");
        }
    }

    private void performDeletion(String path) {
        TerminusNode target = fs.resolvePath(path);
        if (target == null) {
            println("Path not found: " + path);
            return;
        }
        if (target == fs.getRoot()) {
            println("Cannot delete the root directory.");
            return;
        }

        TerminusDirectory currentDir = fs.getCurrentDirectory();
        boolean isCurrent = (target == currentDir);

        if (target.isDirectory() && !isCurrent && currentDir != null) {
            if (isAncestorOf(target, currentDir)) {
                println("Cannot delete a directory that contains the current directory.");
                return;
            }
        }

        TerminusDirectory parent = target.getParent();
        try {
            fs.delete(path, true, true);
            println("Deleted " + path);

            if (isCurrent) {
                if (parent != null) {
                    fs.setCurrentDirectory(parent);
                } else {
                    fs.setCurrentDirectory(fs.getRoot());
                }
            }
        } catch (FileSystemException e) {
            println("Error: " + e.getMessage());
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
    public void handleInput(String input) {
        if (state == State.WAITING_FOR_CONFIRM) {
            input = input.trim().toLowerCase();
            if (input.equals("yes")) {
                performDeletion(targetPath);
                setDone();
            } else if (input.equals("no")) {
                println("Deletion cancelled.");
                setDone();
            } else {
                println("Please type 'yes' or 'no'.");
            }
        } else {
            setDone();
        }
    }

    @Override
    public void cancel() {
        println("Deletion cancelled.");
        setDone();
    }
}