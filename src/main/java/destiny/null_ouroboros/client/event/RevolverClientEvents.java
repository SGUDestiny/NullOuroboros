package destiny.null_ouroboros.client.event;

import com.mojang.blaze3d.platform.InputConstants;
import destiny.null_ouroboros.client.input.KeyBindRegistry;
import destiny.null_ouroboros.client.render.item.HeavyRevolverGeoRenderer;
import destiny.null_ouroboros.common.revolver.RevolverAction;
import destiny.null_ouroboros.common.revolver.RevolverCartridge;
import destiny.null_ouroboros.common.revolver.RevolverState;
import destiny.null_ouroboros.server.item.HeavyRevolverItem;
import destiny.null_ouroboros.server.network.ServerBoundRevolverActionPacket;
import destiny.null_ouroboros.server.registry.CapabilityRegistry;
import destiny.null_ouroboros.server.registry.PacketHandlerRegistry;
import destiny.null_ouroboros.server.util.ModUtil;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class RevolverClientEvents {
    private RevolverClientEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (!holdingRevolver(minecraft)) {
            drainRevolverClicks();
            return;
        }

        while (KeyBindRegistry.REVOLVER_CYLINDER.consumeClick()) {
            send(RevolverAction.TOGGLE_CYLINDER);
        }
        while (KeyBindRegistry.REVOLVER_PRIMARY.consumeClick()) {
            handlePrimary(minecraft.player);
        }
        while (KeyBindRegistry.REVOLVER_SECONDARY.consumeClick()) {
            handleSecondary(minecraft.player);
        }
    }

    @SubscribeEvent
    public static void onInteractionKey(InputEvent.InteractionKeyMappingTriggered event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!holdingRevolver(minecraft)) {
            return;
        }
        if (event.getKeyMapping() == minecraft.options.keyAttack
                && (isMouseBinding(KeyBindRegistry.REVOLVER_PRIMARY, GLFW.GLFW_MOUSE_BUTTON_LEFT)
                || isMouseBinding(KeyBindRegistry.REVOLVER_SECONDARY, GLFW.GLFW_MOUSE_BUTTON_LEFT))) {
            event.setCanceled(true);
        } else if (event.getKeyMapping() == minecraft.options.keyUse
                && (isMouseBinding(KeyBindRegistry.REVOLVER_PRIMARY, GLFW.GLFW_MOUSE_BUTTON_RIGHT)
                || isMouseBinding(KeyBindRegistry.REVOLVER_SECONDARY, GLFW.GLFW_MOUSE_BUTTON_RIGHT))) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!holdingRevolver(minecraft) || minecraft.player == null || minecraft.screen != null) {
            return;
        }
        if (!RevolverState.isReloading(minecraft.player.getMainHandItem())) {
            return;
        }
        event.setCanceled(true);
        if (event.getScrollDelta() != 0.0D) {
            send(event.getScrollDelta() > 0.0D ? RevolverAction.ROTATE_FORWARD : RevolverAction.ROTATE_BACKWARD);
        }
    }

    @SubscribeEvent
    public static void onMovementInput(MovementInputUpdateEvent event) {
        Player player = event.getEntity();
        if (!(player.getMainHandItem().getItem() instanceof HeavyRevolverItem)) {
            return;
        }
        if (RevolverState.isReloading(player.getMainHandItem())) {
            event.getInput().shiftKeyDown = false;
        }
    }

    private static void handlePrimary(Player player) {
        ItemStack stack = player.getMainHandItem();
        if (RevolverState.isReloading(stack)) {
            send(KeyBindRegistry.REVOLVER_EJECT_ALL.isDown() ? RevolverAction.EJECT_ALL : RevolverAction.EJECT_SELECTED);
            return;
        }

        int selected = RevolverState.getSelected(stack);
        RevolverCartridge cartridge = RevolverState.getChamber(stack, selected);
        if (RevolverState.isCocked(stack) && cartridge.isLive()) {
            player.getCapability(CapabilityRegistry.RECOIL_CAPABILITY).ifPresent(recoil ->
                    recoil.addRecoil(15.0F, ModUtil.getBoundRandomFloatStatic(player.level(), -10, 10), 0.2F));
            RevolverState.setChamber(stack, selected, RevolverCartridge.CASING);
            HeavyRevolverGeoRenderer.requestHammerSnap(stack);
        }
        if (RevolverState.isCocked(stack)) {
            RevolverState.setCocked(stack, false);
            HeavyRevolverGeoRenderer.requestHammerSnap(stack);
        }
        send(RevolverAction.FIRE);
    }

    private static void handleSecondary(Player player) {
        ItemStack stack = player.getMainHandItem();
        send(RevolverState.isReloading(stack) ? RevolverAction.INSERT_SELECTED : RevolverAction.TOGGLE_COCK);
    }

    private static void drainRevolverClicks() {
        while (KeyBindRegistry.REVOLVER_CYLINDER.consumeClick()) {
        }
        while (KeyBindRegistry.REVOLVER_PRIMARY.consumeClick()) {
        }
        while (KeyBindRegistry.REVOLVER_SECONDARY.consumeClick()) {
        }
    }

    private static boolean holdingRevolver(Minecraft minecraft) {
        return minecraft.player != null && minecraft.screen == null
                && minecraft.player.getMainHandItem().getItem() instanceof HeavyRevolverItem;
    }

    private static boolean isMouseBinding(KeyMapping keyMapping, int button) {
        InputConstants.Key key = keyMapping.getKey();
        return key.getType() == InputConstants.Type.MOUSE && key.getValue() == button;
    }

    private static void send(RevolverAction action) {
        PacketHandlerRegistry.INSTANCE.sendToServer(new ServerBoundRevolverActionPacket(action));
    }
}
