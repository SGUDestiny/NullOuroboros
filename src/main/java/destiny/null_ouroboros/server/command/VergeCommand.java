package destiny.null_ouroboros.server.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import destiny.null_ouroboros.server.registry.CapabilityRegistry;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class VergeCommand {
    private static final List<String> STAGE_SUGGESTIONS = List.of("0", "1", "2");

    private VergeCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("verge")
                .requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS));

        root.then(ManifoldingCommand.branch());
        root.then(Commands.literal("asphyxiation")
                .then(Commands.literal("stage")
                        .then(Commands.literal("set")
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .then(Commands.argument("stage", IntegerArgumentType.integer(0, 2))
                                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(STAGE_SUGGESTIONS, builder))
                                                .executes(context -> setStage(
                                                        context.getSource(),
                                                        EntityArgument.getPlayers(context, "targets"),
                                                        IntegerArgumentType.getInteger(context, "stage"))))))
                        .then(Commands.literal("get")
                                .then(Commands.argument("target", EntityArgument.player())
                                        .executes(context -> getStage(
                                                context.getSource(),
                                                EntityArgument.getPlayer(context, "target")))))));

        dispatcher.register(root);
    }

    private static int setStage(CommandSourceStack source, Collection<ServerPlayer> targets, int stage) {
        List<ServerPlayer> updated = new ArrayList<>();
        for (ServerPlayer player : targets) {
            boolean ok = player.getCapability(CapabilityRegistry.RESPIRATORY_CAPABILITY).map(cap -> {
                cap.ensureInitialized(player);
                cap.setStage(stage);
                return true;
            }).orElse(false);
            if (ok) {
                updated.add(player);
            }
        }
        if (updated.isEmpty()) {
            source.sendFailure(Component.translatable("commands.null_ouroboros.verge.asphyxiation.capability_missing"));
            return 0;
        }
        if (updated.size() == 1) {
            ServerPlayer only = updated.get(0);
            source.sendSuccess(() -> Component.translatable(
                    "commands.null_ouroboros.verge.asphyxiation.stage.set.success.single",
                    stage,
                    only.getDisplayName()), true);
        } else {
            int count = updated.size();
            source.sendSuccess(() -> Component.translatable(
                    "commands.null_ouroboros.verge.asphyxiation.stage.set.success.multiple",
                    stage,
                    count), true);
        }
        return updated.size();
    }

    private static int getStage(CommandSourceStack source, ServerPlayer target) {
        return target.getCapability(CapabilityRegistry.RESPIRATORY_CAPABILITY).map(cap -> {
            cap.ensureInitialized(target);
            int stage = cap.getStage();
            source.sendSuccess(() -> Component.translatable(
                    "commands.null_ouroboros.verge.asphyxiation.stage.get.success",
                    stage), false);
            return 1;
        }).orElseGet(() -> {
            source.sendFailure(Component.translatable("commands.null_ouroboros.verge.asphyxiation.capability_missing"));
            return 0;
        });
    }
}
