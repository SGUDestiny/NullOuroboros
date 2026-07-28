package destiny.null_ouroboros.client.sound;

import destiny.null_ouroboros.server.block.entity.IntakeFanBlockEntity;
import destiny.null_ouroboros.server.registry.SoundRegistry;

public final class IntakeFanClientSoundHandler {
    private IntakeFanClientSoundHandler() {
    }

    public static void tick(IntakeFanBlockEntity be) {
        BlockEntityLoopingSoundHandler.tick(
                be,
                b -> b.getRunSpeed() > 0 || b.isOperational(),
                b -> new IntakeFanLoopingSound(SoundRegistry.INTAKE_FAN_LOOP.get(), b)
        );
    }

    public static void stop(IntakeFanBlockEntity be) {
        BlockEntityLoopingSoundHandler.stop(be);
    }
}
