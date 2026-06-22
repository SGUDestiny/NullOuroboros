package destiny.null_ouroboros.server.terminal;

import destiny.null_ouroboros.server.terminal.filesystem.FileSystemException;
import destiny.null_ouroboros.server.terminal.filesystem.TerminusFileSystem;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public abstract class TerminalCommand {
    protected final TerminusFileSystem fs;
    protected final BlockPos computerPos;
    protected final List<Component> output = new ArrayList<>();
    private boolean done = true;

    public TerminalCommand(TerminusFileSystem fs, BlockPos computerPos) {
        this.fs = fs;
        this.computerPos = computerPos;
    }

    public abstract void execute();

    public boolean tick() { return isDone(); }

    public boolean handleInput(String input) { return true; }

    public void setDone() { this.done = true; }

    protected void awaitInput() { this.done = false; }

    public boolean isDone() { return done; }

    public List<Component> getOutput() { return output; }
    public void clearOutput() { output.clear(); }

    protected void println(String text) {
        output.add(Component.literal(text));
    }

    protected void printlnTranslatable(String key) {
        output.add(Component.translatable(key));
    }

    protected void printlnTranslatable(String key, Object... args) {
        output.add(Component.translatable(key, args));
    }

    protected void printlnError(FileSystemException e) {
        printlnTranslatable("message.null_ouroboros.terminus.error", e.getMessage());
    }

    @Nullable
    public FileSessionRequest getFileSessionRequest() {
        return null;
    }

    public boolean requestsShutdown() {
        return false;
    }

    public boolean requestsClear() {
        return false;
    }

    public void cancel() {}
}