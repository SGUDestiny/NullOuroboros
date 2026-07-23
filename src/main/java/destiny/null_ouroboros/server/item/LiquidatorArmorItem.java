package destiny.null_ouroboros.server.item;

import destiny.null_ouroboros.NullOuroboros;
import destiny.null_ouroboros.client.item.LiquidatorArmorClientExtensions;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.fml.DistExecutor;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class LiquidatorArmorItem extends ArmorItem {
    public LiquidatorArmorItem(ArmorMaterial material, ArmorItem.Type type, Properties props) {
        super(material, type, props);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> LiquidatorArmorClientExtensions.register(consumer));
    }

    @Override
    public @Nullable String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
        return NullOuroboros.MODID + ":textures/entity/liquidator_armor.png";
    }
}
