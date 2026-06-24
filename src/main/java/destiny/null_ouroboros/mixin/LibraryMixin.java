package destiny.null_ouroboros.mixin;

import com.mojang.blaze3d.audio.Library;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Library.class)
public class LibraryMixin {
    private static final int CHANNEL_COUNT = 256;
    private static final int STREAMING_CHANNEL_COUNT = 64;

    @Inject(method = "getChannelCount", at = @At("RETURN"), cancellable = true)
    private void boostChannelCount(CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(CHANNEL_COUNT);
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void boostStreamingChannelPool(CallbackInfo ci) {
        Library library = (Library) (Object) this;
        library.streamingChannels = new Library.CountingChannelPool(STREAMING_CHANNEL_COUNT);
        destiny.null_ouroboros.NullOuroboros.LOGGER.info("Successfully boosted streaming channel pool to " + STREAMING_CHANNEL_COUNT);
    }
}
