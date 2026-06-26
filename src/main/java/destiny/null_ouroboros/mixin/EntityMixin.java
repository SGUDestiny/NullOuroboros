package destiny.null_ouroboros.mixin;

import destiny.null_ouroboros.client.render.dimension.VergeOfRealityDimensionEffects;
import destiny.null_ouroboros.server.capability.ClientManifoldingHolder;
import destiny.null_ouroboros.server.capability.ManifoldingCapability;
import destiny.null_ouroboros.server.capability.ManifoldingPhase;
import destiny.null_ouroboros.server.entity.BurrowBeaconEntity;
import destiny.null_ouroboros.manifolding.ManifoldingWindScan;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public class EntityMixin {
    @Inject(method = "tick", at = @At("RETURN"))
    private void nullOuroboros$applyManifoldingWind(CallbackInfo ci) {
        Entity entity = (Entity) (Object) this;
        if (!(entity.level() instanceof ClientLevel level)) return;
        if (entity instanceof LocalPlayer) return;
        if (!VergeOfRealityDimensionEffects.isVergeOfReality(level)) return;
        if (ClientManifoldingHolder.getPhase() == ManifoldingPhase.CLEAR) return;
        if (entity.isSpectator()) return;
        if (entity instanceof BurrowBeaconEntity) return;
        if (entity instanceof Player player && player.isCreative()) return;

        float strength = ClientManifoldingHolder.getWindStrength();
        if (strength <= 0) return;

        float windAngle = ClientManifoldingHolder.getWindAngle();
        Vec3 checkDirection = Vec3.directionFromRotation(0, windAngle + 180).normalize();
        Vec3 checkOrigin = ManifoldingCapability.getEntityCheckOrigin(entity);
        if (!ManifoldingWindScan.isExposedToWind(level, checkOrigin, checkDirection)) return;

        double pushMultiplier = ManifoldingCapability.isNearBurrowBeacon(entity, level)
                ? ManifoldingCapability.BEACON_PUSH_MULTIPLIER
                : 1.0;
        Vec3 windOffset = ManifoldingCapability.computeWindOffset(strength, windAngle, pushMultiplier);
        ManifoldingCapability.applyWindMovement(entity, windOffset);
    }
}
