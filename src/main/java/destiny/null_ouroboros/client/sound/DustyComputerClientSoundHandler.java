package destiny.null_ouroboros.client.sound;

import destiny.null_ouroboros.server.block.DustyComputerBlock;
import destiny.null_ouroboros.server.block.entity.DustyComputerBlockEntity;
import destiny.null_ouroboros.server.registry.SoundRegistry;

public final class DustyComputerClientSoundHandler {
    private DustyComputerClientSoundHandler() {}

    public static void tick(DustyComputerBlockEntity be) {
        BlockEntityLoopingSoundHandler.tick(
                be,
                b -> b.getBlockState().getValue(DustyComputerBlock.POWERED),
                b -> new DustyComputerLoopingSound(SoundRegistry.DUSTY_COMPUTER_LOOP.get(), b)
        );
    }

    public static void stop(DustyComputerBlockEntity be) {
        BlockEntityLoopingSoundHandler.stop(be);
    }
}