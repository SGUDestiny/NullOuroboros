package destiny.null_ouroboros.client.render.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import destiny.null_ouroboros.NullOuroboros;
import destiny.null_ouroboros.server.block.entity.AbandonedDusterbikeSpawnerBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public class AbandonedDusterbikeSpawnerRenderer implements BlockEntityRenderer<AbandonedDusterbikeSpawnerBlockEntity> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, "textures/item/dusterbike_frame.png");
    private static final float HALF = 0.5F;

    public AbandonedDusterbikeSpawnerRenderer(BlockEntityRendererProvider.Context ctx) {
    }

    @Override
    public void render(AbandonedDusterbikeSpawnerBlockEntity be, float partialTick, PoseStack pose, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        pose.pushPose();
        pose.translate(0.5D, 0.5D, 0.5D);
        pose.mulPose(Axis.YP.rotationDegrees(-be.getYaw() + 90f));

        VertexConsumer consumer = buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        PoseStack.Pose entry = pose.last();
        Matrix4f matrix = entry.pose();
        Matrix3f normal = entry.normal();

        vertex(consumer, matrix, normal, -HALF, -HALF, 0.0F, 0.0F, 1.0F, packedLight);
        vertex(consumer, matrix, normal, -HALF, HALF, 0.0F, 0.0F, 0.0F, packedLight);
        vertex(consumer, matrix, normal, HALF, HALF, 0.0F, 1.0F, 0.0F, packedLight);
        vertex(consumer, matrix, normal, HALF, -HALF, 0.0F, 1.0F, 1.0F, packedLight);

        pose.popPose();
    }

    private static void vertex(VertexConsumer consumer, Matrix4f matrix, Matrix3f normal,
                               float x, float y, float z, float u, float v, int packedLight) {
        consumer.vertex(matrix, x, y, z)
                .color(255, 255, 255, 255)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(packedLight)
                .normal(normal, 0.0F, 0.0F, 1.0F)
                .endVertex();
    }
}
