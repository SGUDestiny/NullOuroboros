package destiny.null_ouroboros.client.render.model;

import destiny.null_ouroboros.NullOuroboros;
import destiny.null_ouroboros.server.entity.DusterbikeEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class DusterbikeGeoModel extends GeoModel<DusterbikeEntity> {
    @Override
    public ResourceLocation getModelResource(DusterbikeEntity entity) {
        return ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, "geo/entity/dusterbike.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(DusterbikeEntity entity) {
        return entity.isEngineRunning() ? ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, "textures/entity/dusterbike.png")
                : ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, "textures/entity/dusterbike_off.png");
    }

    @Override
    public ResourceLocation getAnimationResource(DusterbikeEntity entity) {
        return null;
    }
}