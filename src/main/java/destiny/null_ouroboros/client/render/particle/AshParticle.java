package destiny.null_ouroboros.client.render.particle;

import destiny.null_ouroboros.server.util.ModUtil;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import org.jetbrains.annotations.Nullable;

public class AshParticle extends TextureSheetParticle {
    private final SpriteSet sprites;
    private final boolean rollDirection;
    private final float rollOffset;

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
        } else {
            int sprite = this.age / (this.lifetime / 3);
            this.setSprite(sprites.get(sprite, 3));
            this.move(0, this.yd, 0);
            this.quadSize -= 0.1f / this.lifetime;
            this.rCol -= 0.1f / this.lifetime;
            this.gCol -= 0.2f / this.lifetime;
            this.bCol -= 0.2f / this.lifetime;
            if (this.rollDirection) {
                this.roll += (1.5f + this.rollOffset) / this.lifetime;
            } else {
                this.roll -= (1.5f + this.rollOffset) / this.lifetime;
            }
        }
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