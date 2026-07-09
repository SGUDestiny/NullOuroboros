package destiny.null_ouroboros.client.sound;

import destiny.null_ouroboros.common.dimension.VergeOfRealityDimension;
import destiny.null_ouroboros.server.capability.ClientManifoldingHolder;
import destiny.null_ouroboros.server.capability.ManifoldingPhase;
import destiny.null_ouroboros.server.registry.SoundRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.event.TickEvent;

import java.util.function.Supplier;

public class ManifoldingSoundManager {
    private static final int START_DELAY = (int) (20 * 8.5);
    private static final int END_BUFFER = (int) (20 * 2);

    private static final float START_NORMAL_VOL = 0.6f;
    private static final float START_MUFFLED_VOL = 0.2f;
    private static final float LOOP_NORMAL_VOL = 0.6f;
    private static final float LOOP_MUFFLED_VOL = 0.2f;
    private static final float END_NORMAL_VOL = 0.6f;
    private static final float END_MUFFLED_VOL = 0.2f;

    private static final SoundPair startPair = new SoundPair(SoundRegistry.MANIFOLDING_START, SoundRegistry.MANIFOLDING_START_MUFFLED,
            START_NORMAL_VOL, START_MUFFLED_VOL, false);
    private static final SoundPair loopPair = new SoundPair(SoundRegistry.MANIFOLDING_LOOP, SoundRegistry.MANIFOLDING_LOOP_MUFFLED,
            LOOP_NORMAL_VOL, LOOP_MUFFLED_VOL, true);
    private static final SoundPair endPair = new SoundPair(SoundRegistry.MANIFOLDING_END, SoundRegistry.MANIFOLDING_END_MUFFLED,
            END_NORMAL_VOL, END_MUFFLED_VOL, false);

    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || mc.player == null) return;

        if (!VergeOfRealityDimension.isVergeOfReality(level)) return;

        ClientManifoldingHolder.updateExposure();
        float exposure = ClientManifoldingHolder.getExposureLevel();

        ManifoldingPhase phase = ClientManifoldingHolder.getPhase();
        long now = level.getGameTime();
        long elapsed = now - ClientManifoldingHolder.getPhaseStartTime();
        int preDur = ClientManifoldingHolder.getPreDuration();
        int activeDur = ClientManifoldingHolder.getActiveDuration();

        boolean playStart = false, playLoop = false, playEnd = false;

        switch (phase) {
            case PRE_EVENT:
                if (preDur > 0 && (preDur - elapsed) <= START_DELAY && (preDur - elapsed) > 0)
                    playStart = true;
                break;
            case ACTIVE:
                playLoop = true;
                if (activeDur > 0 && (activeDur - elapsed) <= END_BUFFER && (activeDur - elapsed) > 0)
                    playEnd = true;
                break;
        }

        startPair.update(mc, playStart, exposure);
        loopPair.update(mc, playLoop, exposure);
        endPair.update(mc, playEnd, exposure);

        if (playLoop && (loopPair.normal == null || !mc.getSoundManager().isActive(loopPair.normal))) {
            loopPair.forceStart(mc, exposure);
        }
    }

    private static class SoundPair {
        private ManifoldingSoundInstance normal, muffled;
        private final Supplier<SoundEvent> normalSupplier, muffledSupplier;
        private final float normalVol, muffledVol;
        private final boolean looping;

        SoundPair(Supplier<SoundEvent> normalSupplier, Supplier<SoundEvent> muffledSupplier, float nVol, float mVol, boolean looping) {
            this.normalSupplier = normalSupplier;
            this.muffledSupplier = muffledSupplier;
            this.normalVol = nVol;
            this.muffledVol = mVol;
            this.looping = looping;
        }

        void update(Minecraft mc, boolean active, float exposure) {
            if (active) {
                if (normal == null) {
                    startNormalAndMuffled(mc, exposure);
                } else {
                    normal.setTargetVolume(exposure * normalVol);
                    muffled.setTargetVolume((1f - exposure) * muffledVol);
                }
            } else if (normal != null) {
                normal.setTargetVolume(0f);
                muffled.setTargetVolume(0f);
                if (normal.isStopped() && muffled.isStopped()) {
                    normal = null;
                    muffled = null;
                }
            }
        }

        void forceStart(Minecraft mc, float exposure) {
            if (normal != null) {
                mc.getSoundManager().stop(normal);
                mc.getSoundManager().stop(muffled);
            }
            startNormalAndMuffled(mc, exposure);
        }

        private void startNormalAndMuffled(Minecraft mc, float exposure) {
            normal = new ManifoldingSoundInstance(normalSupplier.get(), SoundSource.AMBIENT, looping);
            muffled = new ManifoldingSoundInstance(muffledSupplier.get(), SoundSource.AMBIENT, looping);
            normal.forceVolume(exposure * normalVol);
            muffled.forceVolume((1f - exposure) * muffledVol);
            mc.getSoundManager().play(normal);
            mc.getSoundManager().play(muffled);
        }
    }
}