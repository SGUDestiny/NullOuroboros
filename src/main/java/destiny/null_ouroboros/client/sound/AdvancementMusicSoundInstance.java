package destiny.null_ouroboros.client.sound;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;

public class AdvancementMusicSoundInstance extends AbstractTickableSoundInstance {
    private float targetVolume = 0f;
    private final float fadeSpeed;

    public AdvancementMusicSoundInstance(SoundEvent soundEvent, float fadeSpeed) {
        super(soundEvent, SoundSource.MUSIC, SoundInstance.createUnseededRandom());
        this.looping = false;
        this.volume = 0f;
        this.relative = true;
        this.attenuation = SoundInstance.Attenuation.NONE;
        this.fadeSpeed = fadeSpeed;
    }

    @Override
    public void tick() {
        if (volume < targetVolume) {
            volume = Math.min(volume + fadeSpeed, targetVolume);
        } else if (volume > targetVolume) {
            volume = Math.max(volume - fadeSpeed, targetVolume);
        }

        if (volume <= 0f && targetVolume <= 0f && !isStopped()) {
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
