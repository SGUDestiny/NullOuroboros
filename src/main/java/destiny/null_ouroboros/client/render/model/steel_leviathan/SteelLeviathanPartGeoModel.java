package destiny.null_ouroboros.client.render.model.steel_leviathan;

import destiny.null_ouroboros.NullOuroboros;
import destiny.null_ouroboros.common.steel_leviathan.SteelLeviathanConstants;
import destiny.null_ouroboros.server.entity.steel_leviathan.SteelLeviathanPartEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class SteelLeviathanPartGeoModel extends GeoModel<SteelLeviathanPartEntity> {
    private final String armoredGeo;
    private final String vulnerableGeo;

    public SteelLeviathanPartGeoModel(String partName) {
        this.armoredGeo = "geo/entity/steel_leviathan_" + partName + ".geo.json";
        this.vulnerableGeo = "geo/entity/steel_leviathan_" + partName + "_vulnerable.geo.json";
    }

    @Override
    public ResourceLocation getModelResource(SteelLeviathanPartEntity entity) {
        String path = entity.isVulnerable() ? vulnerableGeo : armoredGeo;
        return ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, path);
    }

    @Override
    public ResourceLocation getTextureResource(SteelLeviathanPartEntity entity) {
        return entity.isVulnerable() ? SteelLeviathanConstants.TEXTURE_VULNERABLE : SteelLeviathanConstants.TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(SteelLeviathanPartEntity entity) {
        return null;
    }
}

