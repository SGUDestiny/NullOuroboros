package destiny.null_ouroboros.client.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import destiny.null_ouroboros.NullOuroboros;
import destiny.null_ouroboros.server.entity.DusterbikeWheelEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public class DusterbikeWheelEntityRenderer extends EntityRenderer<DusterbikeWheelEntity> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, "textures/entity/dusterbike.png");

    public DusterbikeWheelEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public ResourceLocation getTextureLocation(DusterbikeWheelEntity entity) {
        return TEXTURE;
    }

    @Override
    public void render(DusterbikeWheelEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }
}
