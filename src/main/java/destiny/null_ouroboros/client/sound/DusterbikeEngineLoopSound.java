package destiny.null_ouroboros.client.sound;

import destiny.null_ouroboros.common.DusterbikeEngineSoundConstants;
import destiny.null_ouroboros.server.entity.DusterbikeEntity;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;

import java.util.function.Supplier;

public class DusterbikeEngineLoopSound extends AbstractTickableSoundInstance {
    private final DusterbikeEntity bike;
    private final Supplier<Float> pitchSupplier;
    private float targetVolume = 1.0F;

    public DusterbikeEngineLoopSound(SoundEvent soundEvent, DusterbikeEntity bike, Supplier<Float> pitchSupplier) {
        super(soundEvent, SoundSource.NEUTRAL, SoundInstance.createUnseededRandom());
        this.bike = bike;
        this.pitchSupplier = pitchSupplier;
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
        this.pitch = pitchSupplier.get();

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
        this.targetVolume = Mth.clamp(target, 0.0F, 1.0F);
    }

    public void forceVolume(float vol) {
        this.volume = Mth.clamp(vol, 0.0F, 1.0F);
        this.targetVolume = this.volume;
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
