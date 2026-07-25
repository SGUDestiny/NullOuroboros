package destiny.null_ouroboros.server.item;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class FuseItem extends Item {
    public static final String LINKED_POS = "linked_pos";

    public FuseItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        InteractionHand hand = context.getHand();

        if (hand == InteractionHand.OFF_HAND) return InteractionResult.FAIL;

        Player player = context.getPlayer();
        ItemStack stack = player.getItemInHand(hand);
        boolean isCrouching = player.isCrouching();

        if (isCrouching) {
            if (stack.getTag() != null && stack.getTag().get(LINKED_POS) != null) {
                stack.getTag().remove(LINKED_POS);

                player.displayClientMessage(Component.translatable("message.null_ouroboros.fuse_unlink"), true);

                return InteractionResult.SUCCESS;
            }

            return InteractionResult.FAIL;
        } else {
            BlockPos clickedPos = context.getClickedPos();
            Level level = context.getLevel();
            BlockState clickedBlock = level.getBlockState(clickedPos);

            if (clickedBlock.getBlock() == Blocks.REDSTONE_LAMP) {
                stack.getOrCreateTag().put(LINKED_POS, NbtUtils.writeBlockPos(clickedPos));

                player.displayClientMessage(Component.translatable("message.null_ouroboros.fuse_link"), true);

                return InteractionResult.SUCCESS;
            }

            player.displayClientMessage(Component.translatable("message.null_ouroboros.fuse_cannot_link"), true);

            return InteractionResult.FAIL;
        }
    }
}
