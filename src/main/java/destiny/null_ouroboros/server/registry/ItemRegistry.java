package destiny.null_ouroboros.server.registry;

import destiny.null_ouroboros.NullOuroboros;
import destiny.null_ouroboros.server.item.*;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ItemRegistry {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, NullOuroboros.MODID);

    public static final RegistryObject<Item> BLACKMETAL_PANEL = ITEMS.register("blackmetal_panel",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> BURROW_BEACON = ITEMS.register("burrow_beacon",
            () -> new BurrowBeaconItem(new Item.Properties().stacksTo(16)));

    public static final RegistryObject<Item> REDSTICK = ITEMS.register("redstick",
            () -> new RedstickItem(new Item.Properties().stacksTo(16)));
    public static final RegistryObject<Item> DISKETTE = ITEMS.register("diskette",
            () -> new DisketteItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> WRENCH = ITEMS.register("wrench",
            () -> new WrenchItem(new Item.Properties().stacksTo(1).durability(128)));
    public static final RegistryObject<Item> JERRYCAN = ITEMS.register("jerrycan",
            () -> new JerrycanItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> SPRAY_CAN = ITEMS.register("spray_can",
            () -> new SprayCanItem(new Item.Properties().stacksTo(1).durability(128)));

    public static final RegistryObject<Item> ENGINE_HOIST = ITEMS.register("engine_hoist",
            () -> new EngineHoistItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> DUSTERBIKE_FRAME = ITEMS.register("dusterbike_frame",
            () -> new DusterbikeFrameItem(new Item.Properties().stacksTo(1), false));
    public static final RegistryObject<Item> DUSTERBIKE_FRAME_BUILT = ITEMS.register("dusterbike_frame_built",
            () -> new DusterbikeFrameItem(new Item.Properties().stacksTo(1), true));

    public static final RegistryObject<Item> BIKE_KEY = ITEMS.register("bike_key",
            () -> new BikeKeyItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> BIKE_BATTERY = ITEMS.register("bike_battery",
            () -> new BikePartItem(new Item.Properties().stacksTo(1).durability(512)));
    public static final RegistryObject<Item> BIKE_WHEEL = ITEMS.register("bike_wheel",
            () -> new BikePartItem(new Item.Properties().stacksTo(2).durability(512)));
    public static final RegistryObject<Item> PISTON = ITEMS.register("piston",
            () -> new BikePartItem(new Item.Properties().stacksTo(2).durability(512)));
    public static final RegistryObject<Item> SPARK_PLUG = ITEMS.register("spark_plug",
            () -> new BikePartItem(new Item.Properties().stacksTo(2).durability(512)));
    public static final RegistryObject<Item> HEADLIGHT = ITEMS.register("headlight",
            () -> new BikePartItem(new Item.Properties().stacksTo(2).durability(512)));
    public static final RegistryObject<Item> ENGINE_BASE = ITEMS.register("engine_base",
            () -> new BikePartItem(new Item.Properties().stacksTo(2).durability(-1)));

    public static final RegistryObject<Item> RAKE = ITEMS.register("rake",
            () -> new RakeItem(new Item.Properties().stacksTo(1), 9.0F, -3F, 1024));
    public static final RegistryObject<Item> BINARY_SWORD = ITEMS.register("binary_sword",
            () -> new BinarySwordItem(TierRegistry.BINARY_SWORD,0, 0, new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> BINARY_SHARD = ITEMS.register("binary_shard",
            () -> new BinaryShardItem(TierRegistry.BINARY_SHARD, 0, 0, new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> LIQUIDATOR_GAS_MASK = ITEMS.register("liquidator_gas_mask",
            () -> new LiquidatorArmorItem(ArmorMaterialRegistry.LIQUIDATOR, ArmorItem.Type.HELMET, new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> LIQUIDATOR_CHESTPLATE = ITEMS.register("liquidator_chestplate",
            () -> new LiquidatorArmorItem(ArmorMaterialRegistry.LIQUIDATOR, ArmorItem.Type.CHESTPLATE, new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> LIQUIDATOR_LEGGINGS = ITEMS.register("liquidator_leggings",
            () -> new LiquidatorArmorItem(ArmorMaterialRegistry.LIQUIDATOR, ArmorItem.Type.LEGGINGS, new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> LIQUIDATOR_BOOTS = ITEMS.register("liquidator_boots",
            () -> new LiquidatorArmorItem(ArmorMaterialRegistry.LIQUIDATOR, ArmorItem.Type.BOOTS, new Item.Properties().stacksTo(1)));
}