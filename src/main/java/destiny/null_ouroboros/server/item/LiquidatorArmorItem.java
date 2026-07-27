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
    public ItemStack getDefaultInstance() {
        ItemStack stack = super.getDefaultInstance();
        if (getType() == ArmorItem.Type.HELMET) {
            RespiratorGear.ensureDefaults(stack);
        }
        return stack;
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> LiquidatorArmorClientExtensions.register(consumer));
    }

    @Override
    public @Nullable String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
        return NullOuroboros.MODID + ":textures/entity/liquidator_armor.png";
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        if (getType() != ArmorItem.Type.HELMET) {
            return super.isBarVisible(stack);
        }
        return RespiratorGear.isFilterBarVisible(stack);
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        if (getType() != ArmorItem.Type.HELMET) {
            return super.getBarWidth(stack);
        }
        return RespiratorGear.getFilterBarWidth(stack);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        if (getType() != ArmorItem.Type.HELMET) {
            return super.getBarColor(stack);
        }
        return RespiratorGear.getFilterBarColor(stack);
    }
}
