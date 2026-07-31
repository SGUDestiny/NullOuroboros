package destiny.null_ouroboros.mixin;

import destiny.null_ouroboros.common.dimension.VergeOfRealityDimension;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.SleepStatus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(ServerLevel.class)
public class ServerLevelMixin {
    @Redirect(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/players/SleepStatus;areEnoughDeepSleeping(ILjava/util/List;)Z"
            )
    )
    private boolean nullOuroboros$neverFinishSleepOnVerge(SleepStatus sleepStatus, int percentage, List<ServerPlayer> players) {
        ServerLevel self = (ServerLevel) (Object) this;
        if (VergeOfRealityDimension.isVergeOfReality(self)) {
            return false;
        }
        return sleepStatus.areEnoughDeepSleeping(percentage, players);
    }

    @Inject(method = "announceSleepStatus", at = @At("HEAD"), cancellable = true)
    private void nullOuroboros$suppressSleepStatusOnVerge(CallbackInfo ci) {
        ServerLevel self = (ServerLevel) (Object) this;
        if (VergeOfRealityDimension.isVergeOfReality(self)) {
            ci.cancel();
        }
    }
}
