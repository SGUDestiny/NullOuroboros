package destiny.null_ouroboros.server.registry;

import destiny.null_ouroboros.NullOuroboros;
import destiny.null_ouroboros.server.entity.BurrowBeaconEntity;
import destiny.null_ouroboros.server.entity.FallingDroplightBlockEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class EntityRegistry {
    public static DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, NullOuroboros.MODID);

    public static final RegistryObject<EntityType<FallingDroplightBlockEntity>> FALLING_DROPLIGHT =
            ENTITY_TYPES.register("falling_droplight",
                    () -> EntityType.Builder.of(FallingDroplightBlockEntity::new, MobCategory.MISC)
                            .sized(1f, 1f)
                            .build(ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, "falling_droplight").toString()));

    public static final RegistryObject<EntityType<BurrowBeaconEntity>> BURROW_BEACON =
            ENTITY_TYPES.register("burrow_beacon",
                    () -> EntityType.Builder.of(BurrowBeaconEntity::new, MobCategory.MISC)
                            .build(ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, "burrow_beacon").toString()));

    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }
}
