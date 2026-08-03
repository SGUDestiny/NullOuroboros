package destiny.null_ouroboros.server.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class FuseItem extends Item {
    public static final String LINKED_POS = "linked_pos";
    public static final String LINKED_RELATIVE = "linked_relative";
    public static final String FUSE_MODE = "fuse_mode";
    public static final String MODE_TOGGLE = "toggle";
    public static final String MODE_PULSE = "pulse";

    public FuseItem(Properties properties) {
        super(properties);
    }

    public static String getMode(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(FUSE_MODE)) {
            return MODE_TOGGLE;
        }
        String mode = tag.getString(FUSE_MODE);
        return MODE_PULSE.equals(mode) ? MODE_PULSE : MODE_TOGGLE;
    }

    public static boolean isPulse(ItemStack stack) {
        return MODE_PULSE.equals(getMode(stack));
    }

    public static void setMode(ItemStack stack, String mode) {
        stack.getOrCreateTag().putString(FUSE_MODE, mode);
    }

    public static void cycleMode(ItemStack stack) {
        setMode(stack, isPulse(stack) ? MODE_TOGGLE : MODE_PULSE);
    }

    @Nullable
    public static BlockPos getLinkedPos(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(LINKED_POS)) {
            return null;
        }
        return NbtUtils.readBlockPos(tag.getCompound(LINKED_POS));
    }

    public static void setLinkedPos(ItemStack stack, BlockPos pos) {
        stack.getOrCreateTag().put(LINKED_POS, NbtUtils.writeBlockPos(pos));
    }

    public static boolean isLinkedRelative(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.getBoolean(LINKED_RELATIVE);
    }

    public static void setLinkedRelative(ItemStack stack, boolean relative) {
        if (relative) {
            stack.getOrCreateTag().putBoolean(LINKED_RELATIVE, true);
            return;
        }
        CompoundTag tag = stack.getTag();
        if (tag != null) {
            tag.remove(LINKED_RELATIVE);
        }
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        if (context.getHand() == InteractionHand.OFF_HAND) {
            return InteractionResult.PASS;
        }

        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }

        if (player.isShiftKeyDown()) {
            cycleMode(stack);
            player.displayClientMessage(modeComponent(stack), true);
            return InteractionResult.SUCCESS;
        }

        setLinkedPos(stack, context.getClickedPos());
        setLinkedRelative(stack, false);
        player.displayClientMessage(Component.translatable("message.null_ouroboros.fuse_link"), true);
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(modeComponent(stack).copy().withStyle(ChatFormatting.GRAY));
        BlockPos linked = getLinkedPos(stack);
        if (linked != null) {
            tooltip.add(Component.literal(linked.getX() + ", " + linked.getY() + ", " + linked.getZ())
                    .withStyle(ChatFormatting.GRAY));
        }
    }

    private static Component modeComponent(ItemStack stack) {
        return Component.translatable(isPulse(stack)
                ? "tooltip.null_ouroboros.fuse.pulse"
                : "tooltip.null_ouroboros.fuse.toggle");
    }
}
