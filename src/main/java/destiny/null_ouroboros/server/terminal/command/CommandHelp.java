package destiny.null_ouroboros.server.terminal.command;

import destiny.null_ouroboros.server.terminal.CommandRegistry;
import destiny.null_ouroboros.server.terminal.TerminalCommand;
import destiny.null_ouroboros.server.terminal.filesystem.TerminusFileSystem;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

public class CommandHelp extends TerminalCommand {
    private final String args;

    public CommandHelp(TerminusFileSystem fs, BlockPos pos, net.minecraft.world.level.Level level, String args) {
        super(fs, pos, level);
        this.args = args.trim();
    }

    @Override
    public void execute() {
        if (args.isEmpty()) {
            printAllHelp();
            setDone();
            return;
        }

        String usageKey = CommandRegistry.getUsageKey(args);
        if (usageKey == null) {
            String commandName = args.split("\\s+")[0];
            usageKey = CommandRegistry.getUsageKey(commandName);
            if (usageKey == null) {
                printlnTranslatable("message.null_ouroboros.terminus.help.unknown_command", commandName);
                setDone();
                return;
            }
        }
        printlnTranslatable(usageKey);
        setDone();
    }

    private void printAllHelp() {
        var commands = CommandRegistry.getHelpCommands();
        int nameWidth = commands.stream()
                .mapToInt(entry -> entry.helpDisplayName().length())
                .max()
                .orElse(4);

        for (CommandRegistry.CommandEntry entry : commands) {
            String fullUsage = Component.translatable(entry.usageKey()).getString();
            String usageArgs = usageArgsOnly(fullUsage, entry.helpDisplayName());
            println(String.format("%-" + nameWidth + "s  %s", entry.helpDisplayName(), usageArgs));
        }
    }

    private static String usageArgsOnly(String fullUsage, String commandName) {
        String prefix = commandName + " ";
        if (fullUsage.startsWith(prefix)) {
            return fullUsage.substring(prefix.length());
        }
        return fullUsage;
    }
}
