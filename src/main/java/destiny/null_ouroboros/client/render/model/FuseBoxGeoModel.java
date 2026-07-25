package destiny.null_ouroboros.client.render.model;

import destiny.null_ouroboros.NullOuroboros;
import destiny.null_ouroboros.server.block.entity.FuseBoxBlockEntity;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedBlockGeoModel;

public class FuseBoxGeoModel extends DefaultedBlockGeoModel<FuseBoxBlockEntity> {
    public FuseBoxGeoModel() {
        super(ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, "fuse_box"));
    }

    @Override
    public RenderType getRenderType(FuseBoxBlockEntity animatable, ResourceLocation texture) {
        return RenderType.entityCutout(texture);
    }
}
