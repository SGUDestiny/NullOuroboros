package destiny.null_ouroboros.client.render.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import org.jetbrains.annotations.Nullable;

public class LeviathanBloodParticle extends TextureSheetParticle {
    private final SpriteSet sprites;

    protected LeviathanBloodParticle(ClientLevel level, double x, double y, double z,
                                     double xd, double yd, double zd, SpriteSet sprites) {
        super(level, x, y, z, xd, yd, zd);
        this.sprites = sprites;
        this.lifetime = 40 + level.random.nextInt(20);
        this.gravity = 0.35F;
        this.friction = 0.92F;
        this.xd = xd;
        this.yd = yd;
        this.zd = zd;
        this.quadSize = 0.2F + level.random.nextFloat() * 0.15F;
        this.rCol = 0.55F;
        this.gCol = 0.05F;
        this.bCol = 0.08F;
        this.setSpriteFromAge(sprites);
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (this.age++ >= this.lifetime) {
            this.remove();
            return;
        }
        this.setSpriteFromAge(this.sprites);
        this.yd -= 0.04D * this.gravity;
        this.move(this.xd, this.yd, this.zd);
        this.xd *= this.friction;
        this.yd *= this.friction;
        this.zd *= this.friction;
        if (this.onGround) {
            this.xd *= 0.7D;
            this.zd *= 0.7D;
        }
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Nullable
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                                       double xd, double yd, double zd) {
            return new LeviathanBloodParticle(level, x, y, z, xd, yd, zd, this.sprites);
        }
    }
}

