package destiny.null_ouroboros.common.dusterbike;

import destiny.null_ouroboros.server.registry.SoundRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class PartInteraction {
    private PartInteraction() {}

    public static void showDurability(Player player, DusterbikePartState state) {
        if (state == null || !state.installed() || !state.type().hasDurability()) {
            return;
        }
        sendActionBar(player, Component.translatable(
                "message.null_ouroboros.dusterbike.part_durability",
                state.durability(),
                state.maxDurability()));
    }

    public static void sendActionBar(Player player, Component component) {
        player.displayClientMessage(component, true);
    }

    public static void damageTool(Player player, InteractionHand hand, ItemStack stack) {
        stack.hurtAndBreak(1, player, broken -> broken.broadcastBreakEvent(hand));
    }

    public static void playWrenchSound(Level level, BlockPos pos) {
        level.playSound(null, pos, SoundRegistry.WRENCH_INTERACT.get(), SoundSource.PLAYERS, 0.5f, 1f);
    }

    public static void playSpraySound(Level level, BlockPos pos) {
        level.playSound(null, pos, SoundRegistry.SPRAY_CAN_INTERACT.get(), SoundSource.PLAYERS, 0.5f, 1f);
    }

    public static void playPartInstallSound(Level level, BlockPos pos) {
        level.playSound(null, pos, SoundRegistry.DUSTERBIKE_PART_INSTALL.get(), SoundSource.PLAYERS, 0.5f, 1f);
    }

    public static void playKeyInsertSound(Level level, BlockPos pos) {
        level.playSound(null, pos, SoundRegistry.DUSTERBIKE_KEY_INSERT.get(), SoundSource.PLAYERS, 0.5f, 1f);
    }

    public static void playKeyRemoveSound(Level level, BlockPos pos) {
        level.playSound(null, pos, SoundRegistry.DUSTERBIKE_KEY_INSERT.get(), SoundSource.PLAYERS, 0.5f, 0.8f);
    }

    public static void playEngineHoistInteractSound(Level level, BlockPos pos) {
        level.playSound(null, pos, SoundRegistry.ENGINE_HOIST_INTERACT.get(), SoundSource.PLAYERS, 0.5f, 1f);
    }
}
