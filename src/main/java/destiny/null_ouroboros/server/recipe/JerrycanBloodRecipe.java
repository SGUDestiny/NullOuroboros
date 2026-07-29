package destiny.null_ouroboros.server.recipe;

import destiny.null_ouroboros.server.item.JerrycanItem;
import destiny.null_ouroboros.server.registry.ItemRegistry;
import destiny.null_ouroboros.server.registry.RecipeRegistry;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public class JerrycanBloodRecipe extends CustomRecipe {
    public enum Mode {
        FILL_CAN,
        FILL_BUCKET
    }

    private final Mode mode;

    public JerrycanBloodRecipe(ResourceLocation id, CraftingBookCategory category, Mode mode) {
        super(id, category);
        this.mode = mode;
    }

    @Override
    public boolean matches(CraftingContainer container, Level level) {
        ItemStack jerrycan = ItemStack.EMPTY;
        ItemStack other = ItemStack.EMPTY;
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }
            if (stack.getItem() instanceof JerrycanItem) {
                if (!jerrycan.isEmpty()) {
                    return false;
                }
                jerrycan = stack;
                continue;
            }
            if (!other.isEmpty()) {
                return false;
            }
            other = stack;
        }
        if (jerrycan.isEmpty() || other.isEmpty()) {
            return false;
        }
        if (mode == Mode.FILL_CAN) {
            return other.is(ItemRegistry.BLOOD_BUCKET.get())
                    && JerrycanItem.CAPACITY_MB - JerrycanItem.getFuel(jerrycan) >= JerrycanItem.BLOCK_MB;
        }
        return other.is(Items.BUCKET)
                && JerrycanItem.getFuel(jerrycan) >= JerrycanItem.BLOCK_MB;
    }

    @Override
    public ItemStack assemble(CraftingContainer container, RegistryAccess registryAccess) {
        ItemStack jerrycan = ItemStack.EMPTY;
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (stack.getItem() instanceof JerrycanItem) {
                jerrycan = stack;
                break;
            }
        }
        if (mode == Mode.FILL_CAN) {
            ItemStack result = jerrycan.copy();
            result.setCount(1);
            JerrycanItem.addFuel(result, JerrycanItem.BLOCK_MB);
            return result;
        }
        return new ItemStack(ItemRegistry.BLOOD_BUCKET.get());
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingContainer container) {
        NonNullList<ItemStack> remaining = NonNullList.withSize(container.getContainerSize(), ItemStack.EMPTY);
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }
            if (mode == Mode.FILL_BUCKET && stack.getItem() instanceof JerrycanItem) {
                ItemStack left = stack.copy();
                left.setCount(1);
                JerrycanItem.removeFuel(left, JerrycanItem.BLOCK_MB);
                remaining.set(slot, left);
                continue;
            }
            if (stack.hasCraftingRemainingItem()) {
                remaining.set(slot, stack.getCraftingRemainingItem());
            }
        }
        return remaining;
    }

    @Override
    public ItemStack getResultItem(RegistryAccess registryAccess) {
        if (mode == Mode.FILL_CAN) {
            return JerrycanItem.createFilled();
        }
        return new ItemStack(ItemRegistry.BLOOD_BUCKET.get());
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return mode == Mode.FILL_CAN
                ? RecipeRegistry.JERRYCAN_FILL.get()
                : RecipeRegistry.JERRYCAN_EMPTY.get();
    }
}
