package destiny.null_ouroboros.server.item;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class SelfEntangledSubstanceItem extends Item {
    public static final String TICK = "tick";

    public SelfEntangledSubstanceItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return false;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int i, boolean b) {
        if (level.isClientSide()) return;

        if (stack.getTag() == null || stack.getTag().get(TICK) == null) {
            stack.getOrCreateTag().putInt(TICK, 0);
        }

        int tick = stack.getTag().getInt(TICK);
        float chance = 0.001f * ((float) tick / 20);

        if (level.random.nextFloat() < chance) {
            stack.setCount(0);
            return;
        }

        stack.getTag().putInt(TICK, tick + 1);
    }
}
