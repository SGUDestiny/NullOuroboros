package destiny.null_ouroboros.server.block;

import destiny.null_ouroboros.server.block.entity.DustyComputerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class ElectromagneticAssemblyBlock extends ComputerPeripheralBlock {
    public ElectromagneticAssemblyBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        if (level.isClientSide) {
            return;
        }
        if (fromPos.equals(pos.below()) && level.getBlockEntity(pos.below()) instanceof DustyComputerBlockEntity computer) {
            computer.refreshEmaConnection();
        }
    }
}
