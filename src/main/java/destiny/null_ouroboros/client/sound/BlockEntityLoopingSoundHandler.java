package destiny.null_ouroboros.client.sound;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

public final class BlockEntityLoopingSoundHandler {
    private static final Map<BlockEntity, AbstractTickableSoundInstance> SOUNDS = new IdentityHashMap<>();

    private BlockEntityLoopingSoundHandler() {}

    public static <T extends BlockEntity> void tick(T be, Predicate<T> isActive, Function<T, AbstractTickableSoundInstance> soundFactory) {
        AbstractTickableSoundInstance sound = SOUNDS.get(be);
        if (isActive.test(be)) {
            if (sound == null || sound.isStopped()) {
                sound = soundFactory.apply(be);
                Minecraft.getInstance().getSoundManager().play(sound);
                SOUNDS.put(be, sound);
            }
        } else {
            stop(be);
        }
    }

    public static void stop(BlockEntity be) {
        AbstractTickableSoundInstance sound = SOUNDS.remove(be);
        if (sound != null) {
            Minecraft.getInstance().getSoundManager().stop(sound);
        }
    }
}