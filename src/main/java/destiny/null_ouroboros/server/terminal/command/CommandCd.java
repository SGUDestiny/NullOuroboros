package destiny.null_ouroboros.server.terminal.command;

import destiny.null_ouroboros.server.terminal.filesystem.*;
import destiny.null_ouroboros.server.terminal.TerminalCommand;
import net.minecraft.core.BlockPos;

public class CommandCd extends TerminalCommand {
    private final String args;

    public CommandCd(TerminusFileSystem fs, BlockPos pos, String args) {
        super(fs, pos);
        this.args = args.trim();
    }

    @Override
    public void execute() {
        if (fs.getCurrentDirectory() == null) {
            fs.setCurrentDirectory(fs.getRoot());
        }

        if (args.isEmpty()) {
            fs.setCurrentDirectory(fs.getRoot());
            println("Changed to root.");
            setDone();
            return;
        }

        if (args.equals("..")) {
            TerminusDirectory current = fs.getCurrentDirectory();
            if (current != null && current.getParent() != null) {
                fs.setCurrentDirectory(current.getParent());
                println("Up one level.");
            } else {
                fs.setCurrentDirectory(fs.getRoot());
                println("Already at root.");
            }
            setDone();
            return;
        }

        TerminusNode target = fs.resolvePath(args);
        if (target == null) {
            println("Path not found: " + args);
            setDone();
            return;
        }

        if (target.isDirectory()) {
            fs.setCurrentDirectory(target);
            println("Changed to " + fs.getCurrentPath());
        } else {
            if (target instanceof TerminusTextFile file) {
                String content = file.getContent();
                if (content.isEmpty()) {
                    println("(empty file)");
                } else {
                    for (String line : content.split("\n")) {
                        println(line);
                    }
                }
            } else {
                println("Cannot open this file type.");
            }
        }
        setDone();
    }
}