package destiny.null_ouroboros.server.terminal;

import destiny.null_ouroboros.server.terminal.filesystem.TerminusFileSystem;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public abstract class TerminalCommand {
    protected final TerminusFileSystem fs;
    protected final BlockPos computerPos;
    protected final List<Component> output = new ArrayList<>();
    private boolean done = false;

    public TerminalCommand(TerminusFileSystem fs, BlockPos computerPos) {
        this.fs = fs;
        this.computerPos = computerPos;
    }

    public abstract void execute();

    public boolean tick() { return isDone(); }

    public void handleInput(String input) {}

    public void setDone() { this.done = true; }

    public boolean isDone() { return done; }

    public List<Component> getOutput() { return output; }
    public void clearOutput() { output.clear(); }

    protected void println(String text) {
        output.add(Component.literal(text));
    }

    public void cancel() {}
}