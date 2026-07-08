package destiny.null_ouroboros.common.dusterbike;

import destiny.null_ouroboros.server.registry.ItemRegistry;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.RegistryObject;

import java.util.Locale;

public enum DusterbikePartType {
    FRAME("frame", 1024, null, false),
    FRONT_WHEEL("front_wheel", 512, ItemRegistry.BIKE_WHEEL, true),
    REAR_WHEEL("rear_wheel", 512, ItemRegistry.BIKE_WHEEL, true),
    FRONT_LIGHT("front_light", 512, ItemRegistry.HEADLIGHT, true),
    REAR_LIGHT("rear_light", 512, ItemRegistry.HEADLIGHT, true),
    BATTERY("battery", 512, ItemRegistry.BIKE_BATTERY, true),
    ENGINE("engine", 0, null, false),
    PISTON_FRONT("piston_front", 512, ItemRegistry.PISTON, true),
    PISTON_REAR("piston_rear", 512, ItemRegistry.PISTON, true),
    SPARK_PLUG_FRONT("spark_plug_front", 512, ItemRegistry.SPARK_PLUG, true),
    SPARK_PLUG_REAR("spark_plug_rear", 512, ItemRegistry.SPARK_PLUG, true),
    KEY("key", 0, ItemRegistry.BIKE_KEY, true);

    private final String serializedName;
    private final int maxDurability;
    private final RegistryObject<Item> item;
    private final boolean removable;

    DusterbikePartType(String serializedName, int maxDurability, RegistryObject<Item> item, boolean removable) {
        this.serializedName = serializedName;
        this.maxDurability = maxDurability;
        this.item = item;
        this.removable = removable;
    }

    public String serializedName() {
        return serializedName;
    }

    public int maxDurability() {
        return maxDurability;
    }

    public boolean hasDurability() {
        return maxDurability > 0;
    }

    public boolean hasItemForm() {
        return item != null;
    }

    public Item item() {
        return item != null ? item.get() : null;
    }

    public boolean isRemovable() {
        return removable;
    }

    public static DusterbikePartType bySerializedName(String name) {
        for (DusterbikePartType type : values()) {
            if (type.serializedName.equals(name)) {
                return type;
            }
        }
        return valueOf(name.toUpperCase(Locale.ROOT));
    }
}
