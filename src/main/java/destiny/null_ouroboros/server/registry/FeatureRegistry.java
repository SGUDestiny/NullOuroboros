package destiny.null_ouroboros.server.registry;

import destiny.null_ouroboros.NullOuroboros;
import destiny.null_ouroboros.server.worldgen.feature.AshPileFeature;
import destiny.null_ouroboros.server.worldgen.feature.RandomStructureFeature;
import destiny.null_ouroboros.server.worldgen.feature.ScorchedTrunkPlacer;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.trunkplacers.TrunkPlacerType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class FeatureRegistry {
    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(ForgeRegistries.FEATURES, NullOuroboros.MODID);
    public static final DeferredRegister<TrunkPlacerType<?>> TRUNKS = DeferredRegister.create(Registries.TRUNK_PLACER_TYPE, NullOuroboros.MODID);

    public static ResourceKey<ConfiguredFeature<?, ?>> registerKey(String name) {
        return ResourceKey.create(Registries.CONFIGURED_FEATURE, ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, name));
    }

    public static final RegistryObject<Feature<?>> ASH_PILE_FEATURE = FEATURES.register("ash_pile", AshPileFeature::new);

    public static final RegistryObject<Feature<?>> RANDOM_STRUCTURE_FEATURE = FEATURES.register("random_structure", RandomStructureFeature::new);

    public static final ResourceKey<ConfiguredFeature<?, ?>> SCORCHED_TREE = registerKey("scorched_tree");
    public static final RegistryObject<TrunkPlacerType<ScorchedTrunkPlacer>> SCORCHED_TRUNK = TRUNKS.register("scorched_trunk_placer", () -> new TrunkPlacerType<>(ScorchedTrunkPlacer.CODEC));
}
