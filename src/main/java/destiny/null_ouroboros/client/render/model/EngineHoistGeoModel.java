package destiny.null_ouroboros.client.render.model;

import destiny.null_ouroboros.NullOuroboros;
import destiny.null_ouroboros.server.entity.EngineHoistEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class EngineHoistGeoModel extends GeoModel<EngineHoistEntity> {

    @Override
    public ResourceLocation getModelResource(EngineHoistEntity entity) {
        return ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, "geo/entity/engine_hoist.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(EngineHoistEntity entity) {
        return ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, "textures/entity/engine_hoist.png");
    }

    @Override
    public ResourceLocation getAnimationResource(EngineHoistEntity entity) {
        return ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, "animations/entity/engine_hoist.animation.json");
    }
}