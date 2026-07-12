package destiny.null_ouroboros.client.render.particle;

import destiny.null_ouroboros.server.particle.ColoredParticleOptions;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

public class TintedSmokeParticle extends TextureSheetParticle {
    private final SpriteSet sprites;

    public TintedSmokeParticle(ClientLevel level, double x, double y, double z, double dx, double dy, double dz, int color, SpriteSet sprites) {
        super(level, x, y, z, 0.0F, 0.0F, 0.0F);
        this.sprites = sprites;

        this.friction = 0.96F;
        this.gravity = -0.05F;
        this.speedUpWhenYMotionIsBlocked = true;
        this.hasPhysics = false;

        this.xd = dx;
        this.yd = dy;
        this.zd = dz;

        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;
        this.setColor(r, g, b);

        this.quadSize *= 0.75F;
        this.lifetime = (int) (8.0D / (level.random.nextDouble() * 0.8D + 0.2D));
        this.lifetime = Math.max(this.lifetime, 1);

        this.setSpriteFromAge(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        this.setSpriteFromAge(this.sprites);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
    }

    @Override
    public int getLightColor(float p_107086_) {
        int $$1 = super.getLightColor(p_107086_);
        int $$2 = 240;
        int $$3 = $$1 >> 16 & 255;
        return 240 | $$3 << 16;
    }

    @Override
    public float getQuadSize(float partialTicks) {
        return this.quadSize * Mth.clamp(((float)this.age + partialTicks) / (float)this.lifetime * 32.0F, 0.0F, 1.0F);
    }

    public static class Provider implements ParticleProvider<ColoredParticleOptions> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Nullable
        @Override
        public Particle createParticle(ColoredParticleOptions options, ClientLevel level, double x, double y, double z, double dx, double dy, double dz) {
            return new TintedSmokeParticle(level, x, y, z, dx, dy, dz, options.getColor(), this.sprites);
        }
    }
}