package destiny.null_ouroboros.server.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

public class BinarySwordItem extends SwordItem {
    public BinarySwordItem(Tier tier, int damage, float speed, Properties properties) {
        super(tier, damage, speed, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> components, TooltipFlag flag) {
        MutableComponent tooltip = Component.translatable("tooltip.null_ouroboros.binary_sword").withStyle(ChatFormatting.GRAY);
        components.add(tooltip);
    }
}
