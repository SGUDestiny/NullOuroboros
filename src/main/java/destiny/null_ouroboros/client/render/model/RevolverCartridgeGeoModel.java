package destiny.null_ouroboros.client.render.model;

import destiny.null_ouroboros.NullOuroboros;
import destiny.null_ouroboros.server.entity.CartridgeEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class RevolverCartridgeGeoModel extends GeoModel<CartridgeEntity> {
    @Override
    public ResourceLocation getModelResource(CartridgeEntity entity) {
        return ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, "geo/entity/revolver_cartridge.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(CartridgeEntity entity) {
        return ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, "textures/item/heavy_revolver.png");
    }

    @Override
    public ResourceLocation getAnimationResource(CartridgeEntity entity) {
        return null;
    }
}
