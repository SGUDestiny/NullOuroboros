package destiny.null_ouroboros.server.recipe;

import destiny.null_ouroboros.common.revolver.RevolverCartridge;
import destiny.null_ouroboros.server.item.SpeedloaderItem;
import destiny.null_ouroboros.server.registry.ItemRegistry;
import destiny.null_ouroboros.server.registry.RecipeRegistry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

public class SpeedloaderRecipe extends CustomRecipe {
    public SpeedloaderRecipe(ResourceLocation id, CraftingBookCategory category) {
        super(id, category);
    }

    @Override
    public boolean matches(CraftingContainer container, Level level) {
        ItemStack speedloader = ItemStack.EMPTY;
        List<RevolverCartridge> added = new ArrayList<>();
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }
            if (stack.getItem() instanceof SpeedloaderItem) {
                if (!speedloader.isEmpty()) {
                    return false;
                }
                speedloader = stack;
                continue;
            }
            RevolverCartridge cartridge = RevolverCartridge.fromItem(stack.getItem());
            if (!cartridge.isLive()) {
                return false;
            }
            added.add(cartridge);
        }
        if (speedloader.isEmpty() || added.isEmpty()) {
            return false;
        }
        return SpeedloaderItem.getRounds(speedloader).size() + added.size() <= SpeedloaderItem.CAPACITY;
    }

    @Override
    public ItemStack assemble(CraftingContainer container, RegistryAccess registryAccess) {
        ItemStack speedloader = ItemStack.EMPTY;
        List<RevolverCartridge> added = new ArrayList<>();
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }
            if (stack.getItem() instanceof SpeedloaderItem) {
                speedloader = stack;
                continue;
            }
            RevolverCartridge cartridge = RevolverCartridge.fromItem(stack.getItem());
            if (cartridge.isLive()) {
                added.add(cartridge);
            }
        }
        List<RevolverCartridge> rounds = new ArrayList<>(SpeedloaderItem.getRounds(speedloader));
        rounds.addAll(added);
        ItemStack result = speedloader.copy();
        result.setCount(1);
        SpeedloaderItem.setRounds(result, rounds);
        return result;
    }

    @Override
    public ItemStack getResultItem(RegistryAccess registryAccess) {
        return new ItemStack(ItemRegistry.SPEEDLOADER.get());
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return RecipeRegistry.SPEEDLOADER.get();
    }
}
