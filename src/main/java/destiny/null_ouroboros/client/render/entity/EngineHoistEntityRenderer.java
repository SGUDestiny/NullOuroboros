package destiny.null_ouroboros.client.render.entity;

import destiny.null_ouroboros.NullOuroboros;
import destiny.null_ouroboros.server.entity.EngineHoistEntity;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public class EngineHoistEntityRenderer extends EntityRenderer<EngineHoistEntity> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            NullOuroboros.MODID, "textures/entity/engine_hoist.png");

    public EngineHoistEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public ResourceLocation getTextureLocation(EngineHoistEntity entity) {
        return TEXTURE;
    }
}
