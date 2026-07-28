package destiny.null_ouroboros.client.render.model;

import destiny.null_ouroboros.NullOuroboros;
import destiny.null_ouroboros.server.block.entity.BulkheadBlockEntity;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class BulkheadGeoModel extends GeoModel<BulkheadBlockEntity> {
    private static final ResourceLocation MODEL =
            ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, "geo/block/bulkhead.geo.json");
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, "textures/block/bulkhead.png");
    private static final ResourceLocation ANIMATION =
            ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, "animations/block/bulkhead.animation.json");

    @Override
    public ResourceLocation getModelResource(BulkheadBlockEntity animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(BulkheadBlockEntity animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(BulkheadBlockEntity animatable) {
        return ANIMATION;
    }

    @Override
    public RenderType getRenderType(BulkheadBlockEntity animatable, ResourceLocation texture) {
        return RenderType.entityCutout(texture);
    }
}
