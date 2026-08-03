package destiny.null_ouroboros.client.render.model;

import destiny.null_ouroboros.NullOuroboros;
import destiny.null_ouroboros.server.block.entity.CodelockSafeBlockEntity;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class CodelockSafeGeoModel extends GeoModel<CodelockSafeBlockEntity> {
    private static final ResourceLocation CLOSED_MODEL =
            ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, "geo/block/codelock_safe.geo.json");
    private static final ResourceLocation OPEN_MODEL =
            ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, "geo/block/codelock_safe_open.geo.json");
    private static final ResourceLocation CLOSED_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, "textures/block/codelock_safe.png");
    private static final ResourceLocation OPEN_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, "textures/block/codelock_safe_open.png");
    public static final ResourceLocation CLOSED_ON_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, "textures/block/codelock_safe_on.png");
    public static final ResourceLocation OPEN_ON_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, "textures/block/codelock_safe_open_on.png");
    private static final ResourceLocation ANIMATION =
            ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, "animations/block/codelock_safe.animation.json");

    @Override
    public ResourceLocation getModelResource(CodelockSafeBlockEntity animatable) {
        return animatable.isVisuallyOpen() ? OPEN_MODEL : CLOSED_MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(CodelockSafeBlockEntity animatable) {
        return animatable.isVisuallyOpen() ? OPEN_TEXTURE : CLOSED_TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(CodelockSafeBlockEntity animatable) {
        return ANIMATION;
    }

    @Override
    public RenderType getRenderType(CodelockSafeBlockEntity animatable, ResourceLocation texture) {
        return RenderType.entityCutout(texture);
    }

    public static ResourceLocation onTexture(CodelockSafeBlockEntity animatable) {
        return animatable.isVisuallyOpen() ? OPEN_ON_TEXTURE : CLOSED_ON_TEXTURE;
    }
}
