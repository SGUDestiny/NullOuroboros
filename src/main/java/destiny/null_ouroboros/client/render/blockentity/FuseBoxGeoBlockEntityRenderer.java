package destiny.null_ouroboros.client.render.blockentity;

import destiny.null_ouroboros.client.render.model.FuseBoxGeoModel;
import destiny.null_ouroboros.server.block.entity.FuseBoxBlockEntity;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class FuseBoxGeoBlockEntityRenderer extends GeoBlockRenderer<FuseBoxBlockEntity> {
    public FuseBoxGeoBlockEntityRenderer() {
        super(new FuseBoxGeoModel());
    }
}
