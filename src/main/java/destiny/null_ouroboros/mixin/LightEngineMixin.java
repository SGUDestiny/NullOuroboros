package destiny.null_ouroboros.mixin;

import destiny.null_ouroboros.common.light.DusterbikeHeadlightManager;
import destiny.null_ouroboros.common.light.RedstickLightManager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LightChunkGetter;
import net.minecraft.world.level.lighting.BlockLightEngine;
import net.minecraft.world.level.lighting.LightEngine;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LightEngine.class)
public abstract class LightEngineMixin {
    @Shadow
    @Final
    private LightChunkGetter chunkSource;

    @Inject(method = "getLightValue(Lnet/minecraft/core/BlockPos;)I", at = @At("RETURN"), cancellable = true)
    private void nullOuroboros$redstickBlockLight(BlockPos pos, CallbackInfoReturnable<Integer> cir) {
        if (!((Object) this instanceof BlockLightEngine)) return;
        BlockGetter blockGetter = this.chunkSource.getLevel();
        if (!(blockGetter instanceof Level level)) return;

        int contribution = RedstickLightManager.getBlockLightContribution(level, pos);
        int headlightContribution = DusterbikeHeadlightManager.getBlockLightContribution(level, pos);
        contribution = Math.max(contribution, headlightContribution);

        if (contribution > cir.getReturnValue()) {
            cir.setReturnValue(contribution);
        }
    }
}
