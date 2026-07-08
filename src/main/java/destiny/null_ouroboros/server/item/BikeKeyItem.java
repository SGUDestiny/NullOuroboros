package destiny.null_ouroboros.server.item;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.DyeableLeatherItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

public class BikeKeyItem extends Item implements DyeableLeatherItem {
    public static final String BIKE_UUID_TAG = "BikeUuid";

    public BikeKeyItem(Properties properties) {
        super(properties);
    }

    public static boolean hasLinkedBike(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.hasUUID(BIKE_UUID_TAG);
    }

    public static UUID getLinkedBike(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.hasUUID(BIKE_UUID_TAG) ? tag.getUUID(BIKE_UUID_TAG) : null;
    }

    public static void setLinkedBike(ItemStack stack, UUID bikeUuid) {
        stack.getOrCreateTag().putUUID(BIKE_UUID_TAG, bikeUuid);
    }

    @Override
    public int getColor(ItemStack stack) {
        if (hasCustomColor(stack)) {
            return DyeableLeatherItem.super.getColor(stack);
        }
        return 0xFFFFFF;
    }
}
