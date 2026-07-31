package destiny.null_ouroboros.server.item;

import destiny.null_ouroboros.common.dusterbike.DusterbikePartItems;
import destiny.null_ouroboros.common.dusterbike.DusterbikePartState;
import destiny.null_ouroboros.common.dusterbike.DusterbikePartType;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Supplier;

public class BikePartItem extends Item {
    @Nullable
    private final Supplier<Ingredient> repairIngredient;

    public BikePartItem(Properties properties) {
        this(properties, null);
    }

    public BikePartItem(Properties properties, @Nullable Supplier<Ingredient> repairIngredient) {
        super(properties);
        this.repairIngredient = repairIngredient;
    }

    @Override
    public boolean isValidRepairItem(ItemStack toRepair, ItemStack repair) {
        return repairIngredient != null && repairIngredient.get().test(repair);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        CompoundTag partTag = DusterbikePartItems.getPartTag(stack);
        DusterbikePartType type = DusterbikePartItems.getPartType(stack);
        if (type != null && type.hasDurability() && stack.isDamageableItem()) {
            int durability = Math.max(0, type.maxDurability() - stack.getDamageValue());
            tooltip.add(Component.literal("Durability: " + durability).withStyle(ChatFormatting.GRAY));
        } else if (partTag != null && partTag.contains(DusterbikePartState.ITEM_DURABILITY_TAG)) {
            tooltip.add(Component.literal("Durability: " + partTag.getInt(DusterbikePartState.ITEM_DURABILITY_TAG))
                    .withStyle(ChatFormatting.GRAY));
        }
        if (partTag == null) {
            return;
        }
        appendColorLine(tooltip, "Main Color: ", partTag, DusterbikePartState.ITEM_MAIN_COLOR_TAG);
        appendColorLine(tooltip, "Glow Color: ", partTag, DusterbikePartState.ITEM_GLOW_COLOR_TAG);
    }

    private static void appendColorLine(List<Component> tooltip, String label, CompoundTag tag, String key) {
        if (!tag.contains(key)) {
            return;
        }
        int color = tag.getInt(key) & 0xFFFFFF;
        String hex = String.format("#%06X", color);
        tooltip.add(Component.literal(label).withStyle(ChatFormatting.GRAY)
                .append(Component.literal(hex).withStyle(style -> style.withColor(color))));
    }
}
