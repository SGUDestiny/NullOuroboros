package destiny.null_ouroboros.server.event;

import destiny.null_ouroboros.client.render.model.LiquidatorArmorLayer;
import destiny.null_ouroboros.client.sound.SirenSoundManager;
import destiny.null_ouroboros.server.block.entity.MechanicalSirenBlockEntity;
import destiny.null_ouroboros.server.registry.ItemRegistry;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.DyeableLeatherItem;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
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
                SirenSoundManager::stop
        );
    }

    @SubscribeEvent
    public static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        event.register((stack, tintIndex) -> {
            if (tintIndex != 1 || !(stack.getItem() instanceof DyeableLeatherItem dyeable)) {
                return -1;
            }
            return dyeable.getColor(stack);
        }, ItemRegistry.DISKETTE.get(), ItemRegistry.SPRAY_CAN.get(), ItemRegistry.BIKE_KEY.get());
    }

    @SubscribeEvent
    public static void onAddLayers(EntityRenderersEvent.AddLayers event) {
        for (String skinType : event.getSkins()) {
            LivingEntityRenderer<?, ?> renderer = event.getSkin(skinType);
            if (renderer instanceof PlayerRenderer) {
                renderer.addLayer(new LiquidatorArmorLayer(renderer));
            }
        }

        LivingEntityRenderer<?, ?> armorStandRenderer = event.getRenderer(EntityType.ARMOR_STAND);
        if (armorStandRenderer != null) {
            armorStandRenderer.addLayer(new LiquidatorArmorLayer(armorStandRenderer));
        }
    }
}