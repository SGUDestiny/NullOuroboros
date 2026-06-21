package destiny.null_ouroboros.client.sound;

import destiny.null_ouroboros.server.block.StrobelightBlock;
import destiny.null_ouroboros.server.block.entity.StrobelightBlockEntity;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

public class StrobelightLoopingSound extends AbstractTickableSoundInstance {
    private final BlockPos pos;
    private final StrobelightBlockEntity blockEntity;

    public StrobelightLoopingSound(SoundEvent soundEvent, StrobelightBlockEntity strobelightBlockEntity) {
        super(soundEvent, SoundSource.BLOCKS, SoundInstance.createUnseededRandom());
        this.blockEntity = strobelightBlockEntity;
        this.pos = strobelightBlockEntity.getBlockPos();
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
            this.stop();
            return;
        }

        float speed = blockEntity.getRotationSpeed();
        float maxSpeed = StrobelightBlockEntity.getMaxSpeed();
        float factor = speed / maxSpeed;

        this.volume = 1 * factor;
        this.pitch  = 1 * factor;

        if (factor <= 0 && !blockEntity.getBlockState().getValue(StrobelightBlock.LIT)) {
            this.stop();
        }
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }
}