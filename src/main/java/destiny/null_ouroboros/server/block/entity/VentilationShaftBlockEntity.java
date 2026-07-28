package destiny.null_ouroboros.server.block.entity;

import destiny.null_ouroboros.server.registry.BlockEntityRegistry;
import destiny.null_ouroboros.server.vent.VentNetworkTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class VentilationShaftBlockEntity extends CamouflageBlockEntity {
    public VentilationShaftBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.VENTILATION_SHAFT_BLOCK_ENTITY.get(), pos, state);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) {
            VentNetworkTracker.addDuct(level, worldPosition);
        }
    }
}
