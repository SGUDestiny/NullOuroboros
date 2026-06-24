package destiny.null_ouroboros.server.terminal;

import destiny.null_ouroboros.client.network.ClientBoundDustyComputerSyncPacket;
import destiny.null_ouroboros.server.registry.PacketHandlerRegistry;
import destiny.null_ouroboros.server.terminal.filesystem.TerminusDirectory;
import destiny.null_ouroboros.server.terminal.filesystem.TerminusFileSystem;
import destiny.null_ouroboros.server.terminal.filesystem.TerminusNode;
import destiny.null_ouroboros.server.terminal.filesystem.TerminusTextFile;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TerminusSession {
    public record FileSessionState(
            String filePath,
            String returnDirectoryPath,
            TerminusDirectory returnDirectory,
            FileSessionMode mode,
            List<String> savedTerminalLines
    ) {}

    private final UUID filesystemId;
    private BlockPos computerPos;
    private final List<String> lines = new ArrayList<>();
    private String currentPath = "T:\\";
    @Nullable
    private TerminalCommand activeCommand = null;
    @Nullable
    private UUID playerId = null;
    @Nullable
    private FileSessionState fileSession = null;
    private boolean shutdownRequested = false;

    public TerminusSession(UUID filesystemId, BlockPos computerPos) {
        this.filesystemId = filesystemId;
        this.computerPos = computerPos;
    }

    public void setComputerPos(BlockPos computerPos) {
        this.computerPos = computerPos;
    }

    public List<String> getLines() { return lines; }
    public String getCurrentPath() { return currentPath; }
    public void setCurrentPath(String path) { this.currentPath = path; }
    @Nullable public UUID getPlayerId() { return playerId; }
    public void setPlayerId(@Nullable UUID playerId) { this.playerId = playerId; }
    @Nullable public TerminalCommand getActiveCommand() { return activeCommand; }
    public void clearActiveCommand() { activeCommand = null; }
    @Nullable public FileSessionState getFileSession() { return fileSession; }
    public boolean isInFileSession() { return fileSession != null; }

    public void addLine(String line) { lines.add(line); }
    public void clearLines() { lines.clear(); }

    public void replaceLine(int index, String text) {
        if (index >= 0 && index < lines.size()) {
            lines.set(index, text);
        }
    }

    public void removeLines(int start, int count) {
        if (start < 0 || count <= 0 || start >= lines.size()) {
            return;
        }
        int end = Math.min(start + count, lines.size());
        lines.subList(start, end).clear();
    }

    public boolean tickActiveCommand(TerminusFileSystem fs) {
        if (activeCommand == null) {
            return false;
        }

        boolean changed = false;

        if (activeCommand.updatesLiveDisplay()) {
            activeCommand.updateLiveDisplay(this);
            changed = true;
        }

        boolean done = activeCommand.tick();

        if (done) {
            activeCommand.prepareSessionOutput(this);
            appendCommandOutput(activeCommand);
            clearActiveCommand();
            changed = true;
        }

        this.currentPath = fs.getCurrentPath();
        return changed;
    }

    public boolean consumeShutdownRequest() {
        boolean requested = shutdownRequested;
        shutdownRequested = false;
        return requested;
    }

    public void clearToBootState(TerminusFileSystem fs) {
        if (activeCommand != null) {
            activeCommand.cancel();
            clearActiveCommand();
        }
        cancelFileSession(fs);
        clearLines();
        fs.setCurrentDirectory(fs.getRoot());
        syncFromFileSystem(fs);
    }

    public void syncFromFileSystem(TerminusFileSystem fs) {
        this.currentPath = fs.getCurrentPath();
    }

    public void beginFileSession(TerminusTextFile file, TerminusFileSystem fs, FileSessionMode mode) {
        List<String> saved = new ArrayList<>(lines);
        lines.clear();
        this.fileSession = new FileSessionState(
                fs.getAbsolutePath(file),
                fs.getCurrentPath(),
                fs.getCurrentDirectory(),
                mode,
                saved
        );
    }

    public void closeFileSession(TerminusFileSystem fs, @Nullable String contentToSave) {
        if (fileSession == null) return;

        if (fileSession.mode() == FileSessionMode.EDIT && contentToSave != null) {
            TerminusNode node = fs.resolvePath(fileSession.filePath());
            if (node instanceof TerminusTextFile textFile) {
                textFile.setContent(contentToSave);
                fs.markDirtyForEdit();
            }
        }

        fs.setCurrentDirectory(fileSession.returnDirectory());
        this.currentPath = fs.getCurrentPath();

        lines.clear();
        lines.addAll(fileSession.savedTerminalLines());
        this.fileSession = null;
    }

    public void cancelFileSession(TerminusFileSystem fs) {
        if (fileSession == null) return;

        fs.setCurrentDirectory(fileSession.returnDirectory());
        this.currentPath = fs.getCurrentPath();

        lines.clear();
        lines.addAll(fileSession.savedTerminalLines());
        this.fileSession = null;
    }

    public void processCommand(String rawLine, TerminusFileSystem fs, Level level) {
        if (fileSession != null) {
            return;
        }

        String trimmed = rawLine.trim();

        if (activeCommand != null && activeCommand.isDone()) {
            activeCommand = null;
        }

        if (activeCommand != null) {
            if (trimmed.equalsIgnoreCase("cancel")) {
                try {
                    activeCommand.cancel();
                } catch (Exception ignored) {}
                appendCommandOutput(activeCommand);
                activeCommand = null;
            } else {
                boolean accepted;
                try {
                    accepted = activeCommand.handleInput(trimmed);
                } catch (Exception e) {
                    addLine(Component.translatable("message.null_ouroboros.terminus.processing_error").getString());
                    activeCommand = null;
                    accepted = false;
                }
                if (accepted) {
                    addLine("> " + trimmed);
                    if (activeCommand != null) {
                        appendCommandOutput(activeCommand);
                        if (activeCommand.isDone()) {
                            activeCommand = null;
                        }
                    }
                }
            }
            this.currentPath = fs.getCurrentPath();
            return;
        }

        if (trimmed.isEmpty()) return;
        String[] split = trimmed.split("\\s+", 2);
        String cmdName = split[0].toLowerCase();
        String args = split.length > 1 ? split[1] : "";

        TerminalCommand cmd = CommandRegistry.create(cmdName, fs, computerPos, level, args);
        if (cmd == null) {
            addLine("> " + trimmed);
            addLine(Component.translatable("message.null_ouroboros.terminus.invalid_command", cmdName).getString());
            return;
        }

        addLine("> " + trimmed);
        try {
            cmd.execute();
            appendCommandOutput(cmd);

            FileSessionRequest sessionRequest = cmd.getFileSessionRequest();
            if (sessionRequest != null) {
                beginFileSession(sessionRequest.file(), fs, sessionRequest.mode());
            }

            if (cmd.requestsShutdown()) {
                shutdownRequested = true;
            }

            if (cmd.requestsClear()) {
                clearToBootState(fs);
            }

            if (!cmd.isDone()) {
                activeCommand = cmd;
            }
        } catch (Exception e) {
            addLine(Component.translatable("message.null_ouroboros.terminus.internal_error").getString());
            activeCommand = null;
            e.printStackTrace();
        }

        this.currentPath = fs.getCurrentPath();
    }

    private void appendCommandOutput(TerminalCommand cmd) {
        for (Component comp : cmd.getOutput()) {
            addLine(comp.getString());
        }
        cmd.clearOutput();
    }

    public void syncToClient(ServerPlayer player, TerminusFileSystem fs) {
        if (player == null) return;

        ClientBoundDustyComputerSyncPacket.FileSessionType sessionType =
                ClientBoundDustyComputerSyncPacket.FileSessionType.NONE;
        String fileContent = "";
        String filePath = "";

        if (fileSession != null) {
            sessionType = fileSession.mode() == FileSessionMode.VIEW
                    ? ClientBoundDustyComputerSyncPacket.FileSessionType.VIEW
                    : ClientBoundDustyComputerSyncPacket.FileSessionType.EDIT;
            filePath = fileSession.filePath();
            TerminusNode node = fs.resolvePath(fileSession.filePath());
            if (node instanceof TerminusTextFile textFile) {
                fileContent = textFile.getContent();
            }
        }

        PacketHandlerRegistry.INSTANCE.send(
                PacketDistributor.PLAYER.with(() -> player),
                new ClientBoundDustyComputerSyncPacket(
                        computerPos,
                        new ArrayList<>(lines),
                        currentPath,
                        sessionType,
                        fileContent,
                        filePath));
    }
}
