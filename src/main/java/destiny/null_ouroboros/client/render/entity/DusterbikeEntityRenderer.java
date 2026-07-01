package destiny.null_ouroboros.client.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import destiny.null_ouroboros.NullOuroboros;
import destiny.null_ouroboros.client.render.model.DusterbikeEntityModel;
import destiny.null_ouroboros.common.DusterbikeTransforms;
import destiny.null_ouroboros.server.entity.DusterbikeEntity;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

public class DusterbikeEntityRenderer extends EntityRenderer<DusterbikeEntity> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, "textures/entity/dusterbike.png");

    private final DusterbikeEntityModel model;

    public DusterbikeEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new DusterbikeEntityModel(context.bakeLayer(DusterbikeEntityModel.LAYER_LOCATION));
        this.shadowRadius = 0.75F;
    }

    @Override
    public ResourceLocation getTextureLocation(DusterbikeEntity entity) {
        return TEXTURE;
    }

    @Override
    public void render(DusterbikeEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();

        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - entity.getRenderYaw(partialTicks)));

        float roll = entity.getRenderRoll(partialTicks);
        Vec3 pivot = DusterbikeTransforms.PITCH_PIVOT_LOCAL;
        poseStack.translate(pivot.x, pivot.y, pivot.z);
        poseStack.mulPose(Axis.ZP.rotationDegrees(roll));
        poseStack.translate(-pivot.x, -pivot.y, -pivot.z);

        float pitch = entity.getRenderPitch(partialTicks);
        poseStack.translate(pivot.x, pivot.y, pivot.z);
        poseStack.mulPose(Axis.XP.rotationDegrees(-pitch));
        poseStack.translate(-pivot.x, -pivot.y, -pivot.z);

        poseStack.scale(-1.0F, -1.0F, 1.0F);
        poseStack.translate(DusterbikeTransforms.MODEL_X_OFFSET, DusterbikeTransforms.MODEL_Y_OFFSET, 0.0F);

        this.model.setupAnim(entity, 0.0F, 0.0F, entity.tickCount + partialTicks, 0.0F, 0.0F);

        int modelLight = getBikeModelLight(entity, packedLight);
        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        this.model.renderBody(poseStack, vertexConsumer, modelLight, OverlayTexture.NO_OVERLAY);

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
