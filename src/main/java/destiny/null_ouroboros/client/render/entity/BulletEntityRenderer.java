package destiny.null_ouroboros.client.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import destiny.null_ouroboros.NullOuroboros;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import destiny.null_ouroboros.server.entity.BulletEntity;

public class BulletEntityRenderer extends EntityRenderer<BulletEntity> {
    private static final ResourceLocation BULLET_TEXTURE = ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, "textures/particle/blood.png");

    public BulletEntityRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public void render(BulletEntity bullet, float entityYaw, float partialTicks, PoseStack pose, MultiBufferSource buffer, int packedLight) {
        Vec3 cam = this.entityRenderDispatcher.camera.getPosition();

        pose.pushPose();
        pose.translate(-cam.x, -cam.y, -cam.z);
        pose.translate(bullet.getX(), bullet.getY(), bullet.getZ());
        pose.mulPose(this.entityRenderDispatcher.cameraOrientation());
        pose.mulPose(Axis.YP.rotationDegrees(180.0F));

        float size = 0.15F;
        VertexConsumer cutoutBuilder = buffer.getBuffer(RenderType.entityTranslucentEmissive(BULLET_TEXTURE));
        PoseStack.Pose entry = pose.last();
        Matrix4f matrix = entry.pose();
        Matrix3f normal = entry.normal();

        cutoutBuilder.vertex(matrix, -size, -size, 0.0F).color(255, 255, 255, 255)
                .uv(0, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight)
                .normal(normal, 0, 1, 0).endVertex();
        cutoutBuilder.vertex(matrix, -size, size, 0.0F).color(255, 255, 255, 255)
                .uv(0, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight)
                .normal(normal, 0, 1, 0).endVertex();
        cutoutBuilder.vertex(matrix, size, size, 0.0F).color(255, 255, 255, 255)
                .uv(1, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight)
                .normal(normal, 0, 1, 0).endVertex();
        cutoutBuilder.vertex(matrix, size, -size, 0.0F).color(255, 255, 255, 255)
                .uv(1, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight)
                .normal(normal, 0, 1, 0).endVertex();

        pose.popPose();

        if (bullet.tickCount < 2) return;

        Vec3[] positions = bullet.getTrailPositions();
        if (positions.length > 0) {
            VertexConsumer translucentBuilder = buffer.getBuffer(RenderType.entityTranslucentEmissive(BULLET_TEXTURE));
            pose.pushPose();
            pose.translate(-cam.x, -cam.y, -cam.z);

            for (int i = 0; i < positions.length; i++) {
                Vec3 pos = positions[i];
                float alpha = 1.0F - (float) i / positions.length;
                if (alpha <= 0.02F) continue;

                pose.pushPose();
                pose.translate(pos.x, pos.y, pos.z);
                pose.mulPose(this.entityRenderDispatcher.cameraOrientation());
                pose.mulPose(Axis.YP.rotationDegrees(180.0F));

                float trailSize = size * 0.8F * (1.0F - (float) i / positions.length);

                translucentBuilder.vertex(matrix, -trailSize, -trailSize, 0.0F).color(1f, 1f, 1f, alpha)
                        .uv(0, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight)
                        .normal(normal, 0, 1, 0).endVertex();
                translucentBuilder.vertex(matrix, -trailSize, trailSize, 0.0F).color(1f, 1f, 1f, alpha)
                        .uv(0, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight)
                        .normal(normal, 0, 1, 0).endVertex();
                translucentBuilder.vertex(matrix, trailSize, trailSize, 0.0F).color(1f, 1f, 1f, alpha)
                        .uv(1, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight)
                        .normal(normal, 0, 1, 0).endVertex();
                translucentBuilder.vertex(matrix, trailSize, -trailSize, 0.0F).color(1f, 1f, 1f, alpha)
                        .uv(1, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight)
                        .normal(normal, 0, 1, 0).endVertex();

                pose.popPose();
            }
        }

        pose.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(BulletEntity entity) {
        return BULLET_TEXTURE;
    }

    @Override
    public boolean shouldRender(BulletEntity p_114491_, Frustum p_114492_, double p_114493_, double p_114494_, double p_114495_) {
        return true;
    }
}