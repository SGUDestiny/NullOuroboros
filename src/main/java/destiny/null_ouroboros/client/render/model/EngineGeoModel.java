package destiny.null_ouroboros.client.render.model;

import destiny.null_ouroboros.NullOuroboros;
import destiny.null_ouroboros.server.entity.EngineEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class EngineGeoModel extends GeoModel<EngineEntity> {
    @Override
    public ResourceLocation getModelResource(EngineEntity entity) {
        return ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, "geo/entity/engine.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(EngineEntity entity) {
        return ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, "textures/entity/engine_hoist_off.png");
    }

    @Override
    public ResourceLocation getAnimationResource(EngineEntity entity) {
        return null;
    }
}
