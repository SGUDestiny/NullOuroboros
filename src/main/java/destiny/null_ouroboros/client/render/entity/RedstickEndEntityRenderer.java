package destiny.null_ouroboros.client.render.entity;

import destiny.null_ouroboros.NullOuroboros;
import destiny.null_ouroboros.server.entity.RedstickEndEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import com.mojang.blaze3d.vertex.PoseStack;

public class RedstickEndEntityRenderer extends EntityRenderer<RedstickEndEntity> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, "textures/entity/redstick.png");

    public RedstickEndEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public ResourceLocation getTextureLocation(RedstickEndEntity entity) {
        return TEXTURE;
    }

    @Override
    public void render(RedstickEndEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }
}
