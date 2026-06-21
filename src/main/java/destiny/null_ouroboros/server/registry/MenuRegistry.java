package destiny.null_ouroboros.server.registry;

import destiny.null_ouroboros.NullOuroboros;
import destiny.null_ouroboros.server.menu.DustyComputerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class MenuRegistry {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, NullOuroboros.MODID);

    public static final RegistryObject<MenuType<DustyComputerMenu>> DUSTY_COMPUTER_MENU = MENUS.register("dusty_computer", () -> IForgeMenuType.create(DustyComputerMenu::new));
}
