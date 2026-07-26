package destiny.null_ouroboros.client.render.model;

import destiny.null_ouroboros.NullOuroboros;
import destiny.null_ouroboros.server.block.entity.FuseBoxBlockEntity;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class FuseBoxGeoModel extends GeoModel<FuseBoxBlockEntity> {
    private static final ResourceLocation CLOSED_MODEL =
            ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, "geo/block/fuse_box.geo.json");
    private static final ResourceLocation OPEN_MODEL =
            ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, "geo/block/fuse_box_open.geo.json");
    private static final ResourceLocation CLOSED_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, "textures/block/fuse_box.png");
    private static final ResourceLocation OPEN_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, "textures/block/fuse_box_open.png");
    public static final ResourceLocation OPEN_FUSE_OFF_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, "textures/block/fuse_box_open_fuse_off.png");
    private static final ResourceLocation ANIMATION =
            ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, "animations/block/fuse_box.animation.json");

    @Override
    public ResourceLocation getModelResource(FuseBoxBlockEntity animatable) {
        return animatable.isOpen() ? OPEN_MODEL : CLOSED_MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(FuseBoxBlockEntity animatable) {
        return animatable.isOpen() ? OPEN_TEXTURE : CLOSED_TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(FuseBoxBlockEntity animatable) {
        return ANIMATION;
    }

    @Override
    public RenderType getRenderType(FuseBoxBlockEntity animatable, ResourceLocation texture) {
        return RenderType.entityCutout(texture);
    }
}
