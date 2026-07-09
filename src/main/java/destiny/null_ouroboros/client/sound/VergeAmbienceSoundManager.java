package destiny.null_ouroboros.client.sound;

import destiny.null_ouroboros.common.dimension.VergeOfRealityDimension;
import destiny.null_ouroboros.server.capability.ClientManifoldingHolder;
import destiny.null_ouroboros.server.capability.ManifoldingPhase;
import destiny.null_ouroboros.server.registry.SoundRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.event.TickEvent;

public class VergeAmbienceSoundManager {
    private static final float MAX_VOLUME = 0.1f;
    private static final float FADE_SPEED = MAX_VOLUME / (5 * 20);

    private static ManifoldingSoundInstance instance;

    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || mc.player == null) return;

        if (!VergeOfRealityDimension.isVergeOfReality(level)) {
            stopInstance(mc);
            return;
        }

        boolean shouldPlay = ClientManifoldingHolder.getPhase() == ManifoldingPhase.CLEAR
                && level.canSeeSky(mc.player.blockPosition());
        float target = shouldPlay ? MAX_VOLUME : 0f;

        if (instance == null && target > 0f) {
            instance = new ManifoldingSoundInstance(
                    SoundRegistry.VERGE_AMBIENCE.get(), SoundSource.AMBIENT, true, FADE_SPEED);
            mc.getSoundManager().play(instance);
        }
        if (instance != null) {
            instance.setTargetVolume(target);
            if (instance.isStopped()) {
                instance = null;
            }
        }
    }

    public static void stopInstance(Minecraft mc) {
        if (instance != null) {
            mc.getSoundManager().stop(instance);
            instance = null;
        }
    }
}
