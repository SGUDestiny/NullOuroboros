package destiny.null_ouroboros.mixin;

import destiny.null_ouroboros.client.render.dimension.VergeOfRealityDimensionEffects;
import destiny.null_ouroboros.client.render.particle.AshParticle;
import destiny.null_ouroboros.server.capability.ClientManifoldingHolder;
import destiny.null_ouroboros.server.entity.BurrowBeaconEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import static destiny.null_ouroboros.client.render.particle.AshParticle.BEACON_RADIUS;

@Mixin(Particle.class)
public class ParticleMixin {
    @Shadow
    protected double x;
    @Shadow
    protected double y;
    @Shadow
    protected double z;

    @ModifyVariable(method = "move(DDD)V", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private double applyWindDx(double dx) {
        Particle self = (Particle)(Object)this;
        if (self instanceof AshParticle || isNearBurrowBeacon()) return dx;

        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || !VergeOfRealityDimensionEffects.isVergeOfReality(level)) return dx;

        float strength = ClientManifoldingHolder.getWindStrength();
        if (strength <= 0) return dx;

        float angle = ClientManifoldingHolder.getWindAngle();
        double rad = Math.toRadians(angle);
        return dx - Math.sin(rad) * strength;
    }

    @ModifyVariable(method = "move(DDD)V", at = @At("HEAD"), argsOnly = true, ordinal = 1)
    private double applyWindDy(double dy) {
        Particle self = (Particle)(Object)this;
        if (self instanceof AshParticle || isNearBurrowBeacon()) return dy;

        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || !VergeOfRealityDimensionEffects.isVergeOfReality(level)) return dy;

        float strength = ClientManifoldingHolder.getWindStrength();
        if (strength <= 0) return dy;

        return dy * (1 - strength);
    }

    @ModifyVariable(method = "move(DDD)V", at = @At("HEAD"), argsOnly = true, ordinal = 2)
    private double applyWindDz(double dz) {
        Particle self = (Particle)(Object)this;
        if (self instanceof AshParticle || isNearBurrowBeacon()) return dz;

        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || !VergeOfRealityDimensionEffects.isVergeOfReality(level)) return dz;

        float strength = ClientManifoldingHolder.getWindStrength();
        if (strength <= 0) return dz;

        float angle = ClientManifoldingHolder.getWindAngle();
        double rad = Math.toRadians(angle);
        return dz + Math.cos(rad) * strength;
    }

    private boolean isNearBurrowBeacon() {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return false;
        Particle self = (Particle)(Object)this;

        for (BurrowBeaconEntity beacon : level.getEntitiesOfClass(BurrowBeaconEntity.class, self.getBoundingBox().inflate(BEACON_RADIUS))) {
            if (beacon.distanceToSqr(this.x, this.y, this.z) <= BEACON_RADIUS * BEACON_RADIUS) {
                return true;
            }
        }

        return false;
    }
}