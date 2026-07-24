package destiny.null_ouroboros.server.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import destiny.null_ouroboros.server.capability.ManifoldingCapability;
import destiny.null_ouroboros.server.registry.CapabilityRegistry;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

public final class ManifoldingCommand {
    private ManifoldingCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("manifolding")
                .requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS));

        root.then(Commands.literal("start").executes(context -> start(context.getSource())));
        root.then(Commands.literal("end").executes(context -> end(context.getSource())));

        dispatcher.register(root);
    }

    private static int start(CommandSourceStack source) {
        ServerLevel level = requireVergeLevel(source);
        if (level == null) {
            return 0;
        }

        return level.getCapability(CapabilityRegistry.MANIFOLDING_CAPABILITY).map(cap -> {
            if (!cap.tryStart(level)) {
                source.sendFailure(Component.translatable("commands.null_ouroboros.manifolding.start.failed"));
                return 0;
            }
            source.sendSuccess(() -> Component.translatable("commands.null_ouroboros.manifolding.start.success"), true);
            return 1;
        }).orElseGet(() -> {
            source.sendFailure(Component.translatable("commands.null_ouroboros.manifolding.capability_missing"));
            return 0;
        });
    }

    private static int end(CommandSourceStack source) {
        ServerLevel level = requireVergeLevel(source);
        if (level == null) {
            return 0;
        }

        return level.getCapability(CapabilityRegistry.MANIFOLDING_CAPABILITY).map(cap -> {
            if (!cap.tryEnd(level)) {
                source.sendFailure(Component.translatable("commands.null_ouroboros.manifolding.end.failed"));
                return 0;
            }
            source.sendSuccess(() -> Component.translatable("commands.null_ouroboros.manifolding.end.success"), true);
            return 1;
        }).orElseGet(() -> {
            source.sendFailure(Component.translatable("commands.null_ouroboros.manifolding.capability_missing"));
            return 0;
        });
    }

    private static ServerLevel requireVergeLevel(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        if (!level.dimension().location().equals(ManifoldingCapability.DIMENSION_ID)) {
            source.sendFailure(Component.translatable("commands.null_ouroboros.manifolding.wrong_dimension"));
            return null;
        }
        return level;
    }
}
