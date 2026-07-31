package destiny.null_ouroboros.client.sound;

import destiny.null_ouroboros.common.dusterbike.DusterbikePartType;
import destiny.null_ouroboros.server.entity.DusterbikeEntity;
import destiny.null_ouroboros.server.entity.DusterbikePhysics;
import destiny.null_ouroboros.server.entity.DusterbikeWheelEntity;
import destiny.null_ouroboros.server.registry.EntityRegistry;
import destiny.null_ouroboros.server.registry.SoundRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.TickEvent;

import java.util.HashMap;
import java.util.Map;

public final class DusterbikeWheelRimSoundManager {
    private static final Map<Integer, BikeRimState> STATES = new HashMap<>();

    private DusterbikeWheelRimSoundManager() {}

    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) {
            return;
        }

        for (Entity entity : level.entitiesForRendering()) {
            if (entity instanceof DusterbikeEntity bike && bike.getType() == EntityRegistry.DUSTERBIKE.get()) {
                tickBike(minecraft, bike);
            }
        }

        STATES.entrySet().removeIf(entry -> level.getEntity(entry.getKey()) == null);
    }

    public static void stopAll() {
        Minecraft minecraft = Minecraft.getInstance();
        for (BikeRimState state : STATES.values()) {
            state.stopAll(minecraft);
        }
        STATES.clear();
    }

    private static void tickBike(Minecraft minecraft, DusterbikeEntity bike) {
        BikeRimState state = STATES.computeIfAbsent(bike.getId(), id -> new BikeRimState());
        state.tick(minecraft, bike);
    }

    private static final class BikeRimState {
        private DusterbikeWheelRimLoopSound frontLoop;
        private DusterbikeWheelRimLoopSound rearLoop;

        private void tick(Minecraft minecraft, DusterbikeEntity bike) {
            updateWheelLoop(minecraft, bike, DusterbikePartType.FRONT_WHEEL, true);
            updateWheelLoop(minecraft, bike, DusterbikePartType.REAR_WHEEL, false);
        }

        private void updateWheelLoop(Minecraft minecraft, DusterbikeEntity bike, DusterbikePartType wheelType, boolean front) {
            DusterbikeWheelRimLoopSound loop = front ? frontLoop : rearLoop;
            if (!shouldPlayRimLoop(bike, wheelType)) {
                if (loop != null) {
                    loop.setTargetVolume(0.0F);
                    if (loop.isStopped()) {
                        if (front) {
                            frontLoop = null;
                        } else {
                            rearLoop = null;
                        }
                    }
                }
                return;
            }

            if (loop == null || loop.isStopped()) {
                loop = new DusterbikeWheelRimLoopSound(SoundRegistry.DUSTERBIKE_WHEEL_RIM_LOOP.get(), bike);
                loop.setTargetVolume(0.5F);
                minecraft.getSoundManager().play(loop);
                if (front) {
                    frontLoop = loop;
                } else {
                    rearLoop = loop;
                }
                return;
            }

            loop.setTargetVolume(0.5F);
        }

        private static boolean shouldPlayRimLoop(DusterbikeEntity bike, DusterbikePartType wheelType) {
            if (bike.hasUsable(wheelType)) {
                return false;
            }
            if (Math.abs(bike.getDriveForwardSpeed()) <= DusterbikePhysics.SPEED_EPSILON) {
                return false;
            }
            DusterbikeWheelEntity wheel = wheelType == DusterbikePartType.FRONT_WHEEL ? bike.getFrontWheel() : bike.getRearWheel();
            return wheel != null && wheel.isGrounded();
        }

        private void stopAll(Minecraft minecraft) {
            fadeOut(minecraft, frontLoop);
            fadeOut(minecraft, rearLoop);
            frontLoop = null;
            rearLoop = null;
        }

        private static void fadeOut(Minecraft minecraft, DusterbikeWheelRimLoopSound loop) {
            if (loop != null) {
                loop.setTargetVolume(0.0F);
                minecraft.getSoundManager().stop(loop);
            }
        }
    }
}
