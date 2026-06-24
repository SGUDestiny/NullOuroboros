package destiny.null_ouroboros.client.sound;

import destiny.null_ouroboros.server.block.entity.DustyComputerBlockEntity;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

public class DustyComputerLoopingSound extends AbstractTickableSoundInstance {
    private static final int FADE_IN_TICKS = 9 * 20;

    private final DustyComputerBlockEntity blockEntity;
    private int ticksElapsed;

    public DustyComputerLoopingSound(SoundEvent soundEvent, DustyComputerBlockEntity blockEntity) {
        super(soundEvent, SoundSource.BLOCKS, SoundInstance.createUnseededRandom());
        this.blockEntity = blockEntity;
        this.looping = true;
        this.delay = 0;
        this.volume = 0f;
        this.pitch = 1f;
        this.relative = false;

        BlockPos pos = blockEntity.getBlockPos();
        this.x = pos.getX() + 0.5;
        this.y = pos.getY() + 0.5;
        this.z = pos.getZ() + 0.5;
        this.attenuation = SoundInstance.Attenuation.LINEAR;
    }

    @Override
    public void tick() {
        if (blockEntity.isRemoved()) {
            this.stop();
        }

        if (ticksElapsed < FADE_IN_TICKS) {
            ticksElapsed++;
            this.volume = Math.min((float) ticksElapsed / FADE_IN_TICKS, 0.3f);
        } else {
            this.volume = 0.3f;
        }
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }

    public void stopSound() {
        this.stop();
    }
}