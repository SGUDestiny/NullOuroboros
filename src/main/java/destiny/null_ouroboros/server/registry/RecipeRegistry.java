package destiny.null_ouroboros.server.registry;

import destiny.null_ouroboros.NullOuroboros;
import destiny.null_ouroboros.server.recipe.SpeedloaderRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class RecipeRegistry {
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, NullOuroboros.MODID);
    public static final RegistryObject<RecipeSerializer<SpeedloaderRecipe>> SPEEDLOADER =
            RECIPE_SERIALIZERS.register("speedloader", () -> new SimpleCraftingRecipeSerializer<>(SpeedloaderRecipe::new));

    private RecipeRegistry() {
    }
}
