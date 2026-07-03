package destiny.null_ouroboros.server.event;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import destiny.null_ouroboros.client.render.dimension.VergeOfRealityDimensionEffects;
import destiny.null_ouroboros.client.render.DusterbikePistonShakeManager;
import destiny.null_ouroboros.client.render.DusterbikeVisualEffects;
import destiny.null_ouroboros.client.input.KeyBindRegistry;
import destiny.null_ouroboros.client.util.DusterbikeKeyTargeting;
import destiny.null_ouroboros.client.util.DusterbikePartTargeting;
import destiny.null_ouroboros.client.sound.DusterbikeEngineSoundManager;
import destiny.null_ouroboros.client.sound.ManifoldingSoundManager;
import destiny.null_ouroboros.client.sound.SirenSoundManager;
import destiny.null_ouroboros.client.sound.VergeAmbienceSoundManager;
import destiny.null_ouroboros.common.light.RedstickLightManager;
import destiny.null_ouroboros.server.item.BikeKeyItem;
import destiny.null_ouroboros.server.item.SprayCanItem;
import destiny.null_ouroboros.server.entity.DusterbikeEntity;
import destiny.null_ouroboros.server.network.ServerBoundDusterbikeHeadlightPacket;
import destiny.null_ouroboros.server.network.ServerBoundDusterbikeKeyPacket;
import destiny.null_ouroboros.server.network.ServerBoundDusterbikePartInteractPacket;
import destiny.null_ouroboros.server.network.ServerBoundDusterbikeShiftPacket;
import destiny.null_ouroboros.server.registry.PacketHandlerRegistry;
import net.minecraft.client.Camera;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

