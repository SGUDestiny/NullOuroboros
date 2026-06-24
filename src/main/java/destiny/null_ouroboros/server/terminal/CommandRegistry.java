package destiny.null_ouroboros.server.terminal;

import destiny.null_ouroboros.server.terminal.filesystem.TerminusFileSystem;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CommandRegistry {
    @FunctionalInterface
    public interface CommandFactory {
        TerminalCommand create(TerminusFileSystem fs, BlockPos pos, @Nullable Level level, String arguments);
    }

    public record CommandEntry(String primaryName, String usageKey, CommandFactory factory, @Nullable String helpDisplayName) {}

    private static final Map<String, CommandEntry> byName = new HashMap<>();
    private static final Map<String, CommandEntry> byHelpName = new HashMap<>();
    private static final List<CommandEntry> primaryCommands = new ArrayList<>();
    private static final Map<String, String> aliasToPrimary = new HashMap<>();

    public static void registerPrimary(String name, String usageKey, CommandFactory factory) {
        registerPrimary(name, usageKey, factory, name);
    }

    public static void registerPrimary(String name, String usageKey, CommandFactory factory, @Nullable String helpDisplayName) {
        CommandEntry entry = new CommandEntry(name, usageKey, factory, helpDisplayName);
        primaryCommands.add(entry);
        byName.put(name.toLowerCase(), entry);
        if (helpDisplayName != null) {
            byHelpName.put(helpDisplayName.toLowerCase(), entry);
        }
    }

    public static void registerAlias(String alias, String primaryName) {
        aliasToPrimary.put(alias.toLowerCase(), primaryName.toLowerCase());
        CommandEntry entry = byName.get(primaryName.toLowerCase());
        if (entry != null) {
            byName.put(alias.toLowerCase(), entry);
        }
    }

    public static TerminalCommand create(String name, TerminusFileSystem fs, BlockPos pos, @Nullable Level level, String arguments) {
        CommandEntry entry = byName.get(name.toLowerCase());
        return entry != null ? entry.factory().create(fs, pos, level, arguments) : null;
    }

    public static List<CommandEntry> getHelpCommands() {
        return primaryCommands.stream()
                .filter(entry -> entry.helpDisplayName() != null)
                .toList();
    }

    @Nullable
    public static String getUsageKey(String name) {
        CommandEntry entry = byHelpName.get(name.toLowerCase());
        if (entry != null) {
            return entry.usageKey();
        }

        entry = byName.get(resolvePrimaryName(name));
        if (entry != null && name.equalsIgnoreCase(entry.helpDisplayName())) {
            return entry.usageKey();
        }
        return null;
    }

    public static String resolvePrimaryName(String name) {
        String lower = name.toLowerCase();
        String primary = aliasToPrimary.get(lower);
        if (primary != null) {
            return primary;
        }
        CommandEntry entry = byName.get(lower);
        return entry != null ? entry.primaryName().toLowerCase() : lower;
    }

    public static boolean isKnownCommand(String name) {
        return byName.containsKey(name.toLowerCase());
    }
}
