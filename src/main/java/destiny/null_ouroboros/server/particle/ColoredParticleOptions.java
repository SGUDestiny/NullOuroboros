package destiny.null_ouroboros.server.particle;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import destiny.null_ouroboros.server.registry.ParticleTypeRegistry;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;

public class ColoredParticleOptions implements ParticleOptions {
    private final int color;

    public static final Deserializer<ColoredParticleOptions> DESERIALIZER = new Deserializer<>() {
        @Override
        public ColoredParticleOptions fromCommand(ParticleType<ColoredParticleOptions> type, StringReader reader) throws CommandSyntaxException {
            return new ColoredParticleOptions(reader.readInt());
        }

        @Override
        public ColoredParticleOptions fromNetwork(ParticleType<ColoredParticleOptions> type, FriendlyByteBuf buf) {
            return new ColoredParticleOptions(buf.readInt());
        }
    };

    public static final Codec<ColoredParticleOptions> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(Codec.INT.fieldOf("color").forGetter(o -> o.color)).apply(instance, ColoredParticleOptions::new)
    );

    public ColoredParticleOptions(int color) {
        this.color = color;
    }

    @Override
    public ParticleType<?> getType() {
        return ParticleTypeRegistry.TINTED_SMOKE.get();
    }

    @Override
    public void writeToNetwork(FriendlyByteBuf buf) {
        buf.writeInt(this.color);
    }

    @Override
    public String writeToString() {
        return String.format("%s %d", BuiltInRegistries.PARTICLE_TYPE.getKey(getType()), this.color);
    }

    public int getColor() {
        return this.color;
    }
}