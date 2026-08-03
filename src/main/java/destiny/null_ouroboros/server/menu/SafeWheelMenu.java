package destiny.null_ouroboros.server.menu;

import destiny.null_ouroboros.server.block.entity.SafeBlockEntity;
import destiny.null_ouroboros.server.registry.MenuRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public class SafeWheelMenu extends AbstractContainerMenu {
    private final SafeBlockEntity blockEntity;

    public SafeWheelMenu(int containerId, Inventory playerInv, FriendlyByteBuf data) {
        this(containerId, playerInv, getBlockEntity(playerInv, data.readBlockPos()));
    }

    public SafeWheelMenu(int containerId, Inventory playerInv, SafeBlockEntity blockEntity) {
        super(MenuRegistry.SAFE_WHEEL_MENU.get(), containerId);
        this.blockEntity = blockEntity;
    }

    private static SafeBlockEntity getBlockEntity(Inventory playerInv, BlockPos pos) {
        BlockEntity be = playerInv.player.level().getBlockEntity(pos);
        if (be instanceof SafeBlockEntity safe) {
            return safe;
        }
        throw new IllegalStateException("Safe missing at " + pos);
    }

    public SafeBlockEntity getBlockEntity() {
        return blockEntity;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos()),
                player, blockEntity.getBlockState().getBlock());
    }
}
