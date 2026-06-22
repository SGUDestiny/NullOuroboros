package destiny.null_ouroboros.server.terminal;

import destiny.null_ouroboros.server.terminal.filesystem.TerminusFileSystem;
import net.minecraft.core.BlockPos;

import java.util.HashMap;
import java.util.Map;

public class CommandRegistry {
    @FunctionalInterface
    public interface CommandFactory {
        TerminalCommand create(TerminusFileSystem fs, BlockPos pos, String arguments);
    }

    private static final Map<String, CommandFactory> factories = new HashMap<>();

    public static void register(String name, CommandFactory factory) {
        factories.put(name.toLowerCase(), factory);
    }

    public static void registerAlias(String alias, String originalName) {
        var factory = factories.get(originalName.toLowerCase());
        if (factory != null) {
            factories.put(alias.toLowerCase(), factory);
        }
    }

    public static TerminalCommand create(String name, TerminusFileSystem fs, BlockPos pos, String arguments) {
        CommandFactory factory = factories.get(name.toLowerCase());
        return factory != null ? factory.create(fs, pos, arguments) : null;
    }
}