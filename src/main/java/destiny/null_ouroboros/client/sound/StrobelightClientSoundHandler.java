package destiny.null_ouroboros.client.sound;

import destiny.null_ouroboros.server.block.StrobelightBlock;
import destiny.null_ouroboros.server.block.entity.StrobelightBlockEntity;
import destiny.null_ouroboros.server.registry.SoundRegistry;

public final class StrobelightClientSoundHandler {
    private StrobelightClientSoundHandler() {}

    public static void tick(StrobelightBlockEntity be) {
        BlockEntityLoopingSoundHandler.tick(
                be,
                b -> b.getBlockState().getValue(StrobelightBlock.LIT) || b.getRotationSpeed() > 0,
                b -> new StrobelightLoopingSound(SoundRegistry.STROBELIGHT_ALARM.get(), b)
        );
    }

    public static void stop(StrobelightBlockEntity be) {
        BlockEntityLoopingSoundHandler.stop(be);
    }
}