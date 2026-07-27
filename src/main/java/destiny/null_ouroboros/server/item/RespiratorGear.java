package destiny.null_ouroboros.server.item;

import destiny.null_ouroboros.server.registry.ArmorMaterialRegistry;
import destiny.null_ouroboros.server.registry.ItemRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class RespiratorGear {
    public static final String LEFT_FILTER_TAG = "LeftFilter";
    public static final String RIGHT_FILTER_TAG = "RightFilter";
    public static final int FILTER_MAX_DAMAGE = 1024;

    private RespiratorGear() {
    }

    public static boolean isRespiratorHelmet(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (stack.getItem() instanceof RespiratorItem) {
            return true;
        }
        return stack.getItem() instanceof ArmorItem armor
                && armor.getType() == ArmorItem.Type.HELMET
                && armor.getMaterial() == ArmorMaterialRegistry.LIQUIDATOR;
    }

    public static boolean isProtected(Player player) {
        ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
        ensureDefaults(head);
        return isRespiratorHelmet(head) && hasWorkingFilter(head);
    }

    public static void ensureDefaults(ItemStack helmet) {
        if (!isRespiratorHelmet(helmet)) {
            return;
        }
        CompoundTag tag = helmet.getOrCreateTag();
        if (!tag.contains(LEFT_FILTER_TAG) && !tag.contains(RIGHT_FILTER_TAG) && !tag.getBoolean("FiltersInitialized")) {
            setFilter(helmet, true, createFullFilter());
            setFilter(helmet, false, createFullFilter());
            tag.putBoolean("FiltersInitialized", true);
        }
    }

    public static ItemStack createFullFilter() {
        ItemStack filter = new ItemStack(ItemRegistry.FILTER.get());
        filter.setDamageValue(0);
        return filter;
    }

    public static boolean hasLeftFilter(ItemStack helmet) {
        ensureDefaults(helmet);
        return helmet.getTag() != null && helmet.getTag().contains(LEFT_FILTER_TAG);
    }

    public static boolean hasRightFilter(ItemStack helmet) {
        ensureDefaults(helmet);
        return helmet.getTag() != null && helmet.getTag().contains(RIGHT_FILTER_TAG);
    }

    public static ItemStack getFilter(ItemStack helmet, boolean left) {
        ensureDefaults(helmet);
        CompoundTag tag = helmet.getTag();
        if (tag == null) {
            return ItemStack.EMPTY;
        }
        String key = left ? LEFT_FILTER_TAG : RIGHT_FILTER_TAG;
        if (!tag.contains(key)) {
            return ItemStack.EMPTY;
        }
        return ItemStack.of(tag.getCompound(key));
    }

    public static void setFilter(ItemStack helmet, boolean left, ItemStack filter) {
        CompoundTag tag = helmet.getOrCreateTag();
        String key = left ? LEFT_FILTER_TAG : RIGHT_FILTER_TAG;
        if (filter.isEmpty()) {
            tag.remove(key);
        } else {
            tag.put(key, filter.save(new CompoundTag()));
        }
        tag.putBoolean("FiltersInitialized", true);
    }

    public static boolean hasWorkingFilter(ItemStack helmet) {
        return isFilterWorking(getFilter(helmet, true)) || isFilterWorking(getFilter(helmet, false));
    }

    public static boolean hasAnyDrainableFilter(ItemStack helmet) {
        return hasWorkingFilter(helmet);
    }

    public static boolean isFilterWorking(ItemStack filter) {
        return !filter.isEmpty() && filter.getDamageValue() < FILTER_MAX_DAMAGE;
    }

    public static void hurtRandomFilter(Player player, ItemStack helmet, int amount) {
        ensureDefaults(helmet);
        List<Boolean> candidates = new ArrayList<>(2);
        if (isFilterWorking(getFilter(helmet, true))) {
            candidates.add(true);
        }
        if (isFilterWorking(getFilter(helmet, false))) {
            candidates.add(false);
        }
        if (candidates.isEmpty()) {
            return;
        }
        boolean left = candidates.get(player.getRandom().nextInt(candidates.size()));
        ItemStack filter = getFilter(helmet, left);
        int damage = Math.min(FILTER_MAX_DAMAGE, filter.getDamageValue() + amount);
        filter.setDamageValue(damage);
        setFilter(helmet, left, filter);
    }

    public static Boolean firstEmptySlotPreferRight(ItemStack helmet) {
        ensureDefaults(helmet);
        if (!hasRightFilter(helmet)) {
            return false;
        }
        if (!hasLeftFilter(helmet)) {
            return true;
        }
        return null;
    }

    public static int getCombinedFilterRemaining(ItemStack helmet) {
        ensureDefaults(helmet);
        int remaining = 0;
        ItemStack left = getFilterWithoutEnsure(helmet, true);
        ItemStack right = getFilterWithoutEnsure(helmet, false);
        if (!left.isEmpty()) {
            remaining += Math.max(0, FILTER_MAX_DAMAGE - left.getDamageValue());
        }
        if (!right.isEmpty()) {
            remaining += Math.max(0, FILTER_MAX_DAMAGE - right.getDamageValue());
        }
        return remaining;
    }

    public static int getCombinedFilterMax() {
        return FILTER_MAX_DAMAGE * 2;
    }

    public static boolean isFilterBarVisible(ItemStack helmet) {
        return getCombinedFilterRemaining(helmet) < getCombinedFilterMax();
    }

    public static int getFilterBarWidth(ItemStack helmet) {
        int max = getCombinedFilterMax();
        if (max <= 0) {
            return 0;
        }
        return Math.round(13.0F * getCombinedFilterRemaining(helmet) / max);
    }

    public static int getFilterBarColor(ItemStack helmet) {
        float fraction = (float) getCombinedFilterRemaining(helmet) / (float) getCombinedFilterMax();
        return net.minecraft.util.Mth.hsvToRgb(Math.max(0.0F, fraction) / 3.0F, 1.0F, 1.0F);
    }

    private static ItemStack getFilterWithoutEnsure(ItemStack helmet, boolean left) {
        CompoundTag tag = helmet.getTag();
        if (tag == null) {
            return ItemStack.EMPTY;
        }
        String key = left ? LEFT_FILTER_TAG : RIGHT_FILTER_TAG;
        if (!tag.contains(key)) {
            return ItemStack.EMPTY;
        }
        return ItemStack.of(tag.getCompound(key));
    }
}
