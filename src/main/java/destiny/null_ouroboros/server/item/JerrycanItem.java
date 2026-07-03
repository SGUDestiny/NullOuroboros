package destiny.null_ouroboros.server.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class JerrycanItem extends Item {
    public static final int CAPACITY_MB = 20_000;
    private static final String FUEL_TAG = "FuelMilliBuckets";

    public JerrycanItem(Properties properties) {
        super(properties);
    }

    public static ItemStack createFilled() {
        ItemStack stack = new ItemStack(destiny.null_ouroboros.server.registry.ItemRegistry.JERRYCAN.get());
        setFuel(stack, CAPACITY_MB);
        return stack;
    }

    public static int getFuel(ItemStack stack) {
        return stack.getOrCreateTag().getInt(FUEL_TAG);
    }

    public static void setFuel(ItemStack stack, int amount) {
        stack.getOrCreateTag().putInt(FUEL_TAG, Math.max(0, Math.min(CAPACITY_MB, amount)));
    }

    public static int addFuel(ItemStack stack, int amount) {
        int accepted = Math.min(Math.max(0, amount), CAPACITY_MB - getFuel(stack));
        setFuel(stack, getFuel(stack) + accepted);
        return accepted;
    }

    public static int removeFuel(ItemStack stack, int amount) {
        int removed = Math.min(Math.max(0, amount), getFuel(stack));
        setFuel(stack, getFuel(stack) - removed);
        return removed;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0F * getFuel(stack) / CAPACITY_MB);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0xD9A441;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Fuel: " + getFuel(stack) + " mB").withStyle(ChatFormatting.GRAY));
    }
}
