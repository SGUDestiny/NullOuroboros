package destiny.null_ouroboros.server.camouflage;

import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public interface Camouflageable {
    @Nullable
    BlockState getCamouflage();

    void setCamouflage(@Nullable BlockState camouflage);

    default void clearCamouflage() {
        setCamouflage(null);
    }

    default boolean hasCamouflage() {
        return getCamouflage() != null;
    }
}
