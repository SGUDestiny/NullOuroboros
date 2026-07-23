package destiny.null_ouroboros.client.item;

import destiny.null_ouroboros.client.render.item.HeavyRevolverGeoRenderer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import java.util.function.Consumer;

public final class HeavyRevolverClientExtensions {
    private HeavyRevolverClientExtensions() {
    }

    public static void register(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private HeavyRevolverGeoRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (this.renderer == null) {
                    this.renderer = new HeavyRevolverGeoRenderer();
                }
                return this.renderer;
            }
        });
    }
}
