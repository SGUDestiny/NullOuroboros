package destiny.null_ouroboros.server.item;

import destiny.null_ouroboros.common.dusterbike.DusterbikePartItems;
import destiny.null_ouroboros.common.dusterbike.DusterbikePartState;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class BikePartItem extends Item {
    public BikePartItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        CompoundTag partTag = DusterbikePartItems.getPartTag(stack);
        if (partTag == null) {
            return;
        }

        if (partTag.contains(DusterbikePartState.ITEM_DURABILITY_TAG)) {
            tooltip.add(Component.literal("Durability: " + partTag.getInt(DusterbikePartState.ITEM_DURABILITY_TAG))
                    .withStyle(ChatFormatting.GRAY));
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
