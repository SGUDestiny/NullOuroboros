package destiny.null_ouroboros.mixin;

import destiny.null_ouroboros.common.dimension.VergeOfRealityDimension;
import destiny.null_ouroboros.client.render.particle.AshParticle;
import destiny.null_ouroboros.server.capability.ClientManifoldingHolder;
import destiny.null_ouroboros.server.capability.ManifoldingCapability;
import destiny.null_ouroboros.server.entity.BurrowBeaconEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import destiny.null_ouroboros.manifolding.ManifoldingWindScan;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import static destiny.null_ouroboros.server.capability.ManifoldingCapability.BEACON_PROTECTION_RANGE;

@Mixin(Particle.class)
public class ParticleMixin {
    @Shadow
    protected double x;
    @Shadow
    protected double y;
    @Shadow
    protected double z;
    @Shadow
    @Final
    protected ClientLevel level;

    @ModifyVariable(method = "move(DDD)V", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private double applyWindDx(double dx) {
        Particle self = (Particle)(Object)this;
        if (self instanceof AshParticle || isNearBurrowBeacon() || isShelteredByBlock(level)) return dx;

        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || !VergeOfRealityDimension.isVergeOfReality(level)) return dx;

        float strength = ClientManifoldingHolder.getWindStrength();
        if (strength <= 0) return dx;

        float angle = ClientManifoldingHolder.getWindAngle();
        double rad = Math.toRadians(angle);
        return dx - Math.sin(rad) * strength;
    }

    @ModifyVariable(method = "move(DDD)V", at = @At("HEAD"), argsOnly = true, ordinal = 1)
    private double applyWindDy(double dy) {
        Particle self = (Particle)(Object)this;
        if (self instanceof AshParticle || isNearBurrowBeacon() || isShelteredByBlock(level)) return dy;

        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || !VergeOfRealityDimension.isVergeOfReality(level)) return dy;

        float strength = ClientManifoldingHolder.getWindStrength();
        if (strength <= 0) return dy;

        return dy * (1 - strength);
    }

    @ModifyVariable(method = "move(DDD)V", at = @At("HEAD"), argsOnly = true, ordinal = 2)
    private double applyWindDz(double dz) {
        Particle self = (Particle)(Object)this;
        if (self instanceof AshParticle || isNearBurrowBeacon() || isShelteredByBlock(level)) return dz;

        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || !VergeOfRealityDimension.isVergeOfReality(level)) return dz;

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

        for (BurrowBeaconEntity beacon : level.getEntitiesOfClass(BurrowBeaconEntity.class, self.getBoundingBox().inflate(BEACON_PROTECTION_RANGE))) {
            if (beacon.isProvidingProtection() && beacon.distanceToSqr(this.x, this.y, this.z) <= BEACON_PROTECTION_RANGE * BEACON_PROTECTION_RANGE) {
                return true;
            }
        }

        return false;
    }

    private boolean isShelteredByBlock(ClientLevel level) {
        float strength = ClientManifoldingHolder.getWindStrength();
        if (strength <= 0) return false;

        BlockPos currentPos = BlockPos.containing(this.x, this.y, this.z);
        BlockState currentState = level.getBlockState(currentPos);

        if (!currentState.is(ManifoldingCapability.DOESNT_PROTECT_FROM_MANIFOLDING) && !currentState.getCollisionShape(level, currentPos).isEmpty()) {
            return true;
        }

        float angle = ClientManifoldingHolder.getWindAngle();
        double rad = Math.toRadians(angle + 180);
        double dx = -Math.sin(rad);
        double dz = Math.cos(rad);

        Vec3 from = new Vec3(this.x + dx * 0.1, this.y, this.z + dz * 0.1);
        Vec3 direction = new Vec3(dx, 0, dz);
        return !ManifoldingWindScan.isExposedToWind(level, from, direction);
    }
}