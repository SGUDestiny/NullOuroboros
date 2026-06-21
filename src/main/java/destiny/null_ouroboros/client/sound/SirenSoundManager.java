package destiny.null_ouroboros.client.sound;

import net.minecraft.client.Minecraft;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

import java.util.HashMap;
import java.util.Map;

public class SirenSoundManager {
    private static final Map<BlockPos, SoundPair> ACTIVE_SOUNDS = new HashMap<>();

    public static void play(BlockPos pos, SoundEvent normalEvent, SoundEvent distantEvent, boolean looping) {
        SoundManager sm = Minecraft.getInstance().getSoundManager();
        SoundPair old = ACTIVE_SOUNDS.get(pos);
        if (old != null) {
            sm.stop(old.normal);
            sm.stop(old.distant);
        }
        var normal = new SirenSoundInstance(normalEvent, SoundSource.AMBIENT, pos, looping, 0f, 128f);
        var distant = new SirenSoundInstance(distantEvent, SoundSource.AMBIENT, pos, looping, 64f, 256f);
        sm.play(normal);
        sm.play(distant);
        ACTIVE_SOUNDS.put(pos, new SoundPair(normal, distant));
    }

    public static void stop(BlockPos pos) {
        SoundManager sm = Minecraft.getInstance().getSoundManager();
        SoundPair old = ACTIVE_SOUNDS.remove(pos);
        if (old != null) {
            sm.stop(old.normal);
            sm.stop(old.distant);
        }
    }

    public static void stopAll() {
        SoundManager sm = Minecraft.getInstance().getSoundManager();
        for (BlockPos pos : ACTIVE_SOUNDS.keySet()) {
            stop(pos);
        }
        ACTIVE_SOUNDS.clear();
    }

    private record SoundPair(SirenSoundInstance normal, SirenSoundInstance distant) {}
}