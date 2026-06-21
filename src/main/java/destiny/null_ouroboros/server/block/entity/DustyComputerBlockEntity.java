package destiny.null_ouroboros.server.block.entity;

import destiny.null_ouroboros.client.sound.DustyComputerLoopingSound;
import destiny.null_ouroboros.server.block.DustyComputerBlock;
import destiny.null_ouroboros.server.registry.BlockEntityRegistry;
import destiny.null_ouroboros.server.registry.SoundRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class DustyComputerBlockEntity extends BlockEntity {

    private DustyComputerLoopingSound loopingSound;

    public DustyComputerBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.DUSTY_COMPUTER_BLOCK_ENTITY.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, DustyComputerBlockEntity blockEntity) {
        if (level.isClientSide) {
            blockEntity.clientTickSounds();
        }
    }

    private void clientTickSounds() {
        boolean powered = getBlockState().getValue(DustyComputerBlock.POWERED);

        if (powered) {
            if (loopingSound == null || loopingSound.isStopped()) {
                loopingSound = new DustyComputerLoopingSound(SoundRegistry.DUSTY_COMPUTER_LOOP.get(), this);
                Minecraft.getInstance().getSoundManager().play(loopingSound);
            }
        } else {
            if (loopingSound != null) {
                loopingSound.stopSound();
                loopingSound = null;
            }
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level != null && level.isClientSide && loopingSound != null) {
            loopingSound.stopSound();
            loopingSound = null;
        }
    }
}