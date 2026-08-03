package destiny.null_ouroboros.server.menu;

import destiny.null_ouroboros.server.block.entity.SafeBlockEntity;
import destiny.null_ouroboros.server.registry.MenuRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.SlotItemHandler;

public class SafeInventoryMenu extends AbstractContainerMenu {
    public static final int SAFE_SLOT_COUNT = SafeBlockEntity.SLOT_COUNT;
    private static final int SAFE_SLOT_X = 43;
    private static final int SAFE_SLOT_Y = 20;
    private static final int PLAYER_INV_X = 7;
    private static final int PLAYER_INV_Y = 135;

    private final SafeBlockEntity blockEntity;

    public SafeInventoryMenu(int containerId, Inventory playerInv, FriendlyByteBuf data) {
        this(containerId, playerInv, getBlockEntity(playerInv, data.readBlockPos()));
    }

    public SafeInventoryMenu(int containerId, Inventory playerInv, SafeBlockEntity blockEntity) {
        super(MenuRegistry.SAFE_INVENTORY_MENU.get(), containerId);
        this.blockEntity = blockEntity;

        for (int row = 0; row < 5; row++) {
            for (int col = 0; col < 5; col++) {
                int index = row * 5 + col;
                addSlot(new SlotItemHandler(blockEntity.getInventory(), index,
                        SAFE_SLOT_X + col * 18, SAFE_SLOT_Y + row * 18));
            }
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInv, col + row * 9 + 9, PLAYER_INV_X + col * 18, PLAYER_INV_Y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInv, col, PLAYER_INV_X + col * 18, PLAYER_INV_Y + 58));
        }

        if (!playerInv.player.level().isClientSide) {
            blockEntity.startOpen(playerInv.player);
        }
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
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return result;
        }

        ItemStack stack = slot.getItem();
        result = stack.copy();

        if (index < SAFE_SLOT_COUNT) {
            if (!moveItemStackTo(stack, SAFE_SLOT_COUNT, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, 0, SAFE_SLOT_COUNT, false)) {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }

        return result;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!player.level().isClientSide) {
            blockEntity.stopOpen(player);
        }
    }

    @Override
    public boolean stillValid(Player player) {
        if (blockEntity.isLatchLocked()) {
            return false;
        }
        return stillValid(ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos()),
                player, blockEntity.getBlockState().getBlock());
    }
}
