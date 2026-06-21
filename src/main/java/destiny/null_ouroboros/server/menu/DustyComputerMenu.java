package destiny.null_ouroboros.server.menu;

import destiny.null_ouroboros.server.block.DustyComputerBlock;
import destiny.null_ouroboros.server.block.entity.DustyComputerBlockEntity;
import destiny.null_ouroboros.server.registry.BlockRegistry;
import destiny.null_ouroboros.server.registry.MenuRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.UUID;

public class DustyComputerMenu extends AbstractContainerMenu {
    private final ContainerLevelAccess access;
    private final BlockPos pos;
    @Nullable
    private UUID currentUserId;

    public DustyComputerMenu(int containerId, Inventory playerInv, FriendlyByteBuf data) {
        this(containerId, playerInv, ContainerLevelAccess.NULL, data.readBlockPos());
    }

    public DustyComputerMenu(int containerId, Inventory playerInv, ContainerLevelAccess access, BlockPos pos) {
        super(MenuRegistry.DUSTY_COMPUTER_MENU.get(), containerId);
        this.access = access;
        this.pos = pos;

        if (playerInv.player instanceof ServerPlayer serverPlayer) {
            this.currentUserId = serverPlayer.getUUID();
        } else {
            this.currentUserId = null;
        }
    }

    public void addCommand(String commandText) {
        if (commandText.isEmpty()) return;

        access.execute((level, blockPos) -> {
            DustyComputerBlockEntity be = (DustyComputerBlockEntity) level.getBlockEntity(blockPos);
            if (be != null) {
                be.addLine("> " + commandText);
            }
        });
    }

    public BlockPos getBlockPos() {
        return pos;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);

        if (!player.level().isClientSide && currentUserId != null) {
            BlockEntity entity = player.level().getBlockEntity(pos);

            if (entity instanceof DustyComputerBlockEntity computer) {
                computer.unclaim(currentUserId);
            }
        }
    }

    @Override
    public boolean stillValid(Player player) {
        if (currentUserId != null && !player.getUUID().equals(currentUserId)) {
            return false;
        }

        if (player.level().isClientSide) {
            BlockState state = player.level().getBlockState(pos);
            return state.is(BlockRegistry.DUSTY_COMPUTER.get()) && state.getValue(DustyComputerBlock.POWERED) &&
                    player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0;
        } else {
            return this.access.evaluate((level, blockPos) -> {
                BlockState state = level.getBlockState(blockPos);

                return state.is(BlockRegistry.DUSTY_COMPUTER.get()) && state.getValue(DustyComputerBlock.POWERED) &&
                        player.distanceToSqr(blockPos.getX() + 0.5, blockPos.getY() + 0.5, blockPos.getZ() + 0.5) <= 64.0;
            }, true);
        }
    }
}