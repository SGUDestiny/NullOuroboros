package destiny.null_ouroboros.client.sound;

import destiny.null_ouroboros.server.block.entity.OutputVentBlockEntity;
import destiny.null_ouroboros.server.registry.SoundRegistry;

public final class OutputVentClientSoundHandler {
    private OutputVentClientSoundHandler() {
    }

    public static void tick(OutputVentBlockEntity be) {
        BlockEntityLoopingSoundHandler.tick(
                be,
                b -> b.getRunSpeed() > 0 || b.isVisuallyActive(),
                b -> new OutputVentLoopingSound(SoundRegistry.OUTPUT_VENT_LOOP.get(), b)
        );
    }

    public static void stop(OutputVentBlockEntity be) {
        BlockEntityLoopingSoundHandler.stop(be);
    }
}
