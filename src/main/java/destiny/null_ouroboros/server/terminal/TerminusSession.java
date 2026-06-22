package destiny.null_ouroboros.server.terminal;

import destiny.null_ouroboros.client.network.ClientBoundDustyComputerSyncPacket;
import destiny.null_ouroboros.server.registry.PacketHandlerRegistry;
import destiny.null_ouroboros.server.terminal.filesystem.TerminusFileSystem;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TerminusSession {
    private final UUID filesystemId;
    private final BlockPos computerPos;
    private final List<String> lines = new ArrayList<>();
    private String currentPath = "T:\\";
    @Nullable
    private TerminalCommand activeCommand = null;
    @Nullable
    private UUID playerId = null;

    public TerminusSession(UUID filesystemId, BlockPos computerPos) {
        this.filesystemId = filesystemId;
        this.computerPos = computerPos;
    }

    public List<String> getLines() { return lines; }
    public String getCurrentPath() { return currentPath; }
    public void setCurrentPath(String path) { this.currentPath = path; }
    @Nullable public UUID getPlayerId() { return playerId; }
    public void setPlayerId(@Nullable UUID playerId) { this.playerId = playerId; }
    @Nullable public TerminalCommand getActiveCommand() { return activeCommand; }
    public void clearActiveCommand() { activeCommand = null; }

    public void addLine(String line) { lines.add(line); }
    public void clearLines() { lines.clear(); }

    public void processCommand(String rawLine, TerminusFileSystem fs) {
        String trimmed = rawLine.trim();

        if (activeCommand != null && activeCommand.isDone()) {
            activeCommand = null;
        }

        if (activeCommand != null) {
            if (trimmed.equalsIgnoreCase("cancel")) {
                try { activeCommand.cancel(); } catch (Exception ignored) {}
                addLine("> " + trimmed);
                addLine("Command cancelled.");
                activeCommand = null;
            } else {
                addLine("> " + trimmed);
                try {
                    activeCommand.handleInput(trimmed);
                } catch (Exception e) {
                    addLine("An error occurred while processing your response.");
                    activeCommand = null;
                }
                if (activeCommand != null) {
                    for (Component comp : activeCommand.getOutput()) {
                        addLine(comp.getString());
                    }
                    activeCommand.clearOutput();
                    if (activeCommand.isDone()) {
                        activeCommand = null;
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

        TerminalCommand cmd = CommandRegistry.create(cmdName, fs, computerPos, args);
        if (cmd == null) {
            addLine("> " + trimmed);
            addLine("'" + cmdName + "' is not a valid command.");
            return;
        }

        addLine("> " + trimmed);
        try {
            cmd.execute();
            for (Component comp : cmd.getOutput()) {
                addLine(comp.getString());
            }
            cmd.clearOutput();

            if (!cmd.isDone()) {
                activeCommand = cmd;
            }
        } catch (Exception e) {
            addLine("An internal error occurred.");
            activeCommand = null;
            e.printStackTrace();
        }

        this.currentPath = fs.getCurrentPath();
    }

    public void syncToClient(ServerPlayer player) {
        if (player == null) return;
        PacketHandlerRegistry.INSTANCE.send(
                PacketDistributor.PLAYER.with(() -> player),
                new ClientBoundDustyComputerSyncPacket(computerPos, new ArrayList<>(lines), currentPath));
    }
}