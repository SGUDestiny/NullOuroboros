package destiny.null_ouroboros.mixin;

import com.mojang.blaze3d.audio.Library;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;

import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Library.class)
public class LibraryMixin {
    @Inject(method = "getChannelCount", at = @At("RETURN"), cancellable = true)
    private void boostChannelCount(CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(256);
    }
}
