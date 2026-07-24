package destiny.null_ouroboros.common.revolver;

import destiny.null_ouroboros.server.registry.ItemRegistry;
import net.minecraft.world.item.Item;

public enum RevolverCartridge {
    EMPTY,
    CASING,
    HP,
    AP,
    IC,
    OP;

    public boolean isLive() {
        return this == HP || this == AP || this == IC || this == OP;
    }

    public Item item() {
        return switch (this) {
            case CASING -> ItemRegistry.REVOLVER_EMPTY_CASING.get();
            case HP -> ItemRegistry.REVOLVER_CARTRIDGE_HP.get();
            case AP -> ItemRegistry.REVOLVER_CARTRIDGE_AP.get();
            case IC -> ItemRegistry.REVOLVER_CARTRIDGE_IC.get();
            case OP -> ItemRegistry.REVOLVER_CARTRIDGE_OP.get();
            case EMPTY -> null;
        };
    }

    public static RevolverCartridge fromItem(Item item) {
        if (item == ItemRegistry.REVOLVER_EMPTY_CASING.get()) {
            return CASING;
        }
        if (item == ItemRegistry.REVOLVER_CARTRIDGE_HP.get()) {
            return HP;
        }
        if (item == ItemRegistry.REVOLVER_CARTRIDGE_AP.get()) {
            return AP;
        }
        if (item == ItemRegistry.REVOLVER_CARTRIDGE_IC.get()) {
            return IC;
        }
        if (item == ItemRegistry.REVOLVER_CARTRIDGE_OP.get()) {
            return OP;
        }
        return EMPTY;
    }

    public static RevolverCartridge byOrdinal(int ordinal) {
        RevolverCartridge[] values = values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : EMPTY;
    }
}
