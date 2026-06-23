package destiny.null_ouroboros.client.render.particle;

import destiny.null_ouroboros.server.capability.ClientManifoldingHolder;
import destiny.null_ouroboros.server.capability.ManifoldingCapability;
import destiny.null_ouroboros.server.entity.BurrowBeaconEntity;
import destiny.null_ouroboros.server.util.ModUtil;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import static destiny.null_ouroboros.server.capability.ManifoldingCapability.BEACON_PROTECTION_RANGE;

public class AshParticle extends TextureSheetParticle {
    private final SpriteSet sprites;
    private final boolean rollDirection;
    private final float rollOffset;
    private final double baseYd;

    public AshParticle(ClientLevel level, double x, double y, double z, SpriteSet sprite, double xSpeed, double ySpeed, double zSpeed) {
        super(level, x, y, z, 0.0D, 0.0D, 0.0D);
        this.sprites = sprite;
        this.friction = 1f;
        this.lifetime = 160 + level.random.nextInt(-10, 10);
        this.setSpriteFromAge(sprite);
        this.rCol = 1f;
        this.gCol = 1f;
        this.bCol = 1f;
        this.yd = 0.02f + ModUtil.getBoundRandomFloatStatic(level, -0.005f, 0.005f);
        this.baseYd = this.yd;
        this.gravity = 1f;
        this.quadSize = 0.1f;
        this.rollDirection = level.random.nextBoolean();
        this.rollOffset = ModUtil.getBoundRandomFloatStatic(level, -0.1f, 0.1f);
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        this.oRoll = this.roll;

        if (this.age++ >= this.lifetime) {
            this.remove();
            return;
        }

        float windStrength = ClientManifoldingHolder.getWindStrength();
        float windAngle = ClientManifoldingHolder.getWindAngle();
        double windRad = Math.toRadians(windAngle);
        double windX = -Math.sin(windRad) * windStrength * 1;
        double windZ =  Math.cos(windRad) * windStrength * 1;

        double currentYd = baseYd * (1.0 - windStrength);

        if (isNearBurrowBeacon() || isShelteredByBlock()) {
            windX = 0;
            windZ = 0;
            currentYd = baseYd;
            windStrength = 0f;
        }

        this.xd = windX;
        this.zd = windZ;
        this.move(this.xd, currentYd, this.zd);

        this.quadSize -= 0.1f / this.lifetime;
        this.rCol -= 0.1f / this.lifetime;
        this.gCol -= 0.2f / this.lifetime;
        this.bCol -= 0.2f / this.lifetime;

        float spinBoost = 1 + windStrength * 4;
        if (this.rollDirection) {
            this.roll += (1.5f + this.rollOffset) * spinBoost / this.lifetime;
        } else {
            this.roll -= (1.5f + this.rollOffset) * spinBoost / this.lifetime;
        }

        int sprite = this.age / (this.lifetime / 3);
        this.setSprite(sprites.get(sprite, 3));
    }

    private boolean isNearBurrowBeacon() {
        for (BurrowBeaconEntity beacon : this.level.getEntitiesOfClass(BurrowBeaconEntity.class, this.getBoundingBox().inflate(BEACON_PROTECTION_RANGE))) {
            if (beacon.distanceToSqr(this.x, this.y, this.z) <= BEACON_PROTECTION_RANGE * BEACON_PROTECTION_RANGE) {
                return true;
            }
        }
        return false;
    }

    private boolean isShelteredByBlock() {
        float strength = ClientManifoldingHolder.getWindStrength();
        if (strength <= 0) return false;

        BlockPos currentPos = BlockPos.containing(this.x, this.y, this.z);
        BlockState currentState = this.level.getBlockState(currentPos);

        if (!currentState.is(ManifoldingCapability.IGNORED_BLOCKS) && !currentState.getCollisionShape(this.level, currentPos).isEmpty()) {
            return true;
        }

        float angle = ClientManifoldingHolder.getWindAngle();
        double rad = Math.toRadians(angle + 180);
        double dx = -Math.sin(rad);
        double dz = Math.cos(rad);

        Vec3 from = new Vec3(this.x + dx * 0.1, this.y, this.z + dz * 0.1);
        Vec3 direction = new Vec3(dx, 0, dz);
        double maxDist = this.level.canSeeSky(currentPos) ? 3 : 16;
        Vec3 to = from.add(direction.scale(maxDist));

        double stepSize = 0.3;
        double distance = from.distanceTo(to);
        if (distance < 1e-6) return false;

        Vec3 step = direction.scale(stepSize);
        Vec3 current = from;

        for (double traveled = 0; traveled <= distance; traveled += stepSize) {
            BlockPos pos = BlockPos.containing(current);
            BlockState state = this.level.getBlockState(pos);

            if (state.is(ManifoldingCapability.IGNORED_BLOCKS)) {
                current = current.add(step);
                continue;
            }

            var shape = state.getCollisionShape(this.level, pos);
            if (!shape.isEmpty()) {
                BlockHitResult hit = shape.clip(from, to, pos);
                if (hit != null && hit.getType() != HitResult.Type.MISS) {
                    return true;
                }
            }
            current = current.add(step);
        }
        return false;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet spriteSet;

        public Provider(SpriteSet spriteSet) {
            this.spriteSet = spriteSet;
        }

        @Override
        public @Nullable Particle createParticle(SimpleParticleType simpleParticleType, ClientLevel clientLevel, double v, double v1, double v2, double v3, double v4, double v5) {
            return new AshParticle(clientLevel, v, v1, v2, this.spriteSet, v3, v4, v5);
        }
    }
}