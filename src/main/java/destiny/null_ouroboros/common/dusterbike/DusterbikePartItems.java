package destiny.null_ouroboros.common.dusterbike;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

public final class DusterbikePartItems {
    private DusterbikePartItems() {}

    public static ItemStack createPartStack(DusterbikePartState state) {
        if (!state.type().hasItemForm()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = new ItemStack(state.type().item());
        stack.getOrCreateTag().put(DusterbikePartState.ITEM_PART_TAG, state.saveForItem());
        if (state.type().hasDurability()) {
            stack.setDamageValue(Math.max(0, state.maxDurability() - state.durability()));
        }
        return stack;
    }

    public static DusterbikePartType getPartType(ItemStack stack) {
        CompoundTag partTag = getPartTag(stack);
        if (partTag != null && partTag.contains(DusterbikePartState.ITEM_PART_TYPE_TAG)) {
            return DusterbikePartType.bySerializedName(partTag.getString(DusterbikePartState.ITEM_PART_TYPE_TAG));
        }

        for (DusterbikePartType type : DusterbikePartType.values()) {
            if (type.hasItemForm() && stack.is(type.item())) {
                return type;
            }
        }
        return null;
    }

    public static void applyStackToState(ItemStack stack, DusterbikePartState state) {
        CompoundTag partTag = getPartTag(stack);
        if (partTag != null) {
            state.loadFromItem(partTag);
        } else if (state.type().hasDurability() && stack.isDamageableItem()) {
            state.loadFromItem(defaultPartTag(state.type(), state.maxDurability() - stack.getDamageValue()));
        }
        state.setInstalled(true);
    }

    public static CompoundTag getPartTag(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(DusterbikePartState.ITEM_PART_TAG)) {
            return null;
        }
        return tag.getCompound(DusterbikePartState.ITEM_PART_TAG);
    }

    private static CompoundTag defaultPartTag(DusterbikePartType type, int durability) {
        CompoundTag tag = new CompoundTag();
        tag.putString(DusterbikePartState.ITEM_PART_TYPE_TAG, type.serializedName());
        tag.putInt(DusterbikePartState.ITEM_DURABILITY_TAG, durability);
        return tag;
    }
}
