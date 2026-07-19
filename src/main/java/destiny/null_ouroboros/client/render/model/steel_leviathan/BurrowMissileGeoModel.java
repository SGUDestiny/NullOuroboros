package destiny.null_ouroboros.client.render.model.steel_leviathan;

import destiny.null_ouroboros.NullOuroboros;
import destiny.null_ouroboros.common.steel_leviathan.SteelLeviathanConstants;
import destiny.null_ouroboros.server.entity.steel_leviathan.BurrowMissileEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class BurrowMissileGeoModel extends GeoModel<BurrowMissileEntity> {
    @Override
    public ResourceLocation getModelResource(BurrowMissileEntity entity) {
        return ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, "geo/entity/steel_leviathan_burrow_missile.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(BurrowMissileEntity entity) {
        return SteelLeviathanConstants.engineTexture(entity.tickCount / SteelLeviathanConstants.ENGINE_FRAME_TICKS);
    }

    @Override
    public ResourceLocation getAnimationResource(BurrowMissileEntity entity) {
        return null;
    }
}

