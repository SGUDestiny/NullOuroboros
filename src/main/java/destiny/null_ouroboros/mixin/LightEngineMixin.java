package destiny.null_ouroboros.mixin;

import destiny.null_ouroboros.common.light.DusterbikeHeadlightManager;
import destiny.null_ouroboros.common.light.RedstickLightManager;
import destiny.null_ouroboros.server.block.BulkheadBlock;
import destiny.null_ouroboros.server.block.IntakeFanBlock;
import destiny.null_ouroboros.server.block.OutputVentBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LightChunkGetter;
import net.minecraft.world.level.lighting.BlockLightEngine;
import net.minecraft.world.level.lighting.LightEngine;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LightEngine.class)
public abstract class LightEngineMixin {
    @Shadow
    @Final
    private LightChunkGetter chunkSource;

    @Shadow
    public abstract int getLightValue(BlockPos pos);

    @Unique
    private static final ThreadLocal<Boolean> null_ouroboros$reentrant = ThreadLocal.withInitial(() -> false);

    @Inject(method = "getLightValue(Lnet/minecraft/core/BlockPos;)I", at = @At("RETURN"), cancellable = true)
    private void nullOuroboros$customLightValues(BlockPos pos, CallbackInfoReturnable<Integer> cir) {
        BlockGetter blockGetter = this.chunkSource.getLevel();
        int value = cir.getReturnValue();

        value = Math.max(value, null_ouroboros$opaqueBlockAmbient(blockGetter, pos, value));

        if ((Object) this instanceof BlockLightEngine && blockGetter instanceof Level level) {
            value = Math.max(value, RedstickLightManager.getBlockLightContribution(level, pos));
            value = Math.max(value, DusterbikeHeadlightManager.getBlockLightContribution(level, pos));
        }

        if (value > cir.getReturnValue()) {
            cir.setReturnValue(value);
        }
    }

    @Unique
    private int null_ouroboros$opaqueBlockAmbient(BlockGetter level, BlockPos pos, int current) {
        if (null_ouroboros$reentrant.get()) {
            return current;
        }

        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();
        if (block instanceof BulkheadBlock) {
            if (BulkheadBlock.isPassable(state)) {
                return current;
            }
            null_ouroboros$reentrant.set(true);
            try {
                Direction facing = state.getValue(BulkheadBlock.FACING);
                return Math.max(current, Math.max(
                        this.getLightValue(pos.relative(facing)),
                        this.getLightValue(pos.relative(facing.getOpposite()))));
            } finally {
                null_ouroboros$reentrant.set(false);
            }
        }

        if (!(block instanceof IntakeFanBlock || block instanceof OutputVentBlock)) {
            return current;
        }

        null_ouroboros$reentrant.set(true);
        try {
            int ambient = current;
            for (Direction direction : Direction.values()) {
                ambient = Math.max(ambient, this.getLightValue(pos.relative(direction)));
            }
            return ambient;
        } finally {
            null_ouroboros$reentrant.set(false);
        }
    }
}
