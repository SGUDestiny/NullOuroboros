package destiny.null_ouroboros.server.registry;

import destiny.null_ouroboros.NullOuroboros;
import destiny.null_ouroboros.server.menu.DustyComputerMenu;
import destiny.null_ouroboros.server.menu.FuseBoxMenu;
import destiny.null_ouroboros.server.menu.SafeInventoryMenu;
import destiny.null_ouroboros.server.menu.SafeWheelMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class MenuRegistry {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, NullOuroboros.MODID);

    public static final RegistryObject<MenuType<DustyComputerMenu>> DUSTY_COMPUTER_MENU = MENUS.register("dusty_computer", () -> IForgeMenuType.create(DustyComputerMenu::new));
    public static final RegistryObject<MenuType<FuseBoxMenu>> FUSE_BOX_MENU = MENUS.register("fuse_box", () -> IForgeMenuType.create(FuseBoxMenu::new));
    public static final RegistryObject<MenuType<SafeWheelMenu>> SAFE_WHEEL_MENU = MENUS.register("safe_wheel", () -> IForgeMenuType.create(SafeWheelMenu::new));
    public static final RegistryObject<MenuType<SafeInventoryMenu>> SAFE_INVENTORY_MENU = MENUS.register("safe_inventory", () -> IForgeMenuType.create(SafeInventoryMenu::new));
}
