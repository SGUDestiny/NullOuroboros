package destiny.null_ouroboros.client.sound;

import destiny.null_ouroboros.server.registry.SoundRegistry;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;

public class SteelLeviathanEngineLoopSound extends AbstractTickableSoundInstance {
    private final Entity entity;

    public SteelLeviathanEngineLoopSound(Entity entity) {
        super(SoundRegistry.STEEL_LEVIATHAN_ENGINE_LOOP.get(), SoundSource.HOSTILE, SoundInstance.createUnseededRandom());
        this.entity = entity;
        this.looping = true;
        this.delay = 0;
        this.volume = 1.0F;
        this.pitch = 1.0F;
        this.relative = false;
        this.attenuation = Attenuation.LINEAR;
        updatePosition();
    }

    @Override
    public void tick() {
        if (entity == null || !entity.isAlive() || entity.isRemoved()) {
            stop();
            return;
        }
        updatePosition();
    }

    private void updatePosition() {
        this.x = entity.getX();
        this.y = entity.getY();
        this.z = entity.getZ();
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
