package destiny.null_ouroboros.client.render.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class BloodParticle extends TextureSheetParticle {
    private static final int TRAIL_LENGTH = 50;

    private final SpriteSet sprites;
    private final Vec3[] trail = new Vec3[TRAIL_LENGTH];
    private final Vec3[] trailOld = new Vec3[TRAIL_LENGTH];
    private boolean trailInitialized;

    protected BloodParticle(ClientLevel level, double x, double y, double z,
                            double xd, double yd, double zd, SpriteSet sprites) {
        super(level, x, y, z, xd, yd, zd);
        this.sprites = sprites;
        this.lifetime = 40 + level.random.nextInt(20);
        this.gravity = 0.8F;
        this.friction = 0.94F;
        this.xd = xd;
        this.yd = yd;
        this.zd = zd;
        this.quadSize = 0.2F + level.random.nextFloat() * 0.15F;
        this.setSpriteFromAge(sprites);
    }

    private double trailSpacing() {
        return Math.max(0.05D, this.quadSize * 0.45D);
    }

    private void initTrail(Vec3 head) {
        for (int i = 0; i < TRAIL_LENGTH; i++) {
            this.trail[i] = head;
            this.trailOld[i] = head;
        }
        this.trailInitialized = true;
    }

    private void updateTrail() {
        Vec3 head = new Vec3(this.x, this.y, this.z);
        if (!this.trailInitialized) {
            initTrail(head);
            return;
        }

        for (int i = 0; i < TRAIL_LENGTH; i++) {
            this.trailOld[i] = this.trail[i];
        }

        double spacing = trailSpacing();
        Vec3 prev = head;
        Vec3 fallbackDir = new Vec3(this.xd, this.yd, this.zd);
        if (fallbackDir.lengthSqr() < 1.0E-8D) {
            fallbackDir = new Vec3(0.0D, -1.0D, 0.0D);
        } else {
            fallbackDir = fallbackDir.normalize();
        }

        for (int i = 0; i < TRAIL_LENGTH; i++) {
            Vec3 point = this.trail[i];
            Vec3 away = point.subtract(prev);
            if (away.lengthSqr() < 1.0E-8D) {
                away = fallbackDir.scale(-1.0D);
            } else {
                away = away.normalize();
            }
            this.trail[i] = prev.add(away.scale(spacing));
            prev = this.trail[i];
        }
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
        if (this.onGround) {
            this.remove();
            return;
        }
        this.xd *= this.friction;
        this.yd *= this.friction;
        this.zd *= this.friction;
        updateTrail();
    }

    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTicks) {
        float baseSize = this.quadSize;
        float baseAlpha = this.alpha;
        double savedX = this.x;
        double savedY = this.y;
        double savedZ = this.z;
        double savedXo = this.xo;
        double savedYo = this.yo;
        double savedZo = this.zo;

        super.render(buffer, camera, partialTicks);

        if (!this.trailInitialized) {
            return;
        }

        for (int i = 0; i < TRAIL_LENGTH; i++) {
            float t = (i + 1) / (float) TRAIL_LENGTH;
            float fade = 1.0F - t;
            Vec3 oldPos = this.trailOld[i];
            Vec3 newPos = this.trail[i];
            double px = Mth.lerp(partialTicks, oldPos.x, newPos.x);
            double py = Mth.lerp(partialTicks, oldPos.y, newPos.y);
            double pz = Mth.lerp(partialTicks, oldPos.z, newPos.z);
            this.quadSize = baseSize * fade;
            this.alpha = baseAlpha;
            this.x = this.xo = px;
            this.y = this.yo = py;
            this.z = this.zo = pz;
            super.render(buffer, camera, 0.0F);
        }

        this.quadSize = baseSize;
        this.alpha = baseAlpha;
        this.x = savedX;
        this.y = savedY;
        this.z = savedZ;
        this.xo = savedXo;
        this.yo = savedYo;
        this.zo = savedZo;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    public boolean shouldCull() {
        return false;
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
            return new BloodParticle(level, x, y, z, xd, yd, zd, this.sprites);
        }
    }
}
