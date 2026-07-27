package destiny.null_ouroboros.server.item;

import destiny.null_ouroboros.NullOuroboros;
import destiny.null_ouroboros.client.item.RespiratorClientExtensions;
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

public class RespiratorItem extends ArmorItem {
    public RespiratorItem(ArmorMaterial material, Properties properties) {
        super(material, ArmorItem.Type.HELMET, properties);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> RespiratorClientExtensions.register(consumer));
    }

    @Override
    public @Nullable String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
        return NullOuroboros.MODID + ":textures/entity/respirator.png";
    }

    @Override
    public ItemStack getDefaultInstance() {
        ItemStack stack = super.getDefaultInstance();
        RespiratorGear.ensureDefaults(stack);
        return stack;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return RespiratorGear.isFilterBarVisible(stack);
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return RespiratorGear.getFilterBarWidth(stack);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return RespiratorGear.getFilterBarColor(stack);
    }
}
