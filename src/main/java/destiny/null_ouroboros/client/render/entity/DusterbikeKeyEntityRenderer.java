package destiny.null_ouroboros.client.render.entity;

import destiny.null_ouroboros.NullOuroboros;
import destiny.null_ouroboros.server.entity.DusterbikeKeyEntity;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public class DusterbikeKeyEntityRenderer extends EntityRenderer<DusterbikeKeyEntity> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, "textures/entity/dusterbike.png");

    public DusterbikeKeyEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public ResourceLocation getTextureLocation(DusterbikeKeyEntity entity) {
        return TEXTURE;
    }
}
