package destiny.null_ouroboros.server.terminal;

import destiny.null_ouroboros.server.terminal.filesystem.FileSystemException;
import destiny.null_ouroboros.server.terminal.filesystem.TerminusFileSystem;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public abstract class TerminalCommand {
    protected final TerminusFileSystem fs;
    protected final BlockPos computerPos;
    @Nullable
    protected final Level level;
    protected final List<Component> output = new ArrayList<>();
    private boolean done = true;
    @Nullable
    private String clipboardText;

    public TerminalCommand(TerminusFileSystem fs, BlockPos computerPos, @Nullable Level level) {
        this.fs = fs;
        this.computerPos = computerPos;
        this.level = level;
    }

    public abstract void execute();

    public boolean tick() { return isDone(); }

    public boolean handleInput(String input) { return true; }

    public boolean echoesInput() { return true; }

    public boolean echoesInput(String input) { return echoesInput(); }

    public boolean updatesLiveDisplay() { return false; }

    public void updateLiveDisplay(TerminusSession session) {}

    public void prepareSessionOutput(TerminusSession session) {}

    public void setDone() { this.done = true; }

    protected void awaitInput() { this.done = false; }

    public boolean isDone() { return done; }

    public List<Component> getOutput() { return output; }
    public void clearOutput() { output.clear(); }

    protected void copyToClipboard(String text) {
        this.clipboardText = text;
    }

    @Nullable
    public String takeClipboardText() {
        String text = clipboardText;
        clipboardText = null;
        return text;
    }

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