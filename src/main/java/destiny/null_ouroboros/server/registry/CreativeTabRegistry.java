package destiny.null_ouroboros.server.registry;

import destiny.null_ouroboros.NullOuroboros;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class CreativeTabRegistry {
    public static final DeferredRegister<CreativeModeTab> DEF_REG  = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, NullOuroboros.MODID);

    public static final RegistryObject<CreativeModeTab> MAIN = DEF_REG.register("main", () -> CreativeModeTab.builder()
            .icon(() -> BlockRegistry.ASH_BLOCK.get().asItem().getDefaultInstance())
            .title(Component.translatable("itemGroup.null_ouroboros.main"))
            .displayItems((parameters, output) -> {
                output.accept(BlockRegistry.ASH_PILE.get());
                output.accept(BlockRegistry.ASH_BLOCK.get());
                output.accept(BlockRegistry.TRAMPLED_ASH.get());

                output.accept(BlockRegistry.SCORCHED_LOG.get());
                output.accept(BlockRegistry.SCORCHED_SAPLING.get());
            })
            .build()
    );
}
