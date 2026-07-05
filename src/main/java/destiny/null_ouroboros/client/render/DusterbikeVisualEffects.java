package destiny.null_ouroboros.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import destiny.null_ouroboros.client.render.model.DusterbikeEntityModel;
import destiny.null_ouroboros.client.render.particle.TintedSmokeParticle;
import destiny.null_ouroboros.common.DusterbikeTransforms;
import destiny.null_ouroboros.common.dusterbike.DusterbikePartType;
import destiny.null_ouroboros.server.entity.DusterbikeEntity;
import destiny.null_ouroboros.server.registry.EntityRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;

public final class DusterbikeVisualEffects {
    private static final int EXHAUST_PARTICLE_INTERVAL_TICKS = 2;
    private static final double EXHAUST_PARTICLE_SPEED = 0.04D;

    private static DusterbikeEntityModel model;

    private DusterbikeVisualEffects() {}

    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) {
            return;
        }

        float partialTick = minecraft.getFrameTime();
        for (Entity entity : level.entitiesForRendering()) {
            if (entity instanceof DusterbikeEntity bike && bike.getType() == EntityRegistry.DUSTERBIKE.get()) {
                tickBike(level, bike, partialTick);
            }
        }
    }

    private static void tickBike(ClientLevel level, DusterbikeEntity bike, float partialTick) {
        if (bike.tickCount % EXHAUST_PARTICLE_INTERVAL_TICKS != 0) {
            return;
        }

        Vec3 entityPos = bike.getPosition(partialTick);
        float yaw = bike.getRenderYaw(partialTick);
        float pitch = bike.getRenderPitch(partialTick);
        float roll = bike.getRenderRoll(partialTick);

        if (bike.isEngineRunning()) {
            DusterbikeEntityModel.ExhaustTips tips = getModel().computeExhaustTipEntityLocals(bike, partialTick);
            Vec3 upperWorld = DusterbikeTransforms.worldPointFromLocal(entityPos, yaw, pitch, roll, tips.upper());
            Vec3 lowerWorld = DusterbikeTransforms.worldPointFromLocal(entityPos, yaw, pitch, roll, tips.lower());

            float yawRad = yaw * Mth.DEG_TO_RAD;
            double backX = Mth.sin(yawRad) * EXHAUST_PARTICLE_SPEED;
            double backZ = -Mth.cos(yawRad) * EXHAUST_PARTICLE_SPEED;

            spawnExhaustParticle(level, upperWorld, backX, backZ, bike);
            spawnExhaustParticle(level, lowerWorld, backX, backZ, bike);
        }

        spawnDamageSmoke(level, bike, entityPos, yaw, pitch, roll);
    }

    private static void spawnDamageSmoke(ClientLevel level, DusterbikeEntity bike, Vec3 entityPos, float yaw, float pitch, float roll) {
        int health = bike.getFrameHealth();
        if (health >= 50) {
            return;
        }
        Vec3 engineWorld = DusterbikeTransforms.worldPointFromLocal(entityPos, yaw, pitch, roll, new Vec3(0.0D, 0.7D, 0.0D));
        float yawRad = yaw * Mth.DEG_TO_RAD;
        double sideX = Mth.cos(yawRad) * 0.035D;
        double sideZ = Mth.sin(yawRad) * 0.035D;
        int particles = 1 + (50 - health) / 10;
        for (int i = 0; i < particles; i++) {
            double direction = (i & 1) == 0 ? 1.0D : -1.0D;
            spawnExhaustParticle(level, engineWorld, sideX * direction, sideZ * direction, bike);
        }
    }

    private static void spawnExhaustParticle(ClientLevel level, Vec3 position, double backX, double backZ, DusterbikeEntity bike) {
        double jitter = 0.01D;
        double x = position.x;
        double y = position.y;
        double z = position.z;
        double dx = backX + (level.random.nextDouble() - 0.5D) * jitter;
        double dy = 0.005D + level.random.nextDouble() * 0.01D;
        double dz = backZ + (level.random.nextDouble() - 0.5D) * jitter;

        Integer glowColor = bike.getPartGlowColor(DusterbikePartType.FRAME);
        if (glowColor != null) {
            level.addParticle(new TintedSmokeParticle.ColoredParticleOptions(glowColor), x, y, z, dx, dy, dz);
        } else {
            level.addParticle(ParticleTypes.SMOKE, x, y, z, dx, dy, dz);
        }
    }

    private static DusterbikeEntityModel getModel() {
        if (model == null) {
            model = new DusterbikeEntityModel(
                    Minecraft.getInstance().getEntityModels().bakeLayer(DusterbikeEntityModel.LAYER_LOCATION));
        }
        return model;
    }
}
