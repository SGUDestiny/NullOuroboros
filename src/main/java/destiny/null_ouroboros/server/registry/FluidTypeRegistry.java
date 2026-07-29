package destiny.null_ouroboros.server.registry;

import destiny.null_ouroboros.NullOuroboros;
import destiny.null_ouroboros.common.fluid.BloodFluid;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.joml.Vector3f;

public class FluidTypeRegistry {
    public static final ResourceLocation BLOOD_STILL = ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, "block/blood_still");
    public static final ResourceLocation BLOOD_FLOW = ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, "block/blood_flow");

    public static final DeferredRegister<FluidType> FLUID_TYPES =
            DeferredRegister.create(ForgeRegistries.Keys.FLUID_TYPES, NullOuroboros.MODID);

    public static final RegistryObject<FluidType> BLOOD_TYPE = registerFluidType("blood",
            new BloodFluid(BLOOD_STILL, BLOOD_FLOW, null, 0xFFFFFFFF,
                    new Vector3f(40f / 256f, 26f / 256f, 45f / 256f),
                    FluidType.Properties.create()
                            .lightLevel(0)
                            .density(2000)
                            .viscosity(3500)
                            .motionScale(0.0105)
                            .canSwim(true)
                            .canDrown(true)
                            .canExtinguish(true)));

    private static RegistryObject<FluidType> registerFluidType(String name, FluidType fluidType) {
        return FLUID_TYPES.register(name, () -> fluidType);
    }

    public static void register(IEventBus eventBus) {
        FLUID_TYPES.register(eventBus);
    }
}
