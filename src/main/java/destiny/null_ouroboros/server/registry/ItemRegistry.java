package destiny.null_ouroboros.server.registry;

import destiny.null_ouroboros.NullOuroboros;
import destiny.null_ouroboros.server.item.BurrowBeaconItem;
import destiny.null_ouroboros.server.item.DisketteItem;
import destiny.null_ouroboros.server.item.RedstickItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ItemRegistry {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, NullOuroboros.MODID);

    public static final RegistryObject<Item> BURROW_BEACON = ITEMS.register("burrow_beacon",
            () -> new BurrowBeaconItem(new Item.Properties().stacksTo(16)));
    public static final RegistryObject<Item> ENGINE_HOIST = ITEMS.register("engine_hoist",
            () -> new Item(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> REDSTICK = ITEMS.register("redstick",
            () -> new RedstickItem(new Item.Properties().stacksTo(16)));
    public static final RegistryObject<Item> DISKETTE = ITEMS.register("diskette",
            () -> new DisketteItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> WRENCH = ITEMS.register("wrench",
            () -> new Item(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> JERRYCAN = ITEMS.register("jerrycan",
            () -> new Item(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> SPRAY_CAN = ITEMS.register("spray_can",
            () -> new Item(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> BIKE_KEY = ITEMS.register("bike_key",
            () -> new Item(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> BIKE_BATTERY = ITEMS.register("bike_battery",
            () -> new Item(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> BIKE_WHEEL = ITEMS.register("bike_wheel",
            () -> new Item(new Item.Properties().stacksTo(2)));
    public static final RegistryObject<Item> PISTON = ITEMS.register("piston",
            () -> new Item(new Item.Properties().stacksTo(2)));
    public static final RegistryObject<Item> SPARK_PLUG = ITEMS.register("spark_plug",
            () -> new Item(new Item.Properties().stacksTo(2)));
    public static final RegistryObject<Item> HEADLIGHT = ITEMS.register("headlight",
            () -> new Item(new Item.Properties().stacksTo(2)));
}