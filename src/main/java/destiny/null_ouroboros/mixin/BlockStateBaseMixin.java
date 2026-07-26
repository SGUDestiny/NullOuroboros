package destiny.null_ouroboros.mixin;

import destiny.null_ouroboros.server.fuse.FusePowerTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class BlockStateBaseMixin {
    @Inject(method = "getSignal", at = @At("RETURN"), cancellable = true)
    private void nullOuroboros$fuseGetSignal(BlockGetter level, BlockPos pos, Direction direction,
                                             CallbackInfoReturnable<Integer> cir) {
        if (cir.getReturnValue() > 0) {
            return;
        }
        if (!(level instanceof Level world)) {
            return;
        }
        BlockPos poweredPos = pos.relative(direction.getOpposite());
        if (FusePowerTracker.isPowered(world, poweredPos)) {
            cir.setReturnValue(15);
        }
    }
}
