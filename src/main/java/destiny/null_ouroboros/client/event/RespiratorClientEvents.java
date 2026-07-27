package destiny.null_ouroboros.client.event;

import destiny.null_ouroboros.client.input.KeyBindRegistry;
import destiny.null_ouroboros.common.respirator.FilterAction;
import destiny.null_ouroboros.server.entity.DusterbikeEntity;
import destiny.null_ouroboros.server.item.FilterItem;
import destiny.null_ouroboros.server.item.RespiratorGear;
import destiny.null_ouroboros.server.network.ServerboundFilterActionPacket;
import destiny.null_ouroboros.server.registry.PacketHandlerRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class RespiratorClientEvents {
    private RespiratorClientEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.screen != null) {
            while (KeyBindRegistry.RESPIRATOR_FILTER.consumeClick()) {
            }
            return;
        }

        if (minecraft.player.getVehicle() instanceof DusterbikeEntity) {
            while (KeyBindRegistry.RESPIRATOR_FILTER.consumeClick()) {
            }
            return;
        }

        ItemStack head = minecraft.player.getItemBySlot(EquipmentSlot.HEAD);
        if (!RespiratorGear.isRespiratorHelmet(head)) {
            while (KeyBindRegistry.RESPIRATOR_FILTER.consumeClick()) {
            }
            return;
        }

        while (KeyBindRegistry.RESPIRATOR_FILTER.consumeClick()) {
            boolean left = minecraft.options.keyShift.isDown();
            PacketHandlerRegistry.INSTANCE.sendToServer(new ServerboundFilterActionPacket(
                    left ? FilterAction.REMOVE_LEFT : FilterAction.REMOVE_RIGHT));
        }
    }

    @SubscribeEvent
    public static void onInteractionKey(InputEvent.InteractionKeyMappingTriggered event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.screen != null) {
            return;
        }
        if (event.getKeyMapping() != minecraft.options.keyUse) {
            return;
        }
        ItemStack head = minecraft.player.getItemBySlot(EquipmentSlot.HEAD);
        if (!RespiratorGear.isRespiratorHelmet(head)) {
            return;
        }
        if (!(minecraft.player.getMainHandItem().getItem() instanceof FilterItem)) {
            return;
        }
        if (RespiratorGear.firstEmptySlotPreferRight(head) == null) {
            return;
        }
        event.setCanceled(true);
        event.setSwingHand(false);
        PacketHandlerRegistry.INSTANCE.sendToServer(new ServerboundFilterActionPacket(FilterAction.PUT));
    }
}
