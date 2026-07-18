package destiny.null_ouroboros.client.render;

import destiny.null_ouroboros.common.dusterbike.DusterbikePistonShakeConstants;
import destiny.null_ouroboros.server.entity.DusterbikeEntity;
import destiny.null_ouroboros.server.registry.EntityRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.TickEvent;

import java.util.HashMap;
import java.util.Map;

public final class DusterbikePistonShakeManager {
    private static final Map<Integer, ShakeState> STATES = new HashMap<>();

    private DusterbikePistonShakeManager() {}

    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }

        for (Entity entity : level.entitiesForRendering()) {
            if (entity instanceof DusterbikeEntity bike && bike.getType() == EntityRegistry.DUSTERBIKE.get()) {
                tickBike(bike);
            }
        }

        STATES.entrySet().removeIf(entry -> level.getEntity(entry.getKey()) == null);
    }

    public static float getShakeIntensity(DusterbikeEntity bike) {
        if (!bike.isEngineRunning()) {
            return 0.0F;
        }

        ShakeState state = STATES.computeIfAbsent(bike.getId(), id -> new ShakeState());
        noteEngineTransition(bike, state);

        float speedRatio = Mth.clamp(Math.abs(bike.getDriveForwardSpeed()) / DusterbikeEntity.MAX_SPEED, 0.0F, 1.0F);
        float intensity = Mth.lerp(speedRatio, DusterbikePistonShakeConstants.IDLE_INTENSITY, 1.0F);

        if (state.ignitionSpikeTicks > 0) {
            float spikeRatio = state.ignitionSpikeTicks / (float) DusterbikePistonShakeConstants.IGNITION_SPIKE_TICKS;
            intensity += DusterbikePistonShakeConstants.IGNITION_SPIKE_BOOST * spikeRatio;
        }

        return Mth.clamp(intensity, 0.0F, DusterbikePistonShakeConstants.MAX_INTENSITY);
    }

    public static float getSpeedGaugeArrowDegrees(DusterbikeEntity bike, float partialTick) {
        float speedRatio = Mth.clamp(Math.abs(bike.getDriveForwardSpeed()) / DusterbikeEntity.MAX_SPEED, 0.0F, 1.0F);
        float speedBasedDegrees = Mth.lerp(
                speedRatio,
                DusterbikePistonShakeConstants.GAUGE_FLING_TARGET_DEGREES * -1.0F,
                DusterbikePistonShakeConstants.GAUGE_FLING_TARGET_DEGREES);

        ShakeState state = STATES.computeIfAbsent(bike.getId(), id -> new ShakeState());
        noteEngineTransition(bike, state);

        if (state.gaugeFlingTicks <= 0) {
            return speedBasedDegrees;
        }

        float elapsed = (DusterbikePistonShakeConstants.GAUGE_FLING_TICKS - state.gaugeFlingTicks) + partialTick;
        float progress = Mth.clamp(elapsed / DusterbikePistonShakeConstants.GAUGE_FLING_TICKS, 0.0F, 1.0F);
        float swing = Mth.sin(progress * Mth.PI);
        return Mth.lerp(swing, speedBasedDegrees, DusterbikePistonShakeConstants.GAUGE_FLING_TARGET_DEGREES);
    }

    public static void clear() {
        STATES.clear();
    }

    private static void tickBike(DusterbikeEntity bike) {
        ShakeState state = STATES.computeIfAbsent(bike.getId(), id -> new ShakeState());
        noteEngineTransition(bike, state);
        if (state.ignitionSpikeTicks > 0) {
            state.ignitionSpikeTicks--;
        }
        if (state.gaugeFlingTicks > 0) {
            state.gaugeFlingTicks--;
        }
    }

    private static void noteEngineTransition(DusterbikeEntity bike, ShakeState state) {
        boolean running = bike.isEngineRunning();
        if (running && !state.wasEngineRunning) {
            state.ignitionSpikeTicks = DusterbikePistonShakeConstants.IGNITION_SPIKE_TICKS;
            state.gaugeFlingTicks = DusterbikePistonShakeConstants.GAUGE_FLING_TICKS;
        }
        state.wasEngineRunning = running;
    }

    private static final class ShakeState {
        private boolean wasEngineRunning;
        private int ignitionSpikeTicks;
        private int gaugeFlingTicks;
    }
}
