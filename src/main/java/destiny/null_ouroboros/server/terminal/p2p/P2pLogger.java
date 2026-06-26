package destiny.null_ouroboros.server.terminal.p2p;

import destiny.null_ouroboros.server.terminal.filesystem.FileSystemException;
import destiny.null_ouroboros.server.terminal.filesystem.TerminusDirectory;
import destiny.null_ouroboros.server.terminal.filesystem.TerminusFileSystem;
import destiny.null_ouroboros.server.terminal.filesystem.TerminusNode;
import destiny.null_ouroboros.server.terminal.filesystem.TerminusTextFile;
import net.minecraft.network.chat.Component;

import javax.annotation.Nullable;

public class P2pLogger {
    public enum Reason {
        CLOSED_BY_REMOTE("message.null_ouroboros.terminus.p2p.reason.closed_by_remote"),
        CLOSED_BY_LOCAL("message.null_ouroboros.terminus.p2p.reason.closed_by_local"),
        LOST("message.null_ouroboros.terminus.p2p.reason.lost"),
        LOCAL_FILTER("message.null_ouroboros.terminus.p2p.reason.local_filter");

        private final String translationKey;

        Reason(String translationKey) {
            this.translationKey = translationKey;
        }

        public String text() {
            return Component.translatable(translationKey).getString();
        }
    }

    @Nullable
    private final String path;

    private P2pLogger(@Nullable String path) {
        this.path = path;
    }

    public static P2pLogger disabled() {
        return new P2pLogger(null);
    }

    public static P2pLogger start(TerminusFileSystem fs, P2pSettings settings, String initiatorIpvInf) {
        if (!settings.loggingEnabled()) {
            return disabled();
        }
        P2pSettings.ensureDirectory(fs, settings.getLogDirectory());
        TerminusNode logDirNode = fs.resolvePath(settings.getLogDirectory());
        if (!(logDirNode instanceof TerminusDirectory logDir)) {
            return disabled();
        }
        String name = fs.resolveLogFileName(logDir);
        String path = settings.getLogDirectory();
        if (!path.endsWith("\\")) {
            path += "\\";
        }
        path += name;
        try {
            fs.createTextFile(path, "");
        } catch (FileSystemException ignored) {
            return disabled();
        }
        P2pLogger logger = new P2pLogger(path);
        logger.appendTranslatable(fs, "message.null_ouroboros.terminus.p2p.log.initiated", initiatorIpvInf);
        return logger;
    }

    public void message(TerminusFileSystem fs, String alias, String message) {
        appendTranslatable(fs, "message.null_ouroboros.terminus.p2p.log.message", alias, message);
    }

    public void sent(TerminusFileSystem fs, String file) {
        appendTranslatable(fs, "message.null_ouroboros.terminus.p2p.log.sent", file);
    }

    public void received(TerminusFileSystem fs, String file) {
        appendTranslatable(fs, "message.null_ouroboros.terminus.p2p.log.received", file);
    }

    public void end(TerminusFileSystem fs, Reason reason) {
        appendTranslatable(fs, "message.null_ouroboros.terminus.p2p.log.terminated", reason.text());
    }

    private void appendTranslatable(TerminusFileSystem fs, String key, Object... args) {
        append(fs, Component.translatable(key, args).getString());
    }

    private void append(TerminusFileSystem fs, String line) {
        if (path == null) {
            return;
        }
        TerminusNode node = fs.resolvePath(path);
        if (!(node instanceof TerminusTextFile file)) {
            return;
        }
        String content = file.getContent();
        file.setContent((content == null || content.isEmpty()) ? line : content + "\n" + line);
        fs.markDirtyForEdit();
    }
}
