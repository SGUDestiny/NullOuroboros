package destiny.null_ouroboros.client.render.model;

import destiny.null_ouroboros.NullOuroboros;
import destiny.null_ouroboros.server.block.entity.DeadlockSafeBlockEntity;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class DeadlockSafeGeoModel extends GeoModel<DeadlockSafeBlockEntity> {
    private static final ResourceLocation CLOSED_MODEL =
            ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, "geo/block/deadlock_safe.geo.json");
    private static final ResourceLocation OPEN_MODEL =
            ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, "geo/block/deadlock_safe_open.geo.json");
    private static final ResourceLocation CLOSED_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, "textures/block/deadlock_safe.png");
    private static final ResourceLocation OPEN_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, "textures/block/deadlock_safe_open.png");
    public static final ResourceLocation CLOSED_ON_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, "textures/block/deadlock_safe_on.png");
    public static final ResourceLocation OPEN_ON_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, "textures/block/deadlock_safe_open_on.png");
    private static final ResourceLocation ANIMATION =
            ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, "animations/block/deadlock_safe.animation.json");

    @Override
    public ResourceLocation getModelResource(DeadlockSafeBlockEntity animatable) {
        return animatable.isVisuallyOpen() ? OPEN_MODEL : CLOSED_MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(DeadlockSafeBlockEntity animatable) {
        return animatable.isVisuallyOpen() ? OPEN_TEXTURE : CLOSED_TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(DeadlockSafeBlockEntity animatable) {
        return ANIMATION;
    }

    @Override
    public RenderType getRenderType(DeadlockSafeBlockEntity animatable, ResourceLocation texture) {
        return RenderType.entityCutout(texture);
    }

    public static ResourceLocation onTexture(DeadlockSafeBlockEntity animatable) {
        return animatable.isVisuallyOpen() ? OPEN_ON_TEXTURE : CLOSED_ON_TEXTURE;
    }
}
