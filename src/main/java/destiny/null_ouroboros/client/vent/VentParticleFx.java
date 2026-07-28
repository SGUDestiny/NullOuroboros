package destiny.null_ouroboros.client.vent;

import destiny.null_ouroboros.common.dimension.VergeOfRealityDimension;
import destiny.null_ouroboros.server.ash.AshAirtight;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import org.joml.Vector3f;

public final class VentParticleFx {
    private static final DustParticleOptions WHITE = new DustParticleOptions(new Vector3f(1.0F, 1.0F, 1.0F), 1.0F);
    private static final DustParticleOptions BLACK = new DustParticleOptions(new Vector3f(0.08F, 0.08F, 0.08F), 1.2F);
    private static final int PARTICLES_PER_FACE = 2;

    private VentParticleFx() {
    }

    public static void spawnIntakeWhirlwind(Level level, BlockPos pos, float speed) {
        if (!VergeOfRealityDimension.isVergeOfReality(level) || speed <= 0.05F) {
            return;
        }
        RandomSource random = level.random;
        for (Direction face : Direction.values()) {
            if (!AshAirtight.isOpenAirCell(level, pos.relative(face))) {
                continue;
            }
            for (int i = 0; i < PARTICLES_PER_FACE; i++) {
                if (random.nextFloat() > 0.7F * speed) {
                    continue;
                }
                spawnVortexParticle(level, pos, face, speed, random, ParticleTypes.SMOKE, true);
                if (random.nextBoolean()) {
                    spawnVortexParticle(level, pos, face, speed, random, BLACK, true);
                }
            }
        }
    }

    public static void spawnOutletWhirlwind(Level level, BlockPos pos, float speed, boolean clean) {
        if (!VergeOfRealityDimension.isVergeOfReality(level) || speed <= 0.05F) {
            return;
        }
        RandomSource random = level.random;
        ParticleOptions primary = clean ? WHITE : BLACK;
        for (Direction face : Direction.values()) {
            if (!AshAirtight.isOpenAirCell(level, pos.relative(face))) {
                continue;
            }
            for (int i = 0; i < PARTICLES_PER_FACE; i++) {
                if (random.nextFloat() > 0.7F * speed) {
                    continue;
                }
                spawnVortexParticle(level, pos, face, speed, random, primary, false);
                if (!clean && random.nextBoolean()) {
                    spawnVortexParticle(level, pos, face, speed * 0.85F, random, ParticleTypes.SMOKE, false);
                }
            }
        }
    }

    private static void spawnVortexParticle(
            Level level,
            BlockPos pos,
            Direction face,
            float speed,
            RandomSource random,
            ParticleOptions particle,
            boolean inbound
    ) {
        Direction.Axis axis = face.getAxis();
        double axialOffset = inbound
                ? 0.70 + random.nextDouble() * 0.40
                : 0.45 + random.nextDouble() * 0.15;
        double cx = pos.getX() + 0.5 + face.getStepX() * axialOffset;
        double cy = pos.getY() + 0.5 + face.getStepY() * axialOffset;
        double cz = pos.getZ() + 0.5 + face.getStepZ() * axialOffset;

        double angle = random.nextDouble() * Math.PI * 2.0;
        double radius = inbound
                ? 0.28 + random.nextDouble() * 0.32
                : 0.20 + random.nextDouble() * 0.28;
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);

        double ox;
        double oy;
        double oz;
        double tx;
        double ty;
        double tz;

        if (axis == Direction.Axis.Y) {
            ox = cos * radius;
            oy = 0;
            oz = sin * radius;
            tx = -sin;
            ty = 0;
            tz = cos;
        } else if (axis == Direction.Axis.X) {
            ox = 0;
            oy = cos * radius;
            oz = sin * radius;
            tx = 0;
            ty = -sin;
            tz = cos;
        } else {
            ox = cos * radius;
            oy = sin * radius;
            oz = 0;
            tx = -sin;
            ty = cos;
            tz = 0;
        }

        double swirl = (inbound ? 1.0 : -1.0) * (0.14 + random.nextDouble() * 0.06) * speed;
        double axial = (inbound ? -1.0 : 1.0) * (0.16 + random.nextDouble() * 0.08) * speed;
        double vx = tx * swirl + face.getStepX() * axial;
        double vy = ty * swirl + face.getStepY() * axial;
        double vz = tz * swirl + face.getStepZ() * axial;

        double inwardRadial = inbound ? -0.04 * speed : 0.03 * speed;
        vx += ox * inwardRadial;
        vy += oy * inwardRadial;
        vz += oz * inwardRadial;

        level.addParticle(particle, cx + ox, cy + oy, cz + oz, vx, vy, vz);
    }
}
