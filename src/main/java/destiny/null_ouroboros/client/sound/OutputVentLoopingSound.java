package destiny.null_ouroboros.client.sound;

import destiny.null_ouroboros.server.block.entity.OutputVentBlockEntity;
import destiny.null_ouroboros.server.registry.SoundRegistry;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

public class OutputVentLoopingSound extends AbstractTickableSoundInstance {
    private final OutputVentBlockEntity blockEntity;

    public OutputVentLoopingSound(SoundEvent soundEvent, OutputVentBlockEntity blockEntity) {
        super(soundEvent, SoundSource.BLOCKS, SoundInstance.createUnseededRandom());
        this.blockEntity = blockEntity;
        BlockPos pos = blockEntity.getBlockPos();
        this.looping = true;
        this.delay = 0;
        this.volume = 0;
        this.pitch = 1;
        this.relative = false;
        this.x = pos.getX() + 0.5;
        this.y = pos.getY() + 0.5;
        this.z = pos.getZ() + 0.5;
        this.attenuation = SoundInstance.Attenuation.LINEAR;
    }

    @Override
    public void tick() {
        if (blockEntity.isRemoved()) {
            stop();
            return;
        }
        float factor = blockEntity.getRunSpeed() / OutputVentBlockEntity.getMaxSpeed();
        this.volume = factor;
        this.pitch = Math.max(0.5F, factor);
        if (factor <= 0) {
            stop();
        }
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }
}
