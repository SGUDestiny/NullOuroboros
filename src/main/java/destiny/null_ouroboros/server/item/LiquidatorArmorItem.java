package destiny.null_ouroboros.server.item;

import destiny.null_ouroboros.NullOuroboros;
import destiny.null_ouroboros.client.item.LiquidatorArmorClientExtensions;
import destiny.null_ouroboros.server.registry.ArmorMaterialRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.fml.DistExecutor;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

public class LiquidatorArmorItem extends ArmorItem {
    public LiquidatorArmorItem(ArmorMaterial material, ArmorItem.Type type, Properties props) {
        super(material, type, props);
    }

    public static boolean isWearingFullSet(LivingEntity entity) {
        return isLiquidator(entity.getItemBySlot(EquipmentSlot.HEAD))
                && isLiquidator(entity.getItemBySlot(EquipmentSlot.CHEST))
                && isLiquidator(entity.getItemBySlot(EquipmentSlot.LEGS))
                && isLiquidator(entity.getItemBySlot(EquipmentSlot.FEET));
    }

    private static boolean isLiquidator(ItemStack stack) {
        return stack.getItem() instanceof ArmorItem armor
                && armor.getMaterial() == ArmorMaterialRegistry.LIQUIDATOR;
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

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        if (getType() == ArmorItem.Type.HELMET) {
            RespiratorGear.appendFilterDurabilityTooltip(stack, tooltip, flag);
        }
    }
}
