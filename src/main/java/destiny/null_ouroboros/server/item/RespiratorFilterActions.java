package destiny.null_ouroboros.server.item;

import destiny.null_ouroboros.common.player_anim.PlayerAnimation;
import destiny.null_ouroboros.common.player_anim.RespiratorPlayerAnims;
import destiny.null_ouroboros.common.respirator.FilterAction;
import destiny.null_ouroboros.server.registry.SoundRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class RespiratorFilterActions {
    private static final Map<UUID, PendingFilterAction> PENDING = new HashMap<>();

    private RespiratorFilterActions() {
    }

    public static void handleAction(ServerPlayer player, FilterAction action) {
        if (action == null || PENDING.containsKey(player.getUUID())) {
            return;
        }
        ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
        RespiratorGear.ensureDefaults(helmet);
        if (!RespiratorGear.isRespiratorHelmet(helmet)) {
            return;
        }

        ResourceLocation animationId;
        boolean putLeft = false;
        if (action.isRemove()) {
            if (action.isLeft() ? !RespiratorGear.hasLeftFilter(helmet) : !RespiratorGear.hasRightFilter(helmet)) {
                return;
            }
            animationId = RespiratorPlayerAnims.animationId(action);
        } else {
            ItemStack held = player.getMainHandItem();
            if (!(held.getItem() instanceof FilterItem)) {
                return;
            }
            Boolean emptyLeft = RespiratorGear.firstEmptySlotPreferRight(helmet);
            if (emptyLeft == null) {
                return;
            }
            putLeft = emptyLeft;
            animationId = putLeft ? RespiratorPlayerAnims.FILTER_PUT_LEFT_ID : RespiratorPlayerAnims.FILTER_PUT_RIGHT_ID;
        }

        long now = player.level().getGameTime();
        PENDING.put(player.getUUID(), new PendingFilterAction(
                action,
                putLeft,
                player.getInventory().selected,
                now + RespiratorPlayerAnims.SOUND_DELAY_TICKS,
                now + RespiratorPlayerAnims.COMMIT_DELAY_TICKS,
                false,
                animationId
        ));
        PlayerAnimation.play(player, animationId, RespiratorPlayerAnims.actionOptions());
    }

    public static void tick(ServerPlayer player) {
        PendingFilterAction pending = PENDING.get(player.getUUID());
        if (pending == null) {
            return;
        }

        if (shouldInterrupt(player, pending)) {
            cancel(player);
            return;
        }

        long now = player.level().getGameTime();
        if (!pending.soundPlayed && now >= pending.soundGameTime) {
            player.level().playSound(null, player.blockPosition(), SoundRegistry.RESPIRATOR_FILTER_SCREW.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
            pending.soundPlayed = true;
        }

        if (now >= pending.commitGameTime) {
            commit(player, pending);
            PENDING.remove(player.getUUID());
        }
    }

    public static void cancel(Player player) {
        PendingFilterAction removed = PENDING.remove(player.getUUID());
        if (removed == null) {
            return;
        }
        PlayerAnimation.cancel(player, removed.animationId);
    }

    public static void clear(UUID playerId) {
        PENDING.remove(playerId);
    }

    private static boolean shouldInterrupt(ServerPlayer player, PendingFilterAction pending) {
        ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
        if (!RespiratorGear.isRespiratorHelmet(helmet)) {
            return true;
        }
        if (player.getInventory().selected != pending.selectedSlot) {
            return true;
        }
        if (!pending.action.isRemove() && !(player.getMainHandItem().getItem() instanceof FilterItem)) {
            return true;
        }
        return false;
    }

    private static void commit(ServerPlayer player, PendingFilterAction pending) {
        ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
        RespiratorGear.ensureDefaults(helmet);
        if (!RespiratorGear.isRespiratorHelmet(helmet)) {
            return;
        }

        if (pending.action.isRemove()) {
            boolean left = pending.action.isLeft();
            ItemStack filter = RespiratorGear.getFilter(helmet, left);
            if (filter.isEmpty()) {
                return;
            }
            RespiratorGear.setFilter(helmet, left, ItemStack.EMPTY);
            if (player.getMainHandItem().isEmpty()) {
                player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, filter);
            } else if (!player.getInventory().add(filter)) {
                player.drop(filter, false);
            }
            return;
        }

        ItemStack held = player.getMainHandItem();
        if (!(held.getItem() instanceof FilterItem)) {
            return;
        }
        boolean left = pending.putLeft;
        if (left ? RespiratorGear.hasLeftFilter(helmet) : RespiratorGear.hasRightFilter(helmet)) {
            return;
        }
        ItemStack inserted = held.split(1);
        RespiratorGear.setFilter(helmet, left, inserted);
    }

    private static final class PendingFilterAction {
        private final FilterAction action;
        private final boolean putLeft;
        private final int selectedSlot;
        private final long soundGameTime;
        private final long commitGameTime;
        private boolean soundPlayed;
        private final ResourceLocation animationId;

        private PendingFilterAction(FilterAction action, boolean putLeft, int selectedSlot, long soundGameTime,
                                    long commitGameTime, boolean soundPlayed, ResourceLocation animationId) {
            this.action = action;
            this.putLeft = putLeft;
            this.selectedSlot = selectedSlot;
            this.soundGameTime = soundGameTime;
            this.commitGameTime = commitGameTime;
            this.soundPlayed = soundPlayed;
            this.animationId = animationId;
        }
    }
}
