package destiny.null_ouroboros.client.render.model;

import destiny.null_ouroboros.NullOuroboros;
import destiny.null_ouroboros.server.block.entity.GarageDoorBlockEntity;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class GarageDoorGeoModel extends GeoModel<GarageDoorBlockEntity> {
    private static final ResourceLocation MODEL =
            ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, "geo/block/garage_door.geo.json");
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, "textures/block/garage_door.png");
    private static final ResourceLocation ANIMATION =
            ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, "animations/block/garage_door.animation.json");

    @Override
    public ResourceLocation getModelResource(GarageDoorBlockEntity animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(GarageDoorBlockEntity animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(GarageDoorBlockEntity animatable) {
        return ANIMATION;
    }

    @Override
    public RenderType getRenderType(GarageDoorBlockEntity animatable, ResourceLocation texture) {
        return RenderType.entityCutout(texture);
    }
}
