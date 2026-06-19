package destiny.null_ouroboros.server.block.entity;

import destiny.null_ouroboros.server.block.TemporalSurgeDetectorBlock;
import destiny.null_ouroboros.server.registry.BlockEntityRegistry;
import destiny.null_ouroboros.server.registry.CapabilityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class TemporalSurgeDetectorBlockEntity extends BlockEntity {
    public TemporalSurgeDetectorBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.TEMPORAL_SURGE_DETECTOR_BLOCK_ENTITY.get(), pos, state);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) {
            level.getCapability(CapabilityRegistry.MANIFOLDING_CAPABILITY).ifPresent(cap -> cap.addDetector(worldPosition));
        }
    }

    @Override
    public void setRemoved() {
        if (level != null && !level.isClientSide) {
            level.getCapability(CapabilityRegistry.MANIFOLDING_CAPABILITY).ifPresent(cap -> cap.removeDetector(worldPosition));
        }
        super.setRemoved();
    }

    public void pulse() {
        if (level != null && !level.isClientSide) {
            BlockState state = getBlockState();

            if (state.getBlock() instanceof TemporalSurgeDetectorBlock) {
                level.setBlock(worldPosition, state.setValue(TemporalSurgeDetectorBlock.POWERED, true), 3);
                level.scheduleTick(worldPosition, this.getBlockState().getBlock(), 2);
            }
        }
    }
}
