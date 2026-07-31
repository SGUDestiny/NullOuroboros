package destiny.null_ouroboros.client.sound;

import destiny.null_ouroboros.common.dusterbike.DusterbikeEngineSoundConstants;
import destiny.null_ouroboros.server.entity.DusterbikeEntity;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;

public class DusterbikeWheelRimLoopSound extends AbstractTickableSoundInstance {
    private final DusterbikeEntity bike;
    private float targetVolume = 0.5F;

    public DusterbikeWheelRimLoopSound(SoundEvent soundEvent, DusterbikeEntity bike) {
        super(soundEvent, SoundSource.NEUTRAL, SoundInstance.createUnseededRandom());
        this.bike = bike;
        this.looping = true;
        this.delay = 0;
        this.volume = 0.0F;
        this.pitch = 1.0F;
        this.relative = false;
        this.attenuation = Attenuation.LINEAR;
        updatePosition();
    }

    @Override
    public void tick() {
        if (!bike.isAlive()) {
            stop();
            return;
        }

        updatePosition();

        if (volume < targetVolume) {
            volume = Math.min(volume + DusterbikeEngineSoundConstants.CROSSFADE_SPEED, targetVolume);
        } else if (volume > targetVolume) {
            volume = Math.max(volume - DusterbikeEngineSoundConstants.CROSSFADE_SPEED, targetVolume);
        }

        if (volume <= 0.0F && targetVolume <= 0.0F && !isStopped()) {
            stop();
        }
    }

    public void setTargetVolume(float target) {
        this.targetVolume = Mth.clamp(target, 0.0F, 0.5F);
    }

    private void updatePosition() {
        this.x = bike.getX();
        this.y = bike.getY();
        this.z = bike.getZ();
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
