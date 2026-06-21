package destiny.null_ouroboros.client.sound;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;

public class ManifoldingSoundInstance extends AbstractTickableSoundInstance {
    private float targetVolume = 0f;
    private static final float FADE_SPEED = 0.15f;

    public ManifoldingSoundInstance(SoundEvent soundEvent, SoundSource source, boolean looping) {
        super(soundEvent, source, SoundInstance.createUnseededRandom());
        this.looping = looping;
        this.volume = 0f;
        this.relative = true;
        this.attenuation = SoundInstance.Attenuation.NONE;
    }

    @Override
    public void tick() {
        if (volume < targetVolume) {
            volume = Math.min(volume + FADE_SPEED, targetVolume);
        } else if (volume > targetVolume) {
            volume = Math.max(volume - FADE_SPEED, targetVolume);
        }

        if (!looping && volume <= 0f && targetVolume <= 0f && !isStopped()) {
            stop();
        }
    }

    public void setTargetVolume(float target) {
        this.targetVolume = Mth.clamp(target, 0f, 1f);
    }

    public void forceVolume(float vol) {
        this.volume = Mth.clamp(vol, 0f, 1f);
        this.targetVolume = this.volume;
    }

    @Override
    public float getVolume() {
        return this.volume;
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }
}