package destiny.null_ouroboros.client.sound;

import destiny.null_ouroboros.common.DusterbikeEngineSoundConstants;
import destiny.null_ouroboros.common.DusterbikeEngineSoundConstants.EngineLoopProfile;
import destiny.null_ouroboros.common.DusterbikeGear;
import destiny.null_ouroboros.common.DusterbikeGearConstants;
import destiny.null_ouroboros.server.entity.DusterbikeEntity;
import destiny.null_ouroboros.server.entity.DusterbikePhysics;
import destiny.null_ouroboros.server.registry.EntityRegistry;
import destiny.null_ouroboros.server.registry.SoundRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.registries.RegistryObject;

import java.util.HashMap;
import java.util.Map;

public final class DusterbikeEngineSoundManager {
    private static final Map<Integer, BikeSoundState> STATES = new HashMap<>();

    private DusterbikeEngineSoundManager() {}

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
        for (BikeSoundState state : STATES.values()) {
            state.stopAll(minecraft);
        }
        STATES.clear();
    }

    private static void tickBike(Minecraft minecraft, DusterbikeEntity bike) {
        BikeSoundState state = STATES.computeIfAbsent(bike.getId(), id -> new BikeSoundState());
        DusterbikeGear gear = bike.getGear();
        if (state.lastGear != null && state.lastGear != gear) {
            playGearChangeSound(minecraft, bike, gear);
        }
        state.lastGear = gear;

        EngineLoopProfile profile = resolveProfile(bike);
        if (profile == null) {
            state.fadeOut(minecraft);
            return;
        }

        SoundEvent soundEvent = soundForProfile(profile);
        state.ensureProfile(minecraft, bike, profile, soundEvent);
        state.tickOutgoing(minecraft);
    }

    private static EngineLoopProfile resolveProfile(DusterbikeEntity bike) {
        if (!bike.isEngineRunning()) {
            return null;
        }

        float speed = bike.getDriveForwardSpeed();
        if (speed < -DusterbikeEngineSoundConstants.SPEED_EPSILON) {
            return EngineLoopProfile.REVERSE;
        }
        if (Math.abs(speed) <= DusterbikeEngineSoundConstants.SPEED_EPSILON) {
            return EngineLoopProfile.IDLE;
        }

        DusterbikeGear gear = bike.getGear();
        return switch (gear) {
            case GEAR_1 -> EngineLoopProfile.GEAR_1;
            case GEAR_2 -> EngineLoopProfile.GEAR_2;
            case GEAR_3 -> EngineLoopProfile.GEAR_3;
            default -> EngineLoopProfile.IDLE;
        };
    }

    private static float speedRatioForProfile(DusterbikeEntity bike, EngineLoopProfile profile) {
        if (profile == EngineLoopProfile.IDLE) {
            return 0.0F;
        }

        float speed = Math.abs(bike.getDriveForwardSpeed());
        float maxSpeed = switch (profile) {
            case GEAR_1 -> DusterbikeGearConstants.MAX_GEAR_1_SPEED;
            case GEAR_2 -> DusterbikeGearConstants.MAX_GEAR_2_SPEED;
            case GEAR_3 -> DusterbikeGearConstants.MAX_GEAR_3_SPEED;
            case REVERSE -> DusterbikeGearConstants.MAX_REVERSE_SPEED;
            case IDLE -> 1.0F;
        };
        if (maxSpeed <= DusterbikePhysics.SPEED_EPSILON) {
            return 0.0F;
        }
        return Mth.clamp(speed / maxSpeed, 0.0F, 1.0F);
    }

    private static SoundEvent soundForProfile(EngineLoopProfile profile) {
        RegistryObject<SoundEvent> sound = switch (profile) {
            case IDLE -> SoundRegistry.DUSTERBIKE_ENGINE_IDLE;
            case GEAR_1 -> SoundRegistry.DUSTERBIKE_ENGINE_GEAR_1;
            case GEAR_2 -> SoundRegistry.DUSTERBIKE_ENGINE_GEAR_2;
            case GEAR_3 -> SoundRegistry.DUSTERBIKE_ENGINE_GEAR_3;
            case REVERSE -> SoundRegistry.DUSTERBIKE_ENGINE_REVERSE;
        };
        return sound.get();
    }

    private static void playGearChangeSound(Minecraft minecraft, DusterbikeEntity bike, DusterbikeGear gear) {
        SoundEvent sound = switch (gear) {
            case R -> SoundRegistry.DUSTERBIKE_GEAR_CHANGE_1.get();
            case N -> SoundRegistry.DUSTERBIKE_GEAR_CHANGE_2.get();
            case GEAR_1 -> SoundRegistry.DUSTERBIKE_GEAR_CHANGE_1.get();
            case GEAR_2 -> SoundRegistry.DUSTERBIKE_GEAR_CHANGE_2.get();
            case GEAR_3 -> SoundRegistry.DUSTERBIKE_GEAR_CHANGE_3.get();
        };

        minecraft.getSoundManager().play(new SimpleSoundInstance(
                sound,
                SoundSource.NEUTRAL,
                1.0F,
                1.0F,
                SoundInstance.createUnseededRandom(),
                bike.getX(),
                bike.getY(),
                bike.getZ()));
    }

    private static final class BikeSoundState {
        private EngineLoopProfile activeProfile;
        private DusterbikeEngineLoopSound activeLoop;
        private DusterbikeEngineLoopSound outgoingLoop;
        private DusterbikeGear lastGear;

        void ensureProfile(
                Minecraft minecraft,
                DusterbikeEntity bike,
                EngineLoopProfile profile,
                SoundEvent soundEvent) {
            if (activeProfile == profile && activeLoop != null && !activeLoop.isStopped()) {
                activeLoop.setTargetVolume(1.0F);
                return;
            }

            if (activeLoop != null) {
                activeLoop.setTargetVolume(0.0F);
                outgoingLoop = activeLoop;
            }

            activeProfile = profile;
            EngineLoopProfile active = profile;
            activeLoop = new DusterbikeEngineLoopSound(
                    soundEvent,
                    bike,
                    () -> DusterbikeEngineSoundConstants.computePitch(active, speedRatioForProfile(bike, active)));
            activeLoop.forceVolume(0.0F);
            activeLoop.setTargetVolume(1.0F);
            minecraft.getSoundManager().play(activeLoop);
        }

        void fadeOut(Minecraft minecraft) {
            if (activeLoop != null) {
                activeLoop.setTargetVolume(0.0F);
                outgoingLoop = activeLoop;
                activeLoop = null;
                activeProfile = null;
            }
        }

        void tickOutgoing(Minecraft minecraft) {
            if (outgoingLoop != null && outgoingLoop.isStopped()) {
                outgoingLoop = null;
            }
        }

        void stopAll(Minecraft minecraft) {
            if (activeLoop != null) {
                minecraft.getSoundManager().stop(activeLoop);
                activeLoop = null;
            }
            if (outgoingLoop != null) {
                minecraft.getSoundManager().stop(outgoingLoop);
                outgoingLoop = null;
            }
            activeProfile = null;
            lastGear = null;
        }
    }
}
