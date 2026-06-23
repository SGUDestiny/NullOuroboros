package destiny.null_ouroboros.mixin;

import destiny.null_ouroboros.client.render.dimension.VergeOfRealityDimensionEffects;
import destiny.null_ouroboros.server.capability.ClientManifoldingHolder;
import destiny.null_ouroboros.server.capability.ManifoldingCapability;
import destiny.null_ouroboros.server.capability.ManifoldingPhase;
import destiny.null_ouroboros.server.entity.BurrowBeaconEntity;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.MoverType;
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

        if (!VergeOfRealityDimensionEffects.isVergeOfReality(level)) return;
        if (ClientManifoldingHolder.getPhase() == ManifoldingPhase.CLEAR) return;
        if (player.isCreative() || player.isSpectator()) return;
        if (!ClientManifoldingHolder.isExposed()) return;

        float strength = ClientManifoldingHolder.getWindStrength();
        if (strength <= 0) return;

        double pushMultiplier = isNearBurrowBeacon(player, level) ? ManifoldingCapability.BEACON_PUSH_MULTIPLIER : 1.0;
        Vec3 windOffset = ManifoldingCapability.computeWindOffset(strength, ClientManifoldingHolder.getWindAngle(), pushMultiplier);
        
        boolean wasOnGround = player.onGround();
        player.move(MoverType.SELF, windOffset);
        player.setOnGround(wasOnGround);
    }

    private static boolean isNearBurrowBeacon(LocalPlayer player, ClientLevel level) {
        double radiusSq = ManifoldingCapability.BEACON_PROTECTION_RANGE * ManifoldingCapability.BEACON_PROTECTION_RANGE;

        for (BurrowBeaconEntity beacon : level.getEntitiesOfClass(BurrowBeaconEntity.class, player.getBoundingBox().inflate(ManifoldingCapability.BEACON_PROTECTION_RANGE))) {
            if (beacon.isProvidingProtection() && beacon.distanceToSqr(player) <= radiusSq) {
                return true;
            }
        }

        return false;
    }
}
