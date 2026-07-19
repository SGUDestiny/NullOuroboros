package destiny.null_ouroboros.server.registry;

import com.mojang.serialization.Codec;
import destiny.null_ouroboros.NullOuroboros;
import destiny.null_ouroboros.server.particle.ColoredParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ParticleTypeRegistry {
    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES = DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, NullOuroboros.MODID);

    public static final RegistryObject<SimpleParticleType> ASH = PARTICLE_TYPES.register("ash", () -> new SimpleParticleType(true));
    public static final RegistryObject<SimpleParticleType> LEVIATHAN_BLOOD = PARTICLE_TYPES.register("leviathan_blood", () -> new SimpleParticleType(true));
    public static final RegistryObject<ParticleType<ColoredParticleOptions>> TINTED_SMOKE =
            PARTICLE_TYPES.register("tinted_smoke", () -> new ParticleType<>(false, ColoredParticleOptions.DESERIALIZER) {
                @Override
                public Codec<ColoredParticleOptions> codec() {
                    return ColoredParticleOptions.CODEC;
                }
            });
}