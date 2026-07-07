package destiny.null_ouroboros.server.worldgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

public record RandomStructureFeatureConfiguration(ResourceLocation structure) implements FeatureConfiguration {
    public static final Codec<RandomStructureFeatureConfiguration> CODEC =
            RecordCodecBuilder.create(instance ->
                    instance.group(
                            ResourceLocation.CODEC.fieldOf("structure").forGetter(RandomStructureFeatureConfiguration::structure)
                    ).apply(instance, RandomStructureFeatureConfiguration::new)
            );
}
