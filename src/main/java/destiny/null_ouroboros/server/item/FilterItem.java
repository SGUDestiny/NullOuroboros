package destiny.null_ouroboros.server.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class FilterItem extends Item {
    public FilterItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isRepairable(ItemStack stack) {
        return false;
    }
}
