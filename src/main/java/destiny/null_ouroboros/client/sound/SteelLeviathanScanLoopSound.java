package destiny.null_ouroboros.client.sound;

import destiny.null_ouroboros.server.entity.steel_leviathan.SteelLeviathanHeadEntity;
import destiny.null_ouroboros.server.registry.SoundRegistry;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;

public class SteelLeviathanScanLoopSound extends AbstractTickableSoundInstance {
    private static final float FADE_SPEED = 0.12F;

    private SteelLeviathanHeadEntity head;
    private float targetVolume = 0.0F;

    public SteelLeviathanScanLoopSound(SteelLeviathanHeadEntity head) {
        super(SoundRegistry.STEEL_LEVIATHAN_SCAN_LOOP.get(), SoundSource.HOSTILE, SoundInstance.createUnseededRandom());
        this.head = head;
        this.looping = true;
        this.delay = 0;
        this.volume = 0.0F;
        this.pitch = 1.0F;
        this.relative = false;
        this.attenuation = Attenuation.LINEAR;
        updatePosition();
    }

    public void setHead(SteelLeviathanHeadEntity head) {
        this.head = head;
        updatePosition();
    }

    @Override
    public void tick() {
        if (head == null || !head.isAlive() || head.isRemoved()) {
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
        this.x = head.getX();
        this.y = head.getY();
        this.z = head.getZ();
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
