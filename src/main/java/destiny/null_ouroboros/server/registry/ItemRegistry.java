package destiny.null_ouroboros.server.registry;

import destiny.null_ouroboros.NullOuroboros;
import destiny.null_ouroboros.server.item.BurrowBeaconItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ItemRegistry {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, NullOuroboros.MODID);

    public static final RegistryObject<Item> BURROW_BEACON = ITEMS.register("burrow_beacon",
            () -> new BurrowBeaconItem(new Item.Properties().stacksTo(16)));

    public static Item.Properties basicItem() {
        return new Item.Properties().stacksTo(1);
    }
}
