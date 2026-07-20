package destiny.null_ouroboros.client.sound;

import destiny.null_ouroboros.server.entity.steel_leviathan.SteelLeviathanPartEntity;
import destiny.null_ouroboros.server.registry.SoundRegistry;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;

public class SteelLeviathanBreachingLoopSound extends AbstractTickableSoundInstance {
    private static final float FADE_SPEED = 0.08F;

    private SteelLeviathanPartEntity part;
    private float targetVolume = 0.0F;

    public SteelLeviathanBreachingLoopSound(SteelLeviathanPartEntity part) {
        super(SoundRegistry.STEEL_LEVIATHAN_BREACHING_GROUND_LOOP.get(), SoundSource.HOSTILE, SoundInstance.createUnseededRandom());
        this.part = part;
        this.looping = true;
        this.delay = 0;
        this.volume = 0.0F;
        this.pitch = 1.0F;
        this.relative = false;
        this.attenuation = Attenuation.LINEAR;
        updatePosition();
    }

    public void setPart(SteelLeviathanPartEntity part) {
        this.part = part;
        updatePosition();
    }

    @Override
    public void tick() {
        if (part == null || !part.isAlive() || part.isRemoved()) {
            stop();
            return;
        }

        updatePosition();

        if (volume < targetVolume) {
            volume = Math.min(volume + FADE_SPEED, targetVolume);
        } else if (volume > targetVolume) {
            volume = Math.max(volume - FADE_SPEED, targetVolume);
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
        this.x = part.getX();
        this.y = part.getY();
        this.z = part.getZ();
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

