package destiny.null_ouroboros.server.registry;

import com.mojang.serialization.Codec;
import destiny.null_ouroboros.NullOuroboros;
import destiny.null_ouroboros.client.render.particle.TintedSmokeParticle;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ParticleTypeRegistry {
    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES = DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, NullOuroboros.MODID);

    public static final RegistryObject<SimpleParticleType> ASH = PARTICLE_TYPES.register("ash", () -> new SimpleParticleType(true));
    public static final RegistryObject<ParticleType<TintedSmokeParticle.ColoredParticleOptions>> TINTED_SMOKE =
            PARTICLE_TYPES.register("tinted_smoke", () -> new ParticleType<>(false, TintedSmokeParticle.ColoredParticleOptions.DESERIALIZER) {
                @Override
                public Codec<TintedSmokeParticle.ColoredParticleOptions> codec() {
                    return TintedSmokeParticle.ColoredParticleOptions.CODEC;
                }
            });
}
