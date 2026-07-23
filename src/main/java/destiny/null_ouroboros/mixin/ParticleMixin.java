package destiny.null_ouroboros.mixin;

import destiny.null_ouroboros.client.render.particle.AshParticle;
import destiny.null_ouroboros.common.dimension.VergeOfRealityDimension;
import destiny.null_ouroboros.manifolding.BurrowBeaconProximity;
import destiny.null_ouroboros.manifolding.ManifoldingWindScan;
import destiny.null_ouroboros.server.capability.ClientManifoldingHolder;
import destiny.null_ouroboros.server.capability.ManifoldingCapability;
import destiny.null_ouroboros.server.capability.ManifoldingPhase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

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

    @Unique
    private boolean null_ouroboros$windEvalValid;
    @Unique
    private boolean null_ouroboros$applyWind;
    @Unique
    private float null_ouroboros$windStrength;
    @Unique
    private float null_ouroboros$windAngle;

    @ModifyVariable(method = "move(DDD)V", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private double applyWindDx(double dx) {
        null_ouroboros$evaluateWind();
        if (!null_ouroboros$applyWind) {
            return dx;
        }
        double rad = Math.toRadians(null_ouroboros$windAngle);
        return dx - Math.sin(rad) * null_ouroboros$windStrength;
    }

    @ModifyVariable(method = "move(DDD)V", at = @At("HEAD"), argsOnly = true, ordinal = 1)
    private double applyWindDy(double dy) {
        null_ouroboros$evaluateWind();
        if (!null_ouroboros$applyWind) {
            return dy;
        }
        return dy * (1.0 - null_ouroboros$windStrength);
    }

    @ModifyVariable(method = "move(DDD)V", at = @At("HEAD"), argsOnly = true, ordinal = 2)
    private double applyWindDz(double dz) {
        null_ouroboros$evaluateWind();
        boolean apply = null_ouroboros$applyWind;
        float strength = null_ouroboros$windStrength;
        float angle = null_ouroboros$windAngle;
        null_ouroboros$windEvalValid = false;
        if (!apply) {
            return dz;
        }
        double rad = Math.toRadians(angle);
        return dz + Math.cos(rad) * strength;
    }

    @Unique
    private void null_ouroboros$evaluateWind() {
        if (null_ouroboros$windEvalValid) {
            return;
        }
        null_ouroboros$windEvalValid = true;
        null_ouroboros$applyWind = false;

        Particle self = (Particle) (Object) this;
        if (self instanceof AshParticle) {
            return;
        }

        float strength = ClientManifoldingHolder.getWindStrength();
        if (strength <= 0) {
            return;
        }
        if (ClientManifoldingHolder.getPhase() == ManifoldingPhase.CLEAR) {
            return;
        }

        ClientLevel clientLevel = Minecraft.getInstance().level;
        if (clientLevel == null || !VergeOfRealityDimension.isVergeOfReality(clientLevel)) {
            return;
        }

        if (BurrowBeaconProximity.isNear(clientLevel, this.x, this.y, this.z)) {
            return;
        }
        if (null_ouroboros$isShelteredByBlock(clientLevel)) {
            return;
        }

        null_ouroboros$applyWind = true;
        null_ouroboros$windStrength = strength;
        null_ouroboros$windAngle = ClientManifoldingHolder.getWindAngle();
    }

    @Unique
    private boolean null_ouroboros$isShelteredByBlock(ClientLevel clientLevel) {
        BlockPos currentPos = BlockPos.containing(this.x, this.y, this.z);
        BlockState currentState = clientLevel.getBlockState(currentPos);

        if (!currentState.is(ManifoldingCapability.DOESNT_PROTECT_FROM_MANIFOLDING)
                && !currentState.getCollisionShape(clientLevel, currentPos).isEmpty()) {
            return true;
        }

        float angle = ClientManifoldingHolder.getWindAngle();
        double rad = Math.toRadians(angle + 180);
        double dx = -Math.sin(rad);
        double dz = Math.cos(rad);

        Vec3 from = new Vec3(this.x + dx * 0.1, this.y, this.z + dz * 0.1);
        Vec3 direction = new Vec3(dx, 0, dz);
        return !ManifoldingWindScan.isExposedToWind(clientLevel, from, direction);
    }
}
