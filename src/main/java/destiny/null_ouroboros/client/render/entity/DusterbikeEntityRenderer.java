package destiny.null_ouroboros.client.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import destiny.null_ouroboros.NullOuroboros;
import destiny.null_ouroboros.client.render.DusterbikeRenderTransforms;
import destiny.null_ouroboros.client.render.model.DusterbikeEntityModel;
import destiny.null_ouroboros.server.entity.DusterbikeEntity;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

public class DusterbikeEntityRenderer extends EntityRenderer<DusterbikeEntity> {
    private static final ResourceLocation DUSTERBIKE_TEXTURE = ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, "textures/entity/dusterbike.png");
    private static final ResourceLocation DUSTERBIKE_OFF_TEXTURE = ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, "textures/entity/dusterbike_off.png");
    private static final ResourceLocation DUSTERBIKE_COLORED_TEXTURE = ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, "textures/entity/dusterbike_colored.png");
    private static final ResourceLocation DUSTERBIKE_COLORED_OFF_TEXTURE = ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, "textures/entity/dusterbike_colored_off.png");

    private final DusterbikeEntityModel model;

    public DusterbikeEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new DusterbikeEntityModel(context.bakeLayer(DusterbikeEntityModel.LAYER_LOCATION));
        this.shadowRadius = 0.75F;
    }

    @Override
    public ResourceLocation getTextureLocation(DusterbikeEntity entity) {
        return entity.isEngineRunning() ? DUSTERBIKE_TEXTURE : DUSTERBIKE_OFF_TEXTURE;
    }

    @Override
    public void render(DusterbikeEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();

        DusterbikeRenderTransforms.applyEntityRenderPose(poseStack, entity, partialTicks);

        this.model.setupAnim(entity, 0.0F, 0.0F, entity.tickCount + partialTicks, 0.0F, 0.0F);

        int modelLight = getBikeModelLight(entity, packedLight);
        boolean engineRunning = entity.isEngineRunning();
        ResourceLocation bodyTexture = engineRunning ? DUSTERBIKE_TEXTURE : DUSTERBIKE_OFF_TEXTURE;

        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.entityCutoutNoCull(bodyTexture));
        this.model.renderBody(poseStack, vertexConsumer, modelLight, OverlayTexture.NO_OVERLAY);

        if (engineRunning) {
            VertexConsumer emissiveConsumer = buffer.getBuffer(RenderType.entityTranslucentEmissive(DUSTERBIKE_TEXTURE));
            this.model.renderEmissive(poseStack, emissiveConsumer, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
        } else {
            VertexConsumer offEmissiveConsumer = buffer.getBuffer(RenderType.entityCutoutNoCull(DUSTERBIKE_OFF_TEXTURE));
            this.model.renderEmissive(poseStack, offEmissiveConsumer, modelLight, OverlayTexture.NO_OVERLAY);
        }

        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    private static int getBikeModelLight(DusterbikeEntity entity, int packedLight) {
        BlockPos bodyTop = BlockPos.containing(entity.getX(), entity.getBoundingBox().maxY + 0.25D, entity.getZ());
        BlockPos headRoom = bodyTop.above();
        return Math.max(packedLight, Math.max(
                LevelRenderer.getLightColor(entity.level(), bodyTop),
                LevelRenderer.getLightColor(entity.level(), headRoom)
        ));
    }
}
