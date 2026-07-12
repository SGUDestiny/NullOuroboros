package destiny.null_ouroboros.mixin;

import destiny.null_ouroboros.common.dimension.VergeOfRealityDimension;
import destiny.null_ouroboros.server.capability.ClientManifoldingHolder;
import destiny.null_ouroboros.server.capability.ManifoldingCapability;
import destiny.null_ouroboros.server.capability.ManifoldingPhase;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public class LocalPlayerMixin {
    @Inject(method = "tick", at = @At("RETURN"))
    private void applyManifoldingWind(CallbackInfo ci) {
        LocalPlayer player = (LocalPlayer) (Object) this;
        ClientLevel level = player.clientLevel;

        if (!VergeOfRealityDimension.isVergeOfReality(level)) return;
        if (ClientManifoldingHolder.getPhase() == ManifoldingPhase.CLEAR) return;
        if (player.isCreative() || player.isSpectator()) return;
        if (!ClientManifoldingHolder.isExposed()) return;

        float strength = ClientManifoldingHolder.getWindStrength();
        if (strength <= 0) return;

        double pushMultiplier = ManifoldingCapability.isNearBurrowBeacon(player, level) ? ManifoldingCapability.BEACON_PUSH_MULTIPLIER : 1.0;
        Vec3 windOffset = ManifoldingCapability.computeWindOffset(strength, ClientManifoldingHolder.getWindAngle(), pushMultiplier);
        ManifoldingCapability.applyWindMovement(player, windOffset);
    }
}
