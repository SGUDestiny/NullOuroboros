package destiny.null_ouroboros.server.menu;

import destiny.null_ouroboros.server.block.entity.FuseBoxBlockEntity;
import destiny.null_ouroboros.server.registry.BlockRegistry;
import destiny.null_ouroboros.server.registry.MenuRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.SlotItemHandler;

public class FuseBoxMenu extends AbstractContainerMenu {
    public static final int FUSE_SLOT_COUNT = FuseBoxBlockEntity.SLOT_COUNT;
    private static final int PLAYER_INV_X = 8;
    private static final int PLAYER_INV_Y = 205;

    private final FuseBoxBlockEntity blockEntity;

    public FuseBoxMenu(int containerId, Inventory playerInv, FriendlyByteBuf data) {
        this(containerId, playerInv, getBlockEntity(playerInv, data.readBlockPos()));
    }

    public FuseBoxMenu(int containerId, Inventory playerInv, FuseBoxBlockEntity blockEntity) {
        super(MenuRegistry.FUSE_BOX_MENU.get(), containerId);
        this.blockEntity = blockEntity;

        for (int i = 0; i < FUSE_SLOT_COUNT; i++) {
            int col = i % 2;
            int row = i / 2;
            int x = (col == 0 ? 34 : 110);
            int y = 10 + 26 * row;
            addSlot(new SlotItemHandler(blockEntity.getInventory(), i, x, y) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return !blockEntity.isCycling() && super.mayPlace(stack);
                }

                @Override
                public boolean mayPickup(Player playerIn) {
                    return !blockEntity.isCycling() && super.mayPickup(playerIn);
                }
            });
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

    private static FuseBoxBlockEntity getBlockEntity(Inventory playerInv, BlockPos pos) {
        BlockEntity be = playerInv.player.level().getBlockEntity(pos);
        if (be instanceof FuseBoxBlockEntity fuseBox) {
            return fuseBox;
        }
        throw new IllegalStateException("Fuse box missing at " + pos);
    }

    public FuseBoxBlockEntity getBlockEntity() {
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

        if (index < FUSE_SLOT_COUNT) {
            if (!moveItemStackTo(stack, FUSE_SLOT_COUNT, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, 0, FUSE_SLOT_COUNT, false)) {
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
        return stillValid(net.minecraft.world.inventory.ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos()),
                player, BlockRegistry.FUSE_BOX.get());
    }
}
