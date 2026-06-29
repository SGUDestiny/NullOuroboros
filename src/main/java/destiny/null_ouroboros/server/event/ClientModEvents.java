package destiny.null_ouroboros.server.event;

import destiny.null_ouroboros.client.sound.SirenSoundManager;
import destiny.null_ouroboros.server.block.entity.MechanicalSirenBlockEntity;
import destiny.null_ouroboros.server.registry.ItemRegistry;
import net.minecraft.world.item.DyeableLeatherItem;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientModEvents {
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        MechanicalSirenBlockEntity.registerClientSoundHandlers(
                SirenSoundManager::clientTick,
                SirenSoundManager::syncFromBlockEntity,
                SirenSoundManager::stop);
    }

    @SubscribeEvent
    public static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        event.register((stack, tintIndex) -> {
            if (tintIndex != 1 || !(stack.getItem() instanceof DyeableLeatherItem dyeable)) {
                return -1;
            }

            return dyeable.getColor(stack);
        }, ItemRegistry.DISKETTE.get());
    }
}
