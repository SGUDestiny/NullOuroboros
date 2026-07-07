package destiny.null_ouroboros.server.registry;

import destiny.null_ouroboros.NullOuroboros;
import destiny.null_ouroboros.server.worldgen.placement.NoisePeakPlacement;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class PlacementRegistry {
    public static final DeferredRegister<PlacementModifierType<?>> PLACEMENT_MODIFIERS =
            DeferredRegister.create(Registries.PLACEMENT_MODIFIER_TYPE, NullOuroboros.MODID);

    public static final RegistryObject<PlacementModifierType<NoisePeakPlacement>> NOISE_PEAK =
            PLACEMENT_MODIFIERS.register("noise_peak", () -> () -> NoisePeakPlacement.CODEC);
}
