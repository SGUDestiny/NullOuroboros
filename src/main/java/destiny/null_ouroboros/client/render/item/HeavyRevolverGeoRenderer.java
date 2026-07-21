package destiny.null_ouroboros.client.render.item;

import destiny.null_ouroboros.NullOuroboros;
import destiny.null_ouroboros.server.item.HeavyRevolverItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedItemGeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class HeavyRevolverGeoRenderer extends GeoItemRenderer<HeavyRevolverItem> {
    public HeavyRevolverGeoRenderer() {
        super(new DefaultedItemGeoModel<>(ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, "heavy_revolver")));
    }
}