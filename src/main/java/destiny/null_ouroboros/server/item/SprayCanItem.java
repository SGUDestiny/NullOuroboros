package destiny.null_ouroboros.server.item;

import net.minecraft.world.item.DyeableLeatherItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class SprayCanItem extends Item implements DyeableLeatherItem {
    public SprayCanItem(Properties properties) {
        super(properties);
    }

    @Override
    public int getColor(ItemStack stack) {
        if (hasCustomColor(stack)) {
            return DyeableLeatherItem.super.getColor(stack);
        }
        return 0xFFFFFF;
    }
}
