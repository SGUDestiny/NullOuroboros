package destiny.null_ouroboros.client.item;

import destiny.null_ouroboros.client.render.model.EmptyArmorModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import java.util.function.Consumer;

public final class LiquidatorArmorClientExtensions {
    private LiquidatorArmorClientExtensions() {
    }

    public static void register(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override
            public HumanoidModel<?> getHumanoidArmorModel(LivingEntity living, ItemStack stack, EquipmentSlot slot, HumanoidModel<?> defaultModel) {
                return EmptyArmorModel.get();
            }
        });
    }
}
