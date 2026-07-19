package destiny.null_ouroboros.server.registry;

import destiny.null_ouroboros.NullOuroboros;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.level.Level;

public class DamageTypeRegistry {
    public static final ResourceKey<DamageType> MANIFOLDING_ERASURE = ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, "manifolding_erasure"));
    public static final ResourceKey<DamageType> BURROW_BEACON_DRILL = ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, "burrow_beacon_drill"));
    public static final ResourceKey<DamageType> DUSTERBIKE_IMPACT = ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, "dusterbike_impact"));
    public static final ResourceKey<DamageType> STEEL_LEVIATHAN_CONTACT = ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, "steel_leviathan_contact"));
    public static final ResourceKey<DamageType> STEEL_LEVIATHAN_LUNGE = ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, "steel_leviathan_lunge"));
    public static final ResourceKey<DamageType> BURROW_MISSILE = ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, "burrow_missile"));

    public static DamageSource getSimpleDamageSource(Level level, ResourceKey<DamageType> type) {
        return new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(type));
    }
}
