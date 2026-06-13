package destiny.null_ouroboros.client.network;

import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;

public class ClientBoundPacketHandler {
    public static void sendParticle(ResourceLocation particleId, double x, double y, double z, double vx, double vy, double vz, int count) {
        Level level = Minecraft.getInstance().level;

        if (level == null) return;

        ParticleType<?> type = ForgeRegistries.PARTICLE_TYPES.getValue(particleId);

        if (!(type instanceof SimpleParticleType simpleType)) return;

        for (int i = 0; i < count; i++) {
            level.addParticle(simpleType, x, y, z, vx, vy, vz);
        }
    }
}