import static org.lwjgl.opengl.GL32C.GL_DEPTH_CLAMP;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ClientForgeEvents {
    private static final BufferBuilder BUFFER = new BufferBuilder(65536);
    private static final int DUSTERBIKE_SHIFT_COOLDOWN_TICKS = 10;

    private static int dusterbikeShiftCooldownTicks;

    private static int keyInteractionBikeId = -1;
    private static boolean keyHoldActive;
    private static boolean keyIgnitionBlockedUntilRelease;
    private static final IntOpenHashSet clientKeyCrankBikeIds = new IntOpenHashSet();

    @SubscribeEvent
    public static void levelRender(RenderLevelStageEvent event) {
        boolean renderSkyPass = event.getStage().equals(RenderLevelStageEvent.Stage.AFTER_SKY);
        if (renderSkyPass) {
            ClientLevel level = Minecraft.getInstance().level;
            if (level == null)
                return;

            float partialTick = event.getPartialTick();

            Camera camera = event.getCamera();

            PoseStack pose = event.getPoseStack();
            MultiBufferSource.BufferSource buffer = MultiBufferSource.immediate(BUFFER);

            GL11.glEnable(GL_DEPTH_CLAMP);

            if (VergeOfRealityDimensionEffects.isVergeOfReality(level)) {
                VergeOfRealityDimensionEffects vergeOfRealityDimensionEffects = VergeOfRealityDimensionEffects.getInstance();

                if (vergeOfRealityDimensionEffects != null) {
                    pose.pushPose();
                    vergeOfRealityDimensionEffects.renderOverlay(level, partialTick, pose, camera, event.getProjectionMatrix());
                    pose.popPose();
                }
            }

            buffer.endBatch();

            GL11.glDisable(GL_DEPTH_CLAMP);
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        ManifoldingSoundManager.tick(event);
        VergeAmbienceSoundManager.tick(event);
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
            drainDusterbikeShiftClicks();
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

        if (direction == 0) {
            return;
        }

        shiftDusterbikeGear(bike, direction);
    }

    private static void drainDusterbikeShiftClicks() {
        while (KeyBindRegistry.SHIFT_UP.consumeClick()) {}
        while (KeyBindRegistry.SHIFT_DOWN.consumeClick()) {}
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

        if (keyHoldActive) {
            if (!useDown) {
                resetDusterbikeKeyHold();
            }
            return;
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

    public static boolean isKeyCrankVisualActive(DusterbikeEntity bike) {
        int bikeId = bike.getId();
        return clientKeyCrankBikeIds.contains(bikeId) || bike.isKeyCranking();
    }

    private static void sendDusterbikeKeyPacket(int entityId, boolean holding, boolean pressed) {
        if (entityId < 0) {
            return;
        }
        PacketHandlerRegistry.INSTANCE.sendToServer(new ServerBoundDusterbikeKeyPacket(entityId, holding, pressed));
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

        boolean forward = KeyBindRegistry.FORWARD.isDown();
        boolean backward = KeyBindRegistry.BACKWARD.isDown();
        boolean left = KeyBindRegistry.STEER_LEFT.isDown();
        boolean right = KeyBindRegistry.STEER_RIGHT.isDown();
        boolean handbrake = KeyBindRegistry.HANDBRAKE.isDown();

        bike.applyRiderInput(forward, backward, left, right, handbrake);
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
            return;
        }

        DusterbikeEntity keyTarget = DusterbikeKeyTargeting.findKeyTarget(minecraft);
        if (keyTarget != null && button == GLFW.GLFW_MOUSE_BUTTON_RIGHT && event.getAction() == GLFW.GLFW_PRESS) {
            event.setCanceled(true);
            var heldStack = minecraft.player.getMainHandItem();
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

        DusterbikePartTargeting.Target partTarget = DusterbikePartTargeting.findPartTarget(minecraft);
        if (partTarget != null && button == GLFW.GLFW_MOUSE_BUTTON_RIGHT && event.getAction() == GLFW.GLFW_PRESS) {
            event.setCanceled(true);
            PacketHandlerRegistry.INSTANCE.sendToServer(new ServerBoundDusterbikePartInteractPacket(partTarget.bike().getId(), partTarget.targetType(),
                    InteractionHand.MAIN_HAND, minecraft.player.isSecondaryUseActive()));
            return;
        }

        DusterbikeEntity bike = getDrivableBike(minecraft);
        if (bike == null) {
            return;
        }

        event.setCanceled(true);

        if (event.getAction() == GLFW.GLFW_PRESS && dusterbikeShiftCooldownTicks <= 0) {
            int direction = getDusterbikeMouseShiftDirection(button);
            if (direction != 0) {
                shiftDusterbikeGear(bike, direction);
            }
        }
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

    @SubscribeEvent
    public static void onDusterbikeInteractionKey(InputEvent.InteractionKeyMappingTriggered event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (DusterbikeKeyTargeting.blocksVanillaUse(minecraft) || DusterbikePartTargeting.blocksVanillaUse(minecraft)) {
            if (event.getKeyMapping() == minecraft.options.keyUse) {
                event.setCanceled(true);
            }
            return;
        }

        if (getDrivableBike(minecraft) == null) {
            return;
        }

        if (event.getKeyMapping() == minecraft.options.keyAttack
                || event.getKeyMapping() == minecraft.options.keyUse
                || event.getKeyMapping() == minecraft.options.keyPickItem) {
            event.setCanceled(true);
        }
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

    @SubscribeEvent
    public static void onClientDisconnect(ClientPlayerNetworkEvent.LoggingOut event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && minecraft.player.getVehicle() instanceof DusterbikeEntity bike) {
            bike.flushDriveStateBeforeDisconnect();
        }

        SirenSoundManager.stopAll();
        DusterbikeEngineSoundManager.stopAll();
        DusterbikePistonShakeManager.clear();
        VergeAmbienceSoundManager.stopInstance(Minecraft.getInstance());
        RedstickLightManager.clearAll();
        resetDusterbikeKeyHold();
        clientKeyCrankBikeIds.clear();
        keyIgnitionBlockedUntilRelease = false;
    }

    @SubscribeEvent
    public static void onClientChunkLoad(ChunkEvent.Load event) {
        if (event.getLevel() instanceof Level level && event.getChunk() instanceof LevelChunk chunk) {
            RedstickLightManager.scheduleRecheckSavedBlockLight(level, chunk);
        }
    }
}
