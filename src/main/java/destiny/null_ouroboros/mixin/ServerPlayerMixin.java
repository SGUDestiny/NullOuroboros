package destiny.null_ouroboros.mixin;

import destiny.null_ouroboros.server.capability.ManifoldingCapability;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ServerPlayer.class)
public class ServerPlayerMixin {
    @Redirect(
            method = "startSleepInBed",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/dimension/DimensionType;natural()Z")
    )
    private boolean nullOuroboros$allowBedOnVerge(DimensionType dimensionType) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        if (player.level().dimension().location().equals(ManifoldingCapability.DIMENSION_ID)) {
            return true;
        }
        return dimensionType.natural();
    }

    @Redirect(
            method = "startSleepInBed",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;canSleepThroughNights()Z")
    )
    private boolean nullOuroboros$suppressSleepNotPossibleMessage(ServerLevel level) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        if (player.level().dimension().location().equals(ManifoldingCapability.DIMENSION_ID)) {
            return true;
        }
        return level.canSleepThroughNights();
    }
}
