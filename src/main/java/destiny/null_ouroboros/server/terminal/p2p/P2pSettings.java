package destiny.null_ouroboros.server.terminal.p2p;

import destiny.null_ouroboros.server.terminal.filesystem.FileSystemException;
import destiny.null_ouroboros.server.terminal.filesystem.TerminusDirectory;
import destiny.null_ouroboros.server.terminal.filesystem.TerminusFileSystem;
import destiny.null_ouroboros.server.terminal.filesystem.TerminusNode;
import destiny.null_ouroboros.server.terminal.filesystem.TerminusTextFile;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

import javax.annotation.Nullable;
import java.util.Locale;

public class P2pSettings {
    public enum FilterMode {
        WLIST,
        BLIST,
        NONE;

        @Nullable
        public static FilterMode parse(String raw) {
            try {
                return valueOf(raw.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
    }

    private String alias = "";
    private FilterMode filterMode = FilterMode.NONE;
    private String filterFilePath;
    private String logDirectory;
    private String receiverDirectory;

    public P2pSettings(String driveSpecifier) {
        resetDefaults(driveSpecifier);
    }

    public void resetDefaults(String driveSpecifier) {
        String root = driveSpecifier + "\\";
        this.filterFilePath = root + "p2p\\filter.txt";
        this.logDirectory = root + "p2p\\logs\\";
        this.receiverDirectory = root + "p2p\\received\\";
    }

    public String getAlias() { return alias; }
    public void setAlias(String alias) { this.alias = alias != null ? alias.trim() : ""; }
    public String displayName(String ipvInf) { return alias.isEmpty() ? ipvInf : alias; }

    public FilterMode getFilterMode() { return filterMode; }
    public void setFilterMode(FilterMode filterMode) { this.filterMode = filterMode != null ? filterMode : FilterMode.NONE; }

    public String getFilterFilePath() { return filterFilePath; }
    public void setFilterFilePath(String filterFilePath) { this.filterFilePath = filterFilePath; }
    public String getLogDirectory() { return logDirectory; }
    public void setLogDirectory(String logDirectory) { this.logDirectory = logDirectory != null ? logDirectory : ""; }
    public String getReceiverDirectory() { return receiverDirectory; }
    public void setReceiverDirectory(String receiverDirectory) { this.receiverDirectory = receiverDirectory; }

    public boolean loggingEnabled() {
        return logDirectory != null && !logDirectory.isBlank();
    }

    public boolean passesFilter(String remoteIpvInf, TerminusFileSystem fs) {
        if (filterMode == FilterMode.NONE) {
            return true;
        }
        boolean listed = readFilterFile(fs).contains(remoteIpvInf);
        return filterMode == FilterMode.WLIST ? listed : !listed;
    }

    public void bootstrap(TerminusFileSystem fs) {
        ensureDirectory(fs, getRootP2pDirectory(fs));
        ensureDirectory(fs, getLogDirectory());
        ensureDirectory(fs, getReceiverDirectory());
        if (!(fs.resolvePath(getFilterFilePath()) instanceof TerminusTextFile)) {
            try {
                ensureParentDirectory(fs, getFilterFilePath());
                fs.createTextFile(getFilterFilePath(), "");
            } catch (FileSystemException ignored) {}
        }
    }

    public void addFilterEntry(String ipvInf, TerminusFileSystem fs) throws FileSystemException {
        TerminusTextFile file = getOrCreateFilterFile(fs);
        String value = ipvInf;
        if (readFilterFile(fs).contains(value)) {
            return;
        }
        String content = file.getContent();
        if (content == null || content.isEmpty()) {
            file.setContent(value);
        } else {
            file.setContent(content.replaceAll("\\s+$", "") + "\n" + value);
        }
        fs.markDirtyForEdit();
    }

    public void removeFilterEntry(String ipvInf, TerminusFileSystem fs) throws FileSystemException {
        TerminusTextFile file = getOrCreateFilterFile(fs);
        String target = ipvInf;
        StringBuilder next = new StringBuilder();
        for (String line : file.getContent().split("\\R")) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty() && !trimmed.equalsIgnoreCase(target)) {
                if (next.length() > 0) {
                    next.append('\n');
                }
                next.append(trimmed);
            }
        }
        file.setContent(next.toString());
        fs.markDirtyForEdit();
    }

    private java.util.Set<String> readFilterFile(TerminusFileSystem fs) {
        java.util.Set<String> entries = new java.util.HashSet<>();
        TerminusNode node = fs.resolvePath(filterFilePath);
        if (!(node instanceof TerminusTextFile file)) {
            return entries;
        }
        for (String line : file.getContent().split("\\R")) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) {
                entries.add(trimmed);
            }
        }
        return entries;
    }

    private TerminusTextFile getOrCreateFilterFile(TerminusFileSystem fs) throws FileSystemException {
        TerminusNode node = fs.resolvePath(filterFilePath);
        if (node instanceof TerminusTextFile file) {
            return file;
        }
        ensureParentDirectory(fs, filterFilePath);
        fs.createTextFile(filterFilePath, "");
        node = fs.resolvePath(filterFilePath);
        if (node instanceof TerminusTextFile file) {
            return file;
        }
        throw new FileSystemException(Component.translatable(
                "message.null_ouroboros.terminus.p2p.error.filter_file_unavailable").getString());
    }

    private String getRootP2pDirectory(TerminusFileSystem fs) {
        return fs.getDriveSpecifier() + "\\p2p\\";
    }

    private static void ensureParentDirectory(TerminusFileSystem fs, String path) throws FileSystemException {
        String parent = fs.getParentPath(path);
        ensureDirectory(fs, parent);
    }

    public static void ensureDirectory(TerminusFileSystem fs, String path) {
        if (path == null || path.isBlank()) {
            return;
        }
        String normalized = path.endsWith("\\") ? path.substring(0, path.length() - 1) : path;
        String root = fs.getDriveSpecifier();
        if (normalized.equalsIgnoreCase(root)) {
            return;
        }
        if (!normalized.startsWith(root + "\\")) {
            return;
        }
        String[] parts = normalized.substring((root + "\\").length()).split("\\\\");
        String current = root + "\\";
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            String next = current + part;
            TerminusNode node = fs.resolvePath(next);
            if (node == null) {
                try {
                    fs.createDirectory(next);
                } catch (FileSystemException ignored) {}
            } else if (!(node instanceof TerminusDirectory)) {
                return;
            }
            current = next + "\\";
        }
    }

    public CompoundTag toNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Alias", alias);
        tag.putString("FilterMode", filterMode.name());
        tag.putString("FilterFilePath", filterFilePath);
        tag.putString("LogDirectory", logDirectory);
        tag.putString("ReceiverDirectory", receiverDirectory);
        return tag;
    }

    public void fromNBT(CompoundTag tag, String driveSpecifier) {
        resetDefaults(driveSpecifier);
        alias = tag.getString("Alias");
        FilterMode parsed = FilterMode.parse(tag.getString("FilterMode"));
        filterMode = parsed != null ? parsed : FilterMode.NONE;
        if (tag.contains("FilterFilePath")) filterFilePath = tag.getString("FilterFilePath");
        if (tag.contains("LogDirectory")) logDirectory = tag.getString("LogDirectory");
        if (tag.contains("ReceiverDirectory")) receiverDirectory = tag.getString("ReceiverDirectory");
    }
}
