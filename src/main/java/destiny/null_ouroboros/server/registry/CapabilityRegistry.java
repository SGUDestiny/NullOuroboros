package destiny.null_ouroboros.server.registry;

import destiny.null_ouroboros.NullOuroboros;
import destiny.null_ouroboros.server.ash.AshAtmosphere;
import destiny.null_ouroboros.server.capability.ManifoldingCapability;
import destiny.null_ouroboros.server.capability.RecoilCapability;
import destiny.null_ouroboros.server.capability.RespiratoryCapability;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = NullOuroboros.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CapabilityRegistry {
    public static final Capability<ManifoldingCapability> MANIFOLDING_CAPABILITY = CapabilityManager.get(new CapabilityToken<>() {
    });
    public static final Capability<RecoilCapability> RECOIL_CAPABILITY = CapabilityManager.get(new CapabilityToken<>() {
    });
    public static final Capability<RespiratoryCapability> RESPIRATORY_CAPABILITY = CapabilityManager.get(new CapabilityToken<>() {
    });
    public static final Capability<AshAtmosphere> ASH_ATMOSPHERE_CAPABILITY = CapabilityManager.get(new CapabilityToken<>() {
    });
}
