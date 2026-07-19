package destiny.null_ouroboros.client.event;

import com.mojang.blaze3d.platform.InputConstants;
import destiny.null_ouroboros.client.input.KeyBindRegistry;
import destiny.null_ouroboros.client.render.DusterbikePistonShakeManager;
import destiny.null_ouroboros.client.render.DusterbikeVisualEffects;
import destiny.null_ouroboros.client.sound.DusterbikeEngineSoundManager;
import destiny.null_ouroboros.client.util.DusterbikeKeyTargeting;
import destiny.null_ouroboros.client.util.PartTargeting;
import destiny.null_ouroboros.common.dusterbike.DusterbikePartTargetType;
import destiny.null_ouroboros.common.light.DusterbikeHeadlightManager;
import destiny.null_ouroboros.server.entity.DusterbikeEntity;
import destiny.null_ouroboros.server.entity.HoistPartInteractionEntity;
import destiny.null_ouroboros.server.item.BikeKeyItem;
import destiny.null_ouroboros.server.item.JerrycanItem;
import destiny.null_ouroboros.server.item.SprayCanItem;
import destiny.null_ouroboros.server.network.ServerBoundDusterbikeFuelPacket;
import destiny.null_ouroboros.server.network.ServerBoundDusterbikeHeadlightPacket;
import destiny.null_ouroboros.server.network.ServerBoundDusterbikeKeyPacket;
import destiny.null_ouroboros.server.network.ServerBoundDusterbikePartInteractPacket;
import destiny.null_ouroboros.server.network.ServerBoundDusterbikeShiftPacket;
import destiny.null_ouroboros.server.network.ServerBoundHoistPartInteractPacket;
import destiny.null_ouroboros.server.registry.ItemRegistry;
import destiny.null_ouroboros.server.registry.PacketHandlerRegistry;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class DusterbikeClientEvents {
    private static final int DUSTERBIKE_SHIFT_COOLDOWN_TICKS = 10;

    private static int dusterbikeShiftCooldownTicks;
    private static int keyInteractionBikeId = -1;
    private static boolean keyHoldActive;
    private static boolean keyIgnitionBlockedUntilRelease;
    private static final IntOpenHashSet clientKeyCrankBikeIds = new IntOpenHashSet();
    private static int fuelTransferBikeId = -1;
    private static boolean fuelTransferDrain;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        DusterbikeEngineSoundManager.tick(event);
        DusterbikeVisualEffects.tick(event);
        DusterbikePistonShakeManager.tick(event);

        if (event.phase == TickEvent.Phase.END) {
            if (dusterbikeShiftCooldownTicks > 0) {
                dusterbikeShiftCooldownTicks--;
            }
            tickDusterbikeKeyHold();
            tickDusterbikeGearShift();
            tickDusterbikeHeadlightToggle();
        }
    }

    @SubscribeEvent
    public static void onDusterbikeMovementInput(MovementInputUpdateEvent event) {
        Player player = event.getEntity();
        if (!(player.getVehicle() instanceof DusterbikeEntity bike)) {
            return;
        }

        var input = event.getInput();
        input.up = false;
        input.down = false;
        input.left = false;
        input.right = false;
        input.jumping = false;

        bike.applyRiderInput(
                KeyBindRegistry.FORWARD.isDown(),
                KeyBindRegistry.BACKWARD.isDown(),
                KeyBindRegistry.STEER_LEFT.isDown(),
                KeyBindRegistry.STEER_RIGHT.isDown(),
                KeyBindRegistry.HANDBRAKE.isDown());
    }

    @SubscribeEvent
    public static void onClientDisconnect(ClientPlayerNetworkEvent.LoggingOut event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && minecraft.player.getVehicle() instanceof DusterbikeEntity bike) {
            bike.flushDriveStateBeforeDisconnect();
        }

        DusterbikeEngineSoundManager.stopAll();
        DusterbikePistonShakeManager.clear();
        DusterbikeHeadlightManager.clearAll();
        resetDusterbikeKeyHold();
        clientKeyCrankBikeIds.clear();
        keyIgnitionBlockedUntilRelease = false;
        fuelTransferBikeId = -1;
    }

    @SubscribeEvent
    public static void onDusterbikeInteractionKey(InputEvent.InteractionKeyMappingTriggered event) {
        Minecraft minecraft = Minecraft.getInstance();

        if (DusterbikeKeyTargeting.blocksVanillaUse(minecraft) || PartTargeting.blocksVanillaUse(minecraft)) {
            if (event.getKeyMapping() == minecraft.options.keyUse) {
                event.setCanceled(true);
            }
            return;
        }

        if (getDrivableBike(minecraft) == null) {
            return;
        }

        if (event.getKeyMapping() == minecraft.options.keyAttack
                && (isMouseBinding(KeyBindRegistry.SHIFT_UP, GLFW.GLFW_MOUSE_BUTTON_LEFT)
                || isMouseBinding(KeyBindRegistry.SHIFT_DOWN, GLFW.GLFW_MOUSE_BUTTON_LEFT))) {
            event.setCanceled(true);
        } else if (event.getKeyMapping() == minecraft.options.keyUse
                && (isMouseBinding(KeyBindRegistry.SHIFT_UP, GLFW.GLFW_MOUSE_BUTTON_RIGHT)
                || isMouseBinding(KeyBindRegistry.SHIFT_DOWN, GLFW.GLFW_MOUSE_BUTTON_RIGHT))) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onDusterbikeMouseInput(InputEvent.MouseButton.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.screen != null) {
            return;
        }

        int button = event.getButton();
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT && button != GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            return;
        }

        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT && event.getAction() == GLFW.GLFW_RELEASE) {
            if (keyHoldActive) {
                event.setCanceled(true);
                resetDusterbikeKeyHold();
            }
            keyIgnitionBlockedUntilRelease = false;

            if (fuelTransferBikeId >= 0) {
                PacketHandlerRegistry.INSTANCE.sendToServer(new ServerBoundDusterbikeFuelPacket(fuelTransferBikeId, false, fuelTransferDrain));
                fuelTransferBikeId = -1;
                event.setCanceled(true);
            }
            return;
        }

        if (button != GLFW.GLFW_MOUSE_BUTTON_RIGHT || event.getAction() != GLFW.GLFW_PRESS) {
            return;
        }

        HoistPartInteractionEntity hoistPart = PartTargeting.findHoistPartTarget(minecraft);
        if (hoistPart != null) {
            event.setCanceled(true);
            PacketHandlerRegistry.INSTANCE.sendToServer(new ServerBoundHoistPartInteractPacket(
                    hoistPart.getParentId(), hoistPart.getTargetType(),
                    InteractionHand.MAIN_HAND, minecraft.player.isSecondaryUseActive()));
            return;
        }

        ItemStack heldStack = minecraft.player.getMainHandItem();

        DusterbikeEntity keyTarget = DusterbikeKeyTargeting.findKeyTarget(minecraft);
        if (keyTarget != null && !heldStack.is(ItemRegistry.WRENCH.get())) {
            event.setCanceled(true);
            if (heldStack.getItem() instanceof BikeKeyItem
                    || heldStack.getItem() instanceof SprayCanItem
                    || (heldStack.isEmpty() && minecraft.player.isSecondaryUseActive())) {
                sendDusterbikeKeyPacket(keyTarget.getId(), false, true);
                return;
            }
            if (keyTarget.isEngineRunning()) {
                sendDusterbikeKeyPacket(keyTarget.getId(), true, false);
                keyHoldActive = true;
                keyInteractionBikeId = keyTarget.getId();
                beginClientKeyCrank(keyTarget.getId());
                keyIgnitionBlockedUntilRelease = true;
            } else if (!keyIgnitionBlockedUntilRelease) {
                sendDusterbikeKeyPacket(keyTarget.getId(), true, false);
                keyHoldActive = true;
                keyInteractionBikeId = keyTarget.getId();
                beginClientKeyCrank(keyTarget.getId());
            }
            return;
        }

        if (getDrivableBike(minecraft) == null) {
            PartTargeting.BikePartTarget partTarget = PartTargeting.findBikePartTarget(minecraft);
            if (partTarget != null) {
                event.setCanceled(true);
                DusterbikePartTargetType type = partTarget.targetType();

                if (type == DusterbikePartTargetType.FUEL_INTAKE) {
                    if (heldStack.getItem() instanceof JerrycanItem) {
                        boolean drain = minecraft.player.isSecondaryUseActive();
                        fuelTransferBikeId = partTarget.bike().getId();
                        fuelTransferDrain = drain;
                        PacketHandlerRegistry.INSTANCE.sendToServer(new ServerBoundDusterbikeFuelPacket(
                                fuelTransferBikeId, true, drain));
                        return;
                    }
                }

                PacketHandlerRegistry.INSTANCE.sendToServer(new ServerBoundDusterbikePartInteractPacket(
                        partTarget.bike().getId(),
                        type,
                        InteractionHand.MAIN_HAND,
                        minecraft.player.isSecondaryUseActive()));
                return;
            }
        }

        DusterbikeEntity bike = getDrivableBike(minecraft);
        if (bike == null) {
            return;
        }

        int direction = getDusterbikeMouseShiftDirection(button);
        if (direction == 0) {
            return;
        }

        event.setCanceled(true);
        if (dusterbikeShiftCooldownTicks <= 0) {
            shiftDusterbikeGear(bike, direction);
            minecraft.player.swingTime = 0;
            minecraft.player.swinging = false;
        }
    }

    public static boolean isKeyCrankVisualActive(DusterbikeEntity bike) {
        int bikeId = bike.getId();
        return clientKeyCrankBikeIds.contains(bikeId) || bike.isKeyCranking();
    }

    private static void tickDusterbikeHeadlightToggle() {
        Minecraft minecraft = Minecraft.getInstance();
        DusterbikeEntity bike = getDrivableBike(minecraft);
        if (bike == null) {
            while (KeyBindRegistry.HEADLIGHTS.consumeClick()) {}
            return;
        }

        while (KeyBindRegistry.HEADLIGHTS.consumeClick()) {
            PacketHandlerRegistry.INSTANCE.sendToServer(new ServerBoundDusterbikeHeadlightPacket(bike.getId()));
        }
    }

    private static void tickDusterbikeGearShift() {
        Minecraft minecraft = Minecraft.getInstance();
        DusterbikeEntity bike = getDrivableBike(minecraft);
        if (bike == null) {
            while (KeyBindRegistry.SHIFT_UP.consumeClick()) {}
            while (KeyBindRegistry.SHIFT_DOWN.consumeClick()) {}
            return;
        }

        if (dusterbikeShiftCooldownTicks > 0) {
            return;
        }

        int direction = 0;
        if (KeyBindRegistry.SHIFT_UP.consumeClick()) {
            direction = 1;
        } else if (KeyBindRegistry.SHIFT_DOWN.consumeClick()) {
            direction = -1;
        }
        if (direction != 0) {
            shiftDusterbikeGear(bike, direction);
            minecraft.player.swingTime = 0;
            minecraft.player.swinging = false;
        }
    }

    private static void shiftDusterbikeGear(DusterbikeEntity bike, int direction) {
        if (!bike.shiftGear(direction)) {
            return;
        }
        PacketHandlerRegistry.INSTANCE.sendToServer(new ServerBoundDusterbikeShiftPacket(bike.getId(), direction));
        dusterbikeShiftCooldownTicks = DUSTERBIKE_SHIFT_COOLDOWN_TICKS;
    }

    private static void tickDusterbikeKeyHold() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.screen != null) {
            resetDusterbikeKeyHold();
            return;
        }

        boolean useDown = isDusterbikeKeyUseHeld(minecraft);
        if (!useDown) {
            keyIgnitionBlockedUntilRelease = false;
        }
        if (keyHoldActive && !useDown) {
            resetDusterbikeKeyHold();
        }
    }

    private static boolean isDusterbikeKeyUseHeld(Minecraft minecraft) {
        if (minecraft.options.keyUse.isDown()) {
            return true;
        }
        long window = minecraft.getWindow().getWindow();
        return GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
    }

    private static void beginClientKeyCrank(int bikeId) {
        if (bikeId >= 0) {
            clientKeyCrankBikeIds.add(bikeId);
        }
    }

    private static void endClientKeyCrank(int bikeId) {
        if (bikeId >= 0) {
            clientKeyCrankBikeIds.remove(bikeId);
        }
    }

    private static void resetDusterbikeKeyHold() {
        if (keyHoldActive && keyInteractionBikeId >= 0) {
            sendDusterbikeKeyPacket(keyInteractionBikeId, false, false);
        }
        endClientKeyCrank(keyInteractionBikeId);
        keyHoldActive = false;
        keyInteractionBikeId = -1;
    }

    private static void sendDusterbikeKeyPacket(int entityId, boolean holding, boolean pressed) {
        if (entityId < 0) {
            return;
        }
        PacketHandlerRegistry.INSTANCE.sendToServer(new ServerBoundDusterbikeKeyPacket(entityId, holding, pressed));
    }

    private static int getDusterbikeMouseShiftDirection(int button) {
        if (isMouseBinding(KeyBindRegistry.SHIFT_UP, button)) {
            return 1;
        }
        if (isMouseBinding(KeyBindRegistry.SHIFT_DOWN, button)) {
            return -1;
        }
        return 0;
    }

    private static boolean isMouseBinding(KeyMapping keyMapping, int button) {
        InputConstants.Key key = keyMapping.getKey();
        return key.getType() == InputConstants.Type.MOUSE && key.getValue() == button;
    }

    private static DusterbikeEntity getDrivableBike(Minecraft minecraft) {
        if (minecraft.screen != null) {
            return null;
        }
        Player player = minecraft.player;
        if (player == null || !(player.getVehicle() instanceof DusterbikeEntity bike)) {
            return null;
        }
        return bike;
    }
}
